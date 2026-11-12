package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.AccionTriviaInvalidaException;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import com.tallerwebi.dominio.excepcion.TriviaNoEncontradaException;
import com.tallerwebi.dominio.servicios.GestorTriviaPartida;
import com.tallerwebi.dominio.servicios.ValidadorParticipanteTrivia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.DTO.EstadoTriviaDTO;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import com.tallerwebi.presentacion.DTO.RespuestaTriviaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoRespuestaTriviaDTO;
import com.tallerwebi.presentacion.DTO.SeleccionPremioDTO;
import com.tallerwebi.presentacion.mapeadores.MapeadorTrivia;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Endpoints del minitorneo de trivia. La respuesta correcta nunca sale del backend. */
@RestController
@RequestMapping("/partida/{codigo}/trivia")
public class ControladorTrivia {

    private static final String CODIGO = "codigo";
    private static final String CLAVE_ERROR = "error";

    private final GestorTriviaPartida gestor;
    private final ServicioPartida servicioPartida;
    private final ServicioParticipacion participacion;
    private final ValidadorParticipanteTrivia validador;

    public ControladorTrivia(GestorTriviaPartida gestor, ServicioPartida servicioPartida,
                             ServicioParticipacion participacion, ValidadorParticipanteTrivia validador) {
        this.gestor = gestor;
        this.servicioPartida = servicioPartida;
        this.participacion = participacion;
        this.validador = validador;
    }

    @GetMapping
    public ResponseEntity<EstadoTriviaDTO> obtenerEstado(@PathVariable(CODIGO) String codigo,
                                                         HttpServletRequest request) {
        resolverJugador(codigo, request);
        return ResponseEntity.ok(gestor.consultarEstado(codigo, trivia -> MapeadorTrivia.estado(codigo, trivia)));
    }

    // Los eliminados también pueden verla, como espectadores.
    @GetMapping("/pregunta")
    public ResponseEntity<PreguntaTriviaDTO> obtenerPregunta(@PathVariable(CODIGO) String codigo,
                                                             HttpServletRequest request) {
        resolverJugador(codigo, request);
        return ResponseEntity.ok(gestor.consultarPregunta(codigo, MapeadorTrivia::pregunta));
    }

    @PostMapping("/respuesta")
    public ResponseEntity<ResultadoRespuestaTriviaDTO> responder(@PathVariable(CODIGO) String codigo,
                                                                 @RequestBody RespuestaTriviaDTO datos,
                                                                 HttpServletRequest request) {
        Partida partida = partidaActiva(codigo);
        Long idJugador = ResolutorJugadorSesion.resolverParticipante(partida, request, participacion);
        validador.validarHabilitado(partida, idJugador);
        if (datos == null || datos.opcion() == null) {
            throw new IllegalArgumentException("Elegí una opción.");
        }
        boolean correcta = gestor.responder(codigo, idJugador, datos.id(), datos.opcion());
        return ResponseEntity.ok(new ResultadoRespuestaTriviaDTO(correcta));
    }

    @PostMapping("/premio")
    public ResponseEntity<EstadoTriviaDTO> elegirPremio(@PathVariable(CODIGO) String codigo,
                                                        @RequestBody SeleccionPremioDTO datos,
                                                        HttpServletRequest request) {
        Long idJugador = resolverJugador(codigo, request);
        if (datos == null) {
            throw new IllegalArgumentException("Elegí un premio.");
        }
        gestor.elegirPremio(codigo, idJugador, datos.premio(), datos.posicion());
        return ResponseEntity.ok(gestor.consultarEstado(codigo, trivia -> MapeadorTrivia.estado(codigo, trivia)));
    }

    @ExceptionHandler(TriviaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> triviaNoEncontrada(TriviaNoEncontradaException ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ParticipanteTriviaNoHabilitadoException.class)
    public ResponseEntity<Map<String, String>> noHabilitado(ParticipanteTriviaNoHabilitadoException ex) {
        return error(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(AccionTriviaInvalidaException.class)
    public ResponseEntity<Map<String, String>> accionInvalida(AccionTriviaInvalidaException ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    // Responde 404 si la partida no existe o terminó, 401 sin sesión y 403 si el jugador es ajeno.
    private Long resolverJugador(String codigo, HttpServletRequest request) {
        return ResolutorJugadorSesion.resolverParticipante(partidaActiva(codigo), request, participacion);
    }

    private Partida partidaActiva(String codigo) {
        Partida partida = servicioPartida.obtenerPartida(codigo);
        if (partida == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La partida no existe o ya terminó.");
        }
        return partida;
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, RuntimeException ex) {
        String mensaje = ex.getMessage() == null ? "No se pudo completar la acción." : ex.getMessage();
        return ResponseEntity.status(estado).body(Map.of(CLAVE_ERROR, mensaje));
    }
}
