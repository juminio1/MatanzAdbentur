package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.tallerwebi.dominio.catalogo.CatalogoTablero.POSICIONES_PROPIEDAD;
import static com.tallerwebi.dominio.catalogo.CatalogoTablero.POSICIONES_PARADA;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ControladorOfertaPropiedad {
    private final ServicioParticipacion participacion;

    private static final String CLAVE_ERROR = "error";
    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private static final String POSICION = "posicion";
    private static final String ID_JUGADOR = "idJugador";
    private static final String DINERO_JUGADOR = "dineroJugador";

    private final ServicioPartida servicioPartida;
    private final ServicioCasillero servicioCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final Map<String, OfertaVenta> ofertas = new ConcurrentHashMap<>();

    public ControladorOfertaPropiedad(ServicioPartida servicioPartida, ServicioCasillero servicioCasillero,
                                      ServicioPropiedad servicioPropiedad,
                                      SimpMessagingTemplate notificadorWebSocket, ServicioParticipacion participacion) {
        this.participacion = participacion;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.notificadorWebSocket = notificadorWebSocket;
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearOferta(
            @PathVariable("codigoPartida") String codigoPartida,
            @PathVariable(POSICION) Integer posicion,
            @RequestBody Map<String, Object> datos,
            HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || !POSICIONES_PROPIEDAD.contains(posicion)) return ResponseEntity.notFound().build();
        Long idVendedor = ResolutorJugadorSesion.resolver(partida, request);
        Long idComprador = numeroLong(datos.get("idJugadorComprador"));
        Integer precio = numeroInteger(datos.get("precio"));
        if (!participantesValidos(partida, idVendedor, idComprador) || precio == null) {
            return error(HttpStatus.BAD_REQUEST, "La oferta no es válida.");
        }
        CasilleroPropiedad casillero = buscarCasillero(partida, posicion);
        if (!perteneceAlVendedor(casillero, idVendedor)) {
            return error(HttpStatus.CONFLICT, "Solo podés ofrecer una propiedad que te pertenece.");
        }
        String idOferta = UUID.randomUUID().toString();
        ofertas.put(idOferta, new OfertaVenta(idOferta, codigoPartida, posicion, idVendedor, idComprador,
                precio, Instant.now().plusSeconds(300)));
        return ResponseEntity.ok(Map.of("idOferta", idOferta, "estado", "pendiente"));
    }

    @GetMapping("/partida/{codigoPartida}/ofertas-pendientes")
    @ResponseBody
    public ResponseEntity<?> ofertasPendientes(@PathVariable("codigoPartida") String codigoPartida,
                                                HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!participacion.pertenece(partida, idJugador)) return error(HttpStatus.FORBIDDEN, "No pertenecés a esta partida.");
        Instant ahora = Instant.now();
        List<Map<String, Object>> resultado = ofertas.values().stream()
                .filter(oferta -> oferta.codigoPartida.equals(codigoPartida)
                        && oferta.idComprador.equals(idJugador) && oferta.expira.isAfter(ahora))
                .map(this::dtoOferta)
                .toList();
        ofertas.values().removeIf(oferta -> !oferta.expira.isAfter(ahora));
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/partida/{codigoPartida}/ofertas/{idOferta}/responder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> responderOferta(
            @PathVariable("codigoPartida") String codigoPartida,
            @PathVariable("idOferta") String idOferta,
            @RequestBody Map<String, Object> datos,
            HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idComprador = ResolutorJugadorSesion.resolver(partida, request);
        if (!participacion.pertenece(partida, idComprador)) return error(HttpStatus.FORBIDDEN, "No pertenecés a esta partida.");
        OfertaVenta oferta = ofertas.get(idOferta);
        if (!esOfertaVigente(oferta, codigoPartida, idComprador)) {
            return error(HttpStatus.CONFLICT, "La oferta ya no está disponible.");
        }
        synchronized (oferta) {
            if (!Boolean.TRUE.equals(datos.get("aceptar"))) {
                ofertas.remove(idOferta, oferta);
                return ResponseEntity.ok(Map.of("estado", "rechazada"));
            }
            if (!puedeParticipar(partida, idComprador)) {
                return error(HttpStatus.FORBIDDEN, "No podés aceptar ofertas en esta partida.");
            }
            return completarTransferencia(partida, oferta);
        }
    }

    private ResponseEntity<Map<String, Object>> completarTransferencia(Partida partida, OfertaVenta oferta) {
        try {
            CasilleroPropiedad casillero = buscarCasillero(partida, oferta.posicion);
            if (casillero == null || casillero.getPropiedad() == null) {
                return error(HttpStatus.CONFLICT, "La propiedad ya no está disponible.");
            }
            Long idPropiedad = casillero.getPropiedad().getId();
            servicioPropiedad.transferirPropiedadParaJugadores(idPropiedad,
                    oferta.idVendedor, oferta.idComprador, oferta.precio);
            Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(idPropiedad);
            Map<String, Object> dto = dtoPropiedad(casillero, propiedad);
            dto.put("precioVenta", oferta.precio);
            Jugador vendedor = servicioPartida.obtenerJugadorPorId(oferta.idVendedor);
            Jugador comprador = servicioPartida.obtenerJugadorPorId(oferta.idComprador);
            dto.put("idVendedor", oferta.idVendedor);
            dto.put("dineroVendedor", vendedor.getDinero());
            dto.put("bancarrotaVendedor", vendedor.getEnBancarrota());
            dto.put(ID_JUGADOR, oferta.idComprador);
            dto.put(DINERO_JUGADOR, comprador.getDinero());
            dto.put("enBancarrota", comprador.getEnBancarrota());
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + oferta.codigoPartida + "/propiedades", dto);
            NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, oferta.codigoPartida);
            ofertas.remove(oferta.id, oferta);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return error(HttpStatus.CONFLICT,
                    ex.getMessage() == null ? "No se pudo aceptar la oferta." : ex.getMessage());
        }
    }

    private Map<String, Object> dtoPropiedad(CasilleroPropiedad casillero, Propiedad propiedad) {
        Jugador propietario = propiedad.getPropietario();
        Map<String, Object> dto = new HashMap<>();
        dto.put(POSICION, casillero.getPosicion());
        dto.put("nombre", casillero.getNombre());
        dto.put("descripcion", casillero.getDescripcion());
        dto.put("precioCompra", propiedad.getPrecioCompra());
        dto.put("alquiler", servicioPropiedad.calcularAlquiler(casillero, propiedad));
        dto.put("propietario", propietario == null ? null : propietario.getApodo());
        dto.put("propietarioId", propietario == null ? null : propietario.getId());
        dto.put("disponible", propietario == null);
        GrupoPropiedades grupo = POSICIONES_PARADA.contains(casillero.getPosicion()) ? null : propiedad.getGrupo();
        dto.put("grupo", grupo == null ? null : grupo.name());
        return dto;
    }

    private Map<String, Object> dtoOferta(OfertaVenta oferta) {
        Map<String, Object> dto = new HashMap<>();
        CasilleroPropiedad casillero = buscarCasillero(servicioPartida.obtenerPartida(oferta.codigoPartida), oferta.posicion);
        dto.put("idOferta", oferta.id);
        dto.put(POSICION, oferta.posicion);
        dto.put("precio", oferta.precio);
        dto.put("nombrePropiedad", casillero == null ? "Propiedad" : casillero.getNombre());
        dto.put("apodoVendedor", servicioPartida.obtenerJugadorPorId(oferta.idVendedor).getApodo());
        return dto;
    }

    private CasilleroPropiedad buscarCasillero(Partida partida, Integer posicion) {
        if (partida == null) return null;
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> partida.getId().equals(casillero.getPartidaId())
                        && posicion.equals(casillero.getPosicion()))
                .findFirst().orElse(null);
    }

    private Boolean participantesValidos(Partida partida, Long vendedor, Long comprador) {
        return vendedor != null && comprador != null && !vendedor.equals(comprador)
                && puedeParticipar(partida, vendedor) && puedeParticipar(partida, comprador);
    }

    private Boolean puedeParticipar(Partida partida, Long idJugador) {
        Jugador jugador = idJugador == null ? null : servicioPartida.obtenerJugadorPorId(idJugador);
        return participacion.permiteAdministrarPropiedades(partida, jugador, idJugador);
    }


    private Boolean perteneceAlVendedor(CasilleroPropiedad casillero, Long idVendedor) {
        return casillero != null && casillero.getPropiedad() != null
                && casillero.getPropiedad().getPropietario() != null
                && idVendedor.equals(casillero.getPropiedad().getPropietario().getId());
    }

    private Boolean esOfertaVigente(OfertaVenta oferta, String codigoPartida, Long idComprador) {
        return oferta != null && oferta.codigoPartida.equals(codigoPartida)
                && oferta.idComprador.equals(idComprador) && oferta.expira.isAfter(Instant.now());
    }


    private Long numeroLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : null;
    }

    private Integer numeroInteger(Object valor) {
        if (!(valor instanceof Number numero)) return null;
        Long precio = numero.longValue();
        return precio < 0 || precio > Integer.MAX_VALUE ? null : precio.intValue();
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(CLAVE_ERROR, mensaje));
    }

    private static class OfertaVenta {
        private final String id;
        private final String codigoPartida;
        private final Integer posicion;
        private final Long idVendedor;
        private final Long idComprador;
        private final Integer precio;
        private final Instant expira;

        private OfertaVenta(String id, String codigoPartida, Integer posicion, Long idVendedor,
                            Long idComprador, Integer precio, Instant expira) {
            this.id = id;
            this.codigoPartida = codigoPartida;
            this.posicion = posicion;
            this.idVendedor = idVendedor;
            this.idComprador = idComprador;
            this.precio = precio;
            this.expira = expira;
        }
    }
}
