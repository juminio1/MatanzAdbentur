package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Identifica siempre al participante desde su sesión, nunca desde el cuerpo de la petición. */
@Controller
@ResponseBody
public class ControladorEventoGlobal {
    private final ServicioEvento eventos;
    private final ServicioPartida partidas;
    private final ServicioParticipacion participacion;

    public ControladorEventoGlobal(ServicioEvento eventos, ServicioPartida partidas,
            ServicioParticipacion participacion) {
        this.eventos = eventos;
        this.partidas = partidas;
        this.participacion = participacion;
    }

    @GetMapping("/partida/{codigo}/evento-global")
    public ResponseEntity<Map<String, Object>> consultar(@PathVariable("codigo") String codigo,
            HttpServletRequest request) {
        return respuesta(eventos.consultar(codigo, autenticar(codigo, request)));
    }

    @PostMapping("/partida/{codigo}/evento-global/tirar")
    public ResponseEntity<Map<String, Object>> tirar(@PathVariable("codigo") String codigo,
            @RequestParam("ronda") int ronda, @RequestParam("etapa") int etapa,
            HttpServletRequest request) {
        return respuesta(eventos.tirar(codigo, autenticar(codigo, request), ronda, etapa));
    }

    @PostMapping("/partida/{codigo}/evento-global/continuar")
    public ResponseEntity<Map<String, Object>> continuar(@PathVariable("codigo") String codigo,
            @RequestParam("ronda") int ronda, HttpServletRequest request) {
        return respuesta(eventos.confirmar(codigo, autenticar(codigo, request), ronda));
    }

    private Long autenticar(String codigo, HttpServletRequest request) {
        Partida partida = partidas.obtenerPartidaParaVisualizacion(codigo);
        return ResolutorJugadorSesion.resolverParticipante(partida, request, participacion);
    }

    private ResponseEntity<Map<String, Object>> respuesta(Map<String, Object> estado) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(estado);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflicto(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> prohibido(SecurityException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", error.getMessage()));
    }
}
