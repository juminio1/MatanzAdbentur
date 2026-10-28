package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.excepcion.TiradasFaltantesException;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST: consulta del evento y tiradas manuales de jugadores autenticados. */
@RestController
@RequestMapping("/partida/{codigoPartida}/evento-global")
public class ControladorEventoGlobal {

    private final ServicioPartida servicioPartida;
    private final ServicioEvento servicioEvento;
    private final ServicioParticipacion participacion;

    public ControladorEventoGlobal(
            ServicioPartida servicioPartida,
            ServicioEvento servicioEvento,
            ServicioParticipacion participacion) {
        this.servicioPartida = servicioPartida;
        this.servicioEvento = servicioEvento;
        this.participacion = participacion;
    }

    @GetMapping
    public ResponseEntity<EstadoEventoGlobal> consultarEvento(
            @PathVariable("codigoPartida") String codigoPartida,
            HttpServletRequest request) {
        validarAcceso(codigoPartida, request);
        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal(codigoPartida);
        return estado == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(estado);
    }

    @PostMapping("/tirar")
    public ResponseEntity<Map<String, Integer>> tirarDadosEvento(@PathVariable("codigoPartida") String codigoPartida,HttpServletRequest request)
            throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {
        Long idJugador = validarAcceso(codigoPartida, request);
        ResultadoTirada resultado = servicioEvento.tirarDadosEventoGlobal(codigoPartida, idJugador);
        return ResponseEntity.ok(Map.of(
                "dado1", resultado.getDado1(),
                "dado2", resultado.getDado2(),
                "suma", resultado.sumarDados()));
    }

    private Long validarAcceso(String codigoPartida, HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La partida no existe.");
        }
        if (ResolutorJugadorSesion.resolverUsuarioSesionId(request) == null
                && request.getSession().getAttribute("NOMBRE") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debés iniciar sesión.");
        }
        Long idJugador = ResolutorJugadorSesion.resolverAutenticado(partida, request);
        if (!participacion.pertenece(partida, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No pertenecés a esta partida.");
        }
        return idJugador;
    }

    @ExceptionHandler({DatosEnventoInvalidoExceptions.class,
            TiradaDadoInvalidaException.class, TiradasFaltantesException.class})
    public ResponseEntity<Map<String, String>> manejarEventoNoDisponible(Exception exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", mensajeSeguro(exception)));
    }

    @ExceptionHandler(JugadorNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarJugadorNoEncontrado(Exception exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", mensajeSeguro(exception)));
    }

    private String mensajeSeguro(Exception exception) {
        return exception.getMessage() == null ? "No se pudo completar la operación" : exception.getMessage();
    }

    @ExceptionHandler({PartidaNoEncontradaException.class, EventoInexistenteException.class})
    public ResponseEntity<Map<String, String>> manejarRecursoNoEncontrado(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage() == null ? "Recurso no encontrado" : exception.getMessage()));
    }
}
