package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.excepcion.OfertaInvalidaException;
import com.tallerwebi.dominio.excepcion.OfertaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.OfertaNoPermitidaException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad.Oferta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad.OfertaAceptada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/** Atiende las ofertas de venta de propiedades entre jugadores. */
@Controller
public class ControladorOfertaPropiedad {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorOfertaPropiedad.class);
    private static final String CLAVE_ERROR = "error";
    private static final String CLAVE_ID_JUGADOR = "idJugador";
    private static final String CLAVE_DINERO_JUGADOR = "dineroJugador";
    private static final String CLAVE_EN_BANCARROTA = "enBancarrota";
    private static final String CLAVE_PRECIO_VENTA = "precioVenta";
    private static final String ERROR_JUGADOR_AJENO = "No pertenecés a esta partida.";
    private static final String PARAM_CODIGO_PARTIDA = "codigoPartida";
    private static final String PARAM_POSICION = "posicion";
    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";

    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioCasillero servicioCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final ServicioGestionQuiebra servicioGestionQuiebra;
    private final ServicioParticipacion participacion;
    private final ServicioOfertaPropiedad servicioOferta;
    private final MapeadorPropiedad mapeadorPropiedad;

    public ControladorOfertaPropiedad(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
            ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
            ServicioGestionQuiebra servicioGestionQuiebra, ServicioParticipacion participacion,
            ServicioOfertaPropiedad servicioOferta) {
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.servicioGestionQuiebra = servicioGestionQuiebra;
        this.participacion = participacion;
        this.servicioOferta = servicioOferta;
        this.mapeadorPropiedad = new MapeadorPropiedad(servicioCasillero, servicioPropiedad);
    }

    // ---------- Ofertas entre jugadores ----------

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearOferta(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            @RequestBody Map<String, Object> datos,
            HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        Long idVendedor = ResolutorJugadorSesion.resolver(partida, request);
        try {
            Oferta oferta = servicioOferta.crearOferta(partida, posicion, idVendedor,
                    numeroLong(datos.get("idJugadorComprador")), numeroInteger(datos.get("precio")));
            publicarOferta(oferta);
            return ResponseEntity.ok(Map.of("idOferta", oferta.id(), "estado", "pendiente"));
        } catch (OfertaInvalidaException ex) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (PropiedadNoDisponibleException ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @GetMapping("/partida/{codigoPartida}/ofertas-pendientes")
    @ResponseBody
    public ResponseEntity<?> ofertasPendientes(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!participacion.pertenece(partida, idJugador)) return error(HttpStatus.FORBIDDEN, ERROR_JUGADOR_AJENO);
        List<Map<String, Object>> pendientes = servicioOferta.ofertasPendientes(codigoPartida, idJugador).stream()
                .map(this::dtoOferta)
                .toList();
        return ResponseEntity.ok(pendientes);
    }

    @PostMapping("/partida/{codigoPartida}/ofertas/{idOferta}/responder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> responderOferta(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable("idOferta") String idOferta,
            @RequestBody Map<String, Object> datos,
            HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idComprador = ResolutorJugadorSesion.resolver(partida, request);
        if (!participacion.pertenece(partida, idComprador)) return error(HttpStatus.FORBIDDEN, ERROR_JUGADOR_AJENO);
        try {
            if (!Boolean.TRUE.equals(datos.get("aceptar"))) {
                servicioOferta.rechazar(codigoPartida, idOferta, idComprador);
                return ResponseEntity.ok(Map.of("estado", "rechazada"));
            }
            OfertaAceptada aceptada = servicioOferta.aceptar(partida, idOferta, idComprador);
            servicioGestionQuiebra.intentarResolverDeuda(codigoPartida, aceptada.oferta().idVendedor());
            return notificarTransferencia(aceptada);
        } catch (OfertaNoPermitidaException ex) {
            return error(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (OfertaNoDisponibleException ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        } catch (RuntimeException ex) {
            return error(HttpStatus.CONFLICT, obtenerMensajeError(ex, "No se pudo aceptar la oferta."));
        }
    }

    private ResponseEntity<Map<String, Object>> notificarTransferencia(OfertaAceptada aceptada) {
        Oferta oferta = aceptada.oferta();
        CasilleroPropiedad casillero = aceptada.casillero();
        Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId());
        Map<String, Object> dto = mapeadorPropiedad.desdePropiedad(casillero, propiedad);
        Jugador vendedor = servicioPartida.obtenerJugadorPorId(oferta.idVendedor());
        Jugador comprador = servicioPartida.obtenerJugadorPorId(oferta.idComprador());
        dto.put(CLAVE_PRECIO_VENTA, oferta.precio());
        dto.put("idVendedor", oferta.idVendedor());
        dto.put("dineroVendedor", vendedor.getDinero());
        dto.put("bancarrotaVendedor", vendedor.getEnBancarrota());
        dto.put(CLAVE_ID_JUGADOR, oferta.idComprador());
        dto.put(CLAVE_DINERO_JUGADOR, comprador.getDinero());
        dto.put(CLAVE_EN_BANCARROTA, comprador.getEnBancarrota());
        publicarPropiedad(oferta.codigoPartida(), dto);
        NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, oferta.codigoPartida());
        return ResponseEntity.ok(dto);
    }

    /** Avisa a la partida de la oferta nueva. Solo la muestra el jugador que es el comprador. */
    private void publicarOferta(Oferta oferta) {
        try {
            notificadorWebSocket.convertAndSend(
                    TOPIC_PARTIDA_PREFIX + oferta.codigoPartida() + "/ofertas", dtoOferta(oferta));
        } catch (RuntimeException ex) {
            // La oferta ya quedó guardada: el comprador la ve igual al consultar sus ofertas pendientes.
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("No se pudo avisar la oferta {} por WebSocket", oferta.id(), ex);
            }
        }
    }

    private Map<String, Object> dtoOferta(Oferta oferta) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("idOferta", oferta.id());
        dto.put(PARAM_POSICION, oferta.posicion());
        dto.put("precio", oferta.precio());
        dto.put("nombrePropiedad", oferta.nombrePropiedad() == null ? "Propiedad" : oferta.nombrePropiedad());
        dto.put("apodoVendedor", oferta.apodoVendedor());
        dto.put("idComprador", oferta.idComprador());
        return dto;
    }

    private void publicarPropiedad(String codigoPartida, Map<String, Object> dto) {
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", dto);
    }

    private String obtenerMensajeError(RuntimeException excepcion, String mensajePredeterminado) {
        String mensaje = excepcion.getMessage();
        return mensaje == null || mensaje.isBlank() ? mensajePredeterminado : mensaje;
    }

    private Long numeroLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : null;
    }

    private Integer numeroInteger(Object valor) {
        if (!(valor instanceof Number numero)) return null;
        long precio = numero.longValue();
        return precio < 0 || precio > Integer.MAX_VALUE ? null : (int) precio;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(CLAVE_ERROR, mensaje));
    }
}
