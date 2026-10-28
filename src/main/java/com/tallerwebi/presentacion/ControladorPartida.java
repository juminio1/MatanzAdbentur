package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioJugada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.coordinadores.CoordinadorTurnoPartida;
import com.tallerwebi.presentacion.mapeadores.MapeadorMovimiento;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

/**
 * Atiendo el tablero, los turnos, las tiradas y el abandono de la partida.
 * Las acciones sobre propiedades están en {@link ControladorPropiedadesPartida}.
 */
@Controller
public class ControladorPartida {

    private static final String CLAVE_ERROR = "error";
    private static final String SESION_NOMBRE = "NOMBRE";
    private static final String CLAVE_ID_JUGADOR = "idJugador";
    private static final String ERROR_JUGADOR_AJENO = "No pertenecés a esta partida.";
    private static final String PARAM_CODIGO_PARTIDA = "codigoPartida";
    private static final String VISTA_PARTIDA = "partida";
    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";

    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioJugada servicioJugada;
    private final ServicioParticipacion participacion;
    private final NotificadorPartida notificadorPartida;
    private final MapeadorPropiedad mapeadorPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
            ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
            NotificadorPartida notificadorPartida, ServicioJugada servicioJugada,
            ServicioParticipacion participacion) {
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioJugada = servicioJugada;
        this.participacion = participacion;
        this.notificadorPartida = notificadorPartida;
        this.mapeadorPropiedad = new MapeadorPropiedad(servicioCasillero, servicioPropiedad);
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
    }

    @PostMapping("/partida/{codigoPartida}/abandonar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> abandonarPartida(
            @PathVariable("codigoPartida") String codigoPartida, HttpServletRequest request) {
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
            Partida partida, Jugador jugador) {

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
        model.put("propiedadesTablero", mapeadorPropiedad.catalogoTablero(partidaReal));

        return new ModelAndView(VISTA_PARTIDA, model);
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
    ) {
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
        if (idSesion == null || !idSesion.equals(jugadorActual.getId()) || !participacion.estaActivo(jugadorActual)) {
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
    ) throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        Partida partida = servicioPartida.obtenerPartida(codigoUnico);
        if (partida == null) throw new PartidaNoEncontradaException();
        Long idSesion = ResolutorJugadorSesion.resolverParticipante(partida, request, participacion);
        if (!Objects.equals(idSesion, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés tirar por otro jugador.");
        }
        try {
            RespuestaMovimientoDTO respuesta = MapeadorMovimiento.desde(
                    servicioJugada.tirarYMover(codigoUnico, idJugador));
            servicioPartida.prepararTiempoTurno(codigoUnico, respuesta.calcularSegundosPresentacion());
            respuesta.setTurnoSiguiente(coordinadorTurno.obtenerTurnoActual(codigoUnico));
            DespuesDeConfirmar.ejecutar(
                    () -> notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoUnico, respuesta));
            return respuesta;
        } catch (TiradaNoPermitidaException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
    }

    @ExceptionHandler(JugadorNoEncontradoException.class)
    public ResponseEntity<String> manejarJugadorNoEncontrado(JugadorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); //le devuelve al frontend un código 404 de "not found" y el mensaje de la excepción
    }
}
