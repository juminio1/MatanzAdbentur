package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.coordinadores.CoordinadorTurnoPartida;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Objects;

/**
 * Atiendo las propiedades de una partida: consulta, compra, venta al banco y mejoras.
 * Las ofertas entre jugadores están en {@link ControladorOfertaPropiedad}.
 */
@Controller
public class ControladorPropiedadesPartida {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorPropiedadesPartida.class);
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
    private final MapeadorPropiedad mapeadorPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;

    @Autowired
    public ControladorPropiedadesPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
            ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
            ServicioGestionQuiebra servicioGestionQuiebra, ServicioParticipacion participacion) {
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.servicioGestionQuiebra = servicioGestionQuiebra;
        this.participacion = participacion;
        this.mapeadorPropiedad = new MapeadorPropiedad(servicioCasillero, servicioPropiedad);
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
    }

    // ---------- Consulta ----------

    @GetMapping("/partida/{codigoPartida}/propiedades/{posicion}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerInfoPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null) {
            return ResponseEntity.ok(mapeadorPropiedad.desdeCatalogo(posicion));
        }
        return ResponseEntity.ok(mapeadorPropiedad.desdePropiedad(casillero, casillero.getPropiedad()));
    }

    @GetMapping("/partida/{codigoPartida}/propiedades")
    @ResponseBody
    public ResponseEntity<?> obtenerPropiedadesTablero(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!participacion.pertenece(partida, idJugador)) {
            return error(HttpStatus.FORBIDDEN, ERROR_JUGADOR_AJENO);
        }
        return ResponseEntity.ok(mapeadorPropiedad.catalogoTablero(partida));
    }

    // ---------- Mejora ----------

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/subir-alquiler")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirAlquilerPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!puedeParticiparEnPartida(partida, idJugador)) {
            return error(HttpStatus.FORBIDDEN, "No podés administrar propiedades en esta partida.");
        }
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (!esTurnoDeJugador(jugadorActual, idJugador)) {
            return error(HttpStatus.FORBIDDEN, "Solo podés subir alquileres durante tu turno.");
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (!esPropietarioDelCasillero(casillero, idJugador)) {
            return error(HttpStatus.FORBIDDEN, "Solo podés subir el alquiler de tus propiedades.");
        }
        return procesarMejoraPropiedad(codigoPartida, idJugador, casillero);
    }

    private ResponseEntity<Map<String, Object>> procesarMejoraPropiedad(
            String codigoPartida,
            Long idJugador,
            CasilleroPropiedad casillero
    ) {
        try {
            Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
            servicioPropiedad.mejorarPropiedad(casillero.getPropiedad().getId(), jugador);
            Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId());
            Map<String, Object> dto = mapeadorPropiedad.desdePropiedad(casillero, propiedad);
            Jugador jugadorActualizado = servicioPartida.obtenerJugadorPorId(idJugador);
            dto.put(CLAVE_ID_JUGADOR, idJugador);
            dto.put(CLAVE_DINERO_JUGADOR, jugadorActualizado.getDinero());
            dto.put(CLAVE_EN_BANCARROTA, jugadorActualizado.getEnBancarrota());
            publicarPropiedad(codigoPartida, dto);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return error(HttpStatus.CONFLICT, obtenerMensajeError(ex, "No se pudo subir el alquiler."));
        }
    }

    // ---------- Compra ----------

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/comprar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comprarPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request
    ) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        Long idSesion = ResolutorJugadorSesion.resolverParticipante(partida, request, participacion);
        Jugador actual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (actual == null) return error(HttpStatus.CONFLICT, "No hay un jugador activo en esta partida.");
        if (!esTurnoDeJugador(actual, idSesion)) return error(HttpStatus.FORBIDDEN, "No podés comprar por otro jugador.");
        try {
            ServicioPropiedad.CompraPreparada compra = servicioPropiedad.prepararCompra(codigoPartida, posicion);
            if (compra == null) return ResponseEntity.notFound().build();
            if (!Objects.equals(compra.jugador().getId(), idSesion)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            return procesarCompraPropiedad(compra);
        } catch (EstadoPartidaInvalidoException ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.error("Error interno al comprar la propiedad {} en la partida {}", posicion, codigoPartida, ex);
            String detalle = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al comprar la propiedad: " + detalle);
        }
    }

    private ResponseEntity<Map<String, Object>> procesarCompraPropiedad(ServicioPropiedad.CompraPreparada compra) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPartida = compra.codigoPartida();
        Jugador jugador = compra.jugador();
        CasilleroPropiedad casillero = compra.casillero();
        try {
            servicioPropiedad.comprarPropiedadParaJugador(casillero.getPropiedad().getId(), jugador.getId());
            Jugador compradorActualizado = servicioPartida.obtenerJugadorPorId(jugador.getId());
            Map<String, Object> actualizado = mapeadorPropiedad.desdePropiedad(casillero,
                    servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId()));
            actualizado.put("idJugadorPropietario", jugador.getId());
            actualizado.put(CLAVE_DINERO_JUGADOR, compradorActualizado.getDinero());
            actualizado.put(CLAVE_EN_BANCARROTA, compradorActualizado.getEnBancarrota());
            publicarPropiedad(codigoPartida, actualizado);
            NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
            TurnoDTO turno = coordinadorTurno.avanzarYObtenerTurno(codigoPartida);
            if (turno != null) actualizado.put("idJugadorActual", turno.getIdJugadorActual());
            return ResponseEntity.ok(actualizado);
        } catch (DineroInsuficienteException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("codigoError", "DINERO_INSUFICIENTE", CLAVE_ERROR, ex.getMessage()));
        } catch (RuntimeException ex) {
            return error(HttpStatus.CONFLICT, obtenerMensajeError(ex, "Error al guardar la compra de la propiedad."));
        }
    }

    // ---------- Venta al banco ----------

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/vender")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> venderPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request
    ) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partida, request);
        CasilleroPropiedad casillero;
        try {
            casillero = servicioPropiedad.prepararVenta(partida, jugadorSesionId, posicion);
        } catch (EstadoPartidaInvalidoException ex) {
            return error(HttpStatus.FORBIDDEN, ex.getMessage());
        }
        if (casillero == null) return error(HttpStatus.CONFLICT, "La propiedad no está disponible para vender.");
        try {
            return procesarVentaPropiedad(codigoPartida, jugadorSesionId, casillero);
        } catch (RuntimeException ex) {
            return error(HttpStatus.CONFLICT, obtenerMensajeError(ex, "No se pudo vender la propiedad."));
        }
    }

    private ResponseEntity<Map<String, Object>> procesarVentaPropiedad(
            String codigoPartida, Long idVendedor, CasilleroPropiedad casillero) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Long idPropiedad = casillero.getPropiedad().getId();
        Integer precioVenta = servicioPropiedad.venderPropiedadParaJugador(idPropiedad, idVendedor);
        if (servicioGestionQuiebra.obtenerDeudaPendiente(codigoPartida, idVendedor) != null) {
            servicioGestionQuiebra.evaluarDeuda(codigoPartida, idVendedor);
        }
        Jugador vendedorActualizado = servicioPartida.obtenerJugadorPorId(idVendedor);
        Map<String, Object> actualizado = mapeadorPropiedad.desdePropiedad(casillero,
                servicioPropiedad.obtenerPropiedadExistente(idPropiedad));
        actualizado.put(CLAVE_ID_JUGADOR, idVendedor);
        actualizado.put(CLAVE_DINERO_JUGADOR, vendedorActualizado.getDinero());
        actualizado.put(CLAVE_PRECIO_VENTA, precioVenta);
        publicarPropiedad(codigoPartida, actualizado);
        NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
        return ResponseEntity.ok(actualizado);
    }

    // ---------- Auxiliares ----------

    private void publicarPropiedad(String codigoPartida, Map<String, Object> dto) {
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", dto);
    }

    private boolean puedeParticiparEnPartida(Partida partida, Long idJugador) {
        if (idJugador == null) return false;
        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        return participacion.permiteAdministrarPropiedades(partida, jugador, idJugador);
    }

    private boolean esTurnoDeJugador(Jugador jugadorActual, Long idJugador) {
        return jugadorActual != null && Objects.equals(idJugador, jugadorActual.getId());
    }

    private boolean esPropietarioDelCasillero(CasilleroPropiedad casillero, Long idJugador) {
        return casillero != null
                && casillero.getPropiedad().getPropietario() != null
                && Objects.equals(idJugador, casillero.getPropiedad().getPropietario().getId());
    }

    private String obtenerMensajeError(RuntimeException excepcion, String mensajePredeterminado) {
        String mensaje = excepcion.getMessage();
        return mensaje == null || mensaje.isBlank() ? mensajePredeterminado : mensaje;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(CLAVE_ERROR, mensaje));
    }
}
