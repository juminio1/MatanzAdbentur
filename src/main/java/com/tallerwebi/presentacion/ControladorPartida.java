package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.coordinadores.CoordinadorTurnoPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atiendo las acciones de la partida y sus propiedades.
 */
@Controller
public class ControladorPartida {
    private final ServicioParticipacion participacion;


    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorPartida.class);
    private static final String CLAVE_ERROR = "error";
    private static final String SESION_NOMBRE = "NOMBRE";
    private static final String CLAVE_ID_JUGADOR = "idJugador";
    private static final String ERROR_JUGADOR_AJENO = "No pertenecés a esta partida.";
    private static final String PARAM_CODIGO_PARTIDA = "codigoPartida";
    private static final String PARAM_POSICION = "posicion";
    private static final String VISTA_PARTIDA = "partida";
    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private final com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad mapeadorPropiedad;
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioCasillero servicioCasillero;
    private final ServicioJugada servicioJugada;
    private final ServicioPropiedad servicioPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;
    private final NotificadorPartida notificadorPartida;
    private final ServicioGestionQuiebra servicioGestionQuiebra;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
            ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
            NotificadorPartida notificadorPartida, ServicioGestionQuiebra servicioGestionQuiebra,
            ServicioJugada servicioJugada, ServicioParticipacion participacion) {
        this.participacion = participacion;
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.mapeadorPropiedad = new com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad(servicioCasillero, servicioPropiedad);
        this.notificadorPartida = notificadorPartida;
        this.servicioGestionQuiebra = servicioGestionQuiebra;
        this.servicioJugada = servicioJugada;
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
    }

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, ERROR_JUGADOR_AJENO)
            );
        }

        return ResponseEntity.ok(obtenerCatalogoTablero(partida));
    }

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, "No podés administrar propiedades en esta partida.")
            );
        }
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (!esTurnoDeJugador(jugadorActual, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, "Solo podés subir alquileres durante tu turno.")
            );
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (!esPropietarioDelCasillero(casillero, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, "Solo podés subir el alquiler de tus propiedades.")
            );
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
            Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(
                    casillero.getPropiedad().getId()
            );
            Map<String, Object> dto = mapeadorPropiedad.desdePropiedad(casillero, propiedad);
            Jugador jugadorActualizado = servicioPartida.obtenerJugadorPorId(idJugador);
            dto.put(CLAVE_ID_JUGADOR, idJugador);
            dto.put("dineroJugador", jugadorActualizado.getDinero());
            dto.put("enBancarrota", jugadorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(
                    TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades",
                    dto
            );
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            String mensaje =
                    ex.getMessage() == null ? "No se pudo subir el alquiler." : ex.getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/comprar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comprarPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request
    ) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || !servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        Long idSesion = resolverJugadorAutenticado(partida, request);
        Jugador actual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (actual == null) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, "No hay un jugador activo en esta partida."));
        if (!esTurnoDeJugador(actual, idSesion)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR, "No podés comprar por otro jugador."));
        }
        try {
            ServicioPropiedad.CompraPreparada compra = servicioPropiedad.prepararCompra(codigoPartida, posicion);
            if (compra == null) return ResponseEntity.notFound().build();
            if (!java.util.Objects.equals(compra.jugador().getId(), idSesion)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            return procesarCompraPropiedad(compra);
        } catch (EstadoPartidaInvalidoException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Error interno al comprar la propiedad {} en la partida {}",
                    posicion,
                    codigoPartida,
                    ex
            );
            String detalle =
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(CLAVE_ERROR, "Error interno al comprar la propiedad: " + detalle)
            );
        }
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/vender")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> venderPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request
    ) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) {
            return ResponseEntity.notFound().build();
        }
        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partida, request);
        CasilleroPropiedad casillero;
        try {
            casillero = servicioPropiedad.prepararVenta(partida, jugadorSesionId, posicion);
        } catch (EstadoPartidaInvalidoException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
        if (casillero == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(CLAVE_ERROR, "La propiedad no está disponible para vender.")
            );
        }
        try {
            Integer precioVenta = servicioPropiedad.venderPropiedadParaJugador(
                    casillero.getPropiedad().getId(), jugadorSesionId);
            if (servicioGestionQuiebra.obtenerDeudaPendiente(codigoPartida, jugadorSesionId) != null) {
                servicioGestionQuiebra.evaluarDeuda(codigoPartida, jugadorSesionId);
            }
            Jugador vendedorActualizado = servicioPartida.obtenerJugadorPorId(jugadorSesionId);
            Map<String, Object> actualizado = mapeadorPropiedad.desdePropiedad(casillero,
                    servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId()));
            actualizado.put(CLAVE_ID_JUGADOR, jugadorSesionId);
            actualizado.put("dineroJugador", vendedorActualizado.getDinero());
            actualizado.put("precioVenta", precioVenta);
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", actualizado);
            NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException ex) {
            String mensaje = obtenerMensajeError(ex, "No se pudo vender la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    @PostMapping("/partida/{codigoPartida}/abandonar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> abandonarPartida(
            @PathVariable("codigoPartida") String codigoPartida, HttpServletRequest request) throws ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);

        if (partida == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(CLAVE_ERROR, "La partida no existe.")
            );
        }

        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);

        if (!participacion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, ERROR_JUGADOR_AJENO)
            );
        }
        Jugador jugador = partida.getJugadores().stream().filter(j -> idJugador.equals(j.getId())).findFirst().orElseThrow(()
                -> new IllegalArgumentException("El jugador no pertenece a la partida."));

        return ejecutarAbandono(partida, jugador);
    }

    private ResponseEntity<Map<String, Object>> ejecutarAbandono(
            Partida partida, Jugador jugador) throws ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {

        try {
            servicioPartida.abandonarPartida(partida.getCodigoUnico(), jugador.getId());

            notificadorPartida.notificarAbandonoPorWebSocket(partida, jugador);

            return ResponseEntity.ok(Map.of("mensaje", "Abandonaste la partida correctamente."));

        } catch (PartidaNoEncontradaException excepcion) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(CLAVE_ERROR, "No se encontró la partida."));

        } catch (IllegalArgumentException excepcion) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(CLAVE_ERROR, "No se encontró el jugador."));
        }
    }

    private Boolean puedeParticiparEnPartida(Partida partida, Long idJugador) {
        if (idJugador == null) return false;
        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        return participacion.permiteAdministrarPropiedades(partida, jugador, idJugador);
    }

    private ResponseEntity<Map<String, Object>> procesarCompraPropiedad(ServicioPropiedad.CompraPreparada compra) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPartida = compra.codigoPartida();
        Jugador jugador = compra.jugador();
        CasilleroPropiedad casillero = compra.casillero();
        try {
            servicioPropiedad.comprarPropiedadParaJugador(
                    casillero.getPropiedad().getId(),
                    jugador.getId()
            );
            Jugador compradorActualizado = servicioPartida.obtenerJugadorPorId(jugador.getId());
            Map<String, Object> actualizado = mapeadorPropiedad.desdePropiedad(
                    casillero,
                    servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId())
            );
            actualizado.put("idJugadorPropietario", jugador.getId());
            actualizado.put("dineroJugador", compradorActualizado.getDinero());
            actualizado.put("enBancarrota", compradorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(
                    TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades",
                    actualizado
            );
            NotificadorPartida.notificarSaldos(
                    notificadorWebSocket,
                    servicioPartida,
                    codigoPartida
            );
            TurnoDTO turno = coordinadorTurno.avanzarYObtenerTurno(codigoPartida);
            if (turno != null) actualizado.put("idJugadorActual", turno.getIdJugadorActual());
            return ResponseEntity.ok(actualizado);
        } catch (DineroInsuficienteException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("codigoError", "DINERO_INSUFICIENTE", CLAVE_ERROR, ex.getMessage())
            );
        } catch (RuntimeException ex) {
            String mensaje = obtenerMensajeError(ex, "Error al guardar la compra de la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private Boolean esTurnoDeJugador(Jugador jugadorActual, Long idJugador) {
        return jugadorActual != null && idJugador.equals(jugadorActual.getId());
    }

    private Boolean esPropietarioDelCasillero(CasilleroPropiedad casillero, Long idJugador) {
        return (
                casillero != null &&
                        casillero.getPropiedad().getPropietario() != null &&
                        idJugador.equals(casillero.getPropiedad().getPropietario().getId())
        );
    }

    private String obtenerMensajeError(RuntimeException excepcion, String mensajePredeterminado) {
        String mensaje = excepcion.getMessage();
        return mensaje == null || mensaje.isBlank() ? mensajePredeterminado : mensaje;
    }

    @GetMapping("/partida")
    public ModelAndView verTablero(
            @RequestParam(value = "codigo", required = false) String codigoUnico,
            HttpServletRequest request
    ) {
        String codigoFinal = ResolutorJugadorSesion.resolverCodigoPartida(codigoUnico, request);
        if (codigoFinal == null) {
            return new ModelAndView("redirect:/home");
        }

        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);
        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + codigoFinal);
        }

        Long usuarioSesionId = ResolutorJugadorSesion.resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute(SESION_NOMBRE);

        if (usuarioSesionId == null && nombreSesion == null) {
            return new ModelAndView("redirect:/login");
        }

        Long jugadorSesionId = ResolutorJugadorSesion.resolver(
                partidaReal,
                usuarioSesionId,
                nombreSesion
        );
        Jugador jugadorSesion =
                jugadorSesionId == null ? null : servicioPartida.obtenerJugadorPorId(jugadorSesionId);

        Map<String, Object> model = new HashMap<>();
        model.put("partida", partidaReal);
        model.put("usuarioSesionId", jugadorSesionId);
        model.put("dineroUsuarioSesion", jugadorSesion == null ? 0 : jugadorSesion.getDinero());
        model.put("usuarioEnBancarrota", jugadorSesion != null && jugadorSesion.getEnBancarrota());
        model.put("turnoActualId", resolverTurnoActualId(codigoFinal));
        model.put("propiedadesTablero", obtenerCatalogoTablero(partidaReal));

        return new ModelAndView(VISTA_PARTIDA, model);
    }

    private List<Map<String, Object>> obtenerCatalogoTablero(Partida partida) {
        List<CasilleroPropiedad> casilleros = servicioCasillero.obtenerCasillerosPropiedadDePartida(partida);

        return servicioCasillero.obtenerPosicionesPropiedad().stream().map(posicion -> casilleros.stream()
                        .filter(casillero -> posicion.equals(casillero.getPosicion()))
                        .findFirst()
                        .map(casillero -> mapeadorPropiedad.desdePropiedad(casillero, casillero.getPropiedad()))
                        .orElseGet(() -> mapeadorPropiedad.desdeCatalogo(posicion)))
                .toList();
    }

    private Long resolverTurnoActualId(String codigo) {
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigo);
        return jugador == null ? null : jugador.getId();
    }

    @GetMapping("/partida/{codigoPartida}/turno")
    @ResponseBody
    public ResponseEntity<TurnoDTO> obtenerTurnoActual(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida
    ) {
        TurnoDTO turno = coordinadorTurno.obtenerTurnoActual(codigoPartida);
        return turno == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(turno);
    }

    @PostMapping("/partida/{codigoPartida}/avanzar-turno")
    @ResponseBody
    public ResponseEntity<TurnoDTO> avanzarTurno(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            HttpServletRequest request
    ) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Long usuarioSesionId = ResolutorJugadorSesion.resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute(SESION_NOMBRE);

        if (usuarioSesionId == null && nombreSesion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jugador jugadorActual = this.servicioPartida.obtenerJugadorActual(codigoPartida);
        if (jugadorActual == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        Long idSesion = ResolutorJugadorSesion.resolverAutenticado(partida, request);
        if (idSesion == null || !esTurnoDeJugador(jugadorActual, idSesion) || !participacion.estaActivo(jugadorActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(coordinadorTurno.avanzarYObtenerTurno(codigoPartida));
    }

    @PostMapping("/partida/tirar-dados")
    @ResponseBody
    public RespuestaMovimientoDTO tirarDados(
            @RequestParam("codigoUnico") String codigoUnico,
            @RequestParam(CLAVE_ID_JUGADOR) Long idJugador,
            HttpServletRequest request
    ) throws PartidaNoEncontradaException, JugadorNoEncontradoException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = servicioPartida.obtenerPartida(codigoUnico);
        if (partida == null) throw new PartidaNoEncontradaException();
        Long idSesion = resolverJugadorAutenticado(partida, request);
        if (!java.util.Objects.equals(idSesion, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés tirar por otro jugador.");
        }
        try {
            RespuestaMovimientoDTO respuesta = com.tallerwebi.presentacion.mapeadores.MapeadorMovimiento.desde(
                    servicioJugada.tirarYMover(codigoUnico, idJugador));
            servicioPartida.prepararTiempoTurno(codigoUnico, respuesta.calcularSegundosPresentacion());
            respuesta.setTurnoSiguiente(coordinadorTurno.obtenerTurnoActual(codigoUnico));
            com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar.ejecutar(
                    () -> notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoUnico, respuesta));
            return respuesta;
        } catch (com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
    }

    private Long resolverJugadorAutenticado(Partida partida, HttpServletRequest request) {
        if (ResolutorJugadorSesion.resolverUsuarioSesionId(request) == null
                && request.getSession().getAttribute(SESION_NOMBRE) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debés iniciar sesión.");
        }
        Long idJugador = ResolutorJugadorSesion.resolverAutenticado(partida, request);
        if (!participacion.pertenece(partida, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_JUGADOR_AJENO);
        }
        return idJugador;
    }

    @ExceptionHandler(JugadorNoEncontradoException.class)
    public ResponseEntity<String> manejarJugadorNoEncontrado(JugadorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); //le devuelve al frontend un código 404 de "not found" y el mensaje de la excepción
    }
}
