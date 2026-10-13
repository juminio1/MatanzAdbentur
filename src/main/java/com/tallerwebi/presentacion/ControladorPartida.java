package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPartida {

    private static final String VISTA_PARTIDA = "partida";
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida ,SimpMessagingTemplate notificadorWebSocket
    ) {
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioPartida=servicioPartida;
    }

    @PostMapping("/partida/tirar-dados")
    @ResponseBody
    public ResultadoTiradaDTO tirarDados() {
        return servicioPartida.tirarDados();
    }

    /*@GetMapping("/partida")
    public ModelAndView verTablero(
        @RequestParam(value = "codigo", required = false) String codigoUnico,
        HttpServletRequest request
    ) {
        Map<String, Object> model = new HashMap<>();

        String codigoFinal = codigoUnico;

        if (codigoFinal == null || codigoFinal.isBlank()) {
            codigoFinal = (String) request.getSession().getAttribute("codigoSala");
        }

        if (codigoFinal == null || codigoFinal.isBlank()) {
            return new ModelAndView("redirect:/sala-espera");
        }

        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);

        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-espera");
        }

        Long usuarioSesionId = (Long) request.getSession().getAttribute("IDUSUARIO");

        Long turnoActualId = null;
        if (partidaReal.getJugadorTurnoAleatorio() != null) {
            turnoActualId = (long) partidaReal.getJugadorTurnoAleatorio().getId();
        }

        model.put("partida", partidaReal);
        model.put("usuarioSesionId", usuarioSesionId);
        model.put("turnoActualId", turnoActualId);

        return new ModelAndView(VISTA_PARTIDA, model);
    }*/

    @GetMapping("/partida")
    public ModelAndView verTablero(
            @RequestParam(value = "codigo", required = false) String codigoUnico,
            HttpServletRequest request
    ) {
        String codigoFinal = resolverCodigoPartida(codigoUnico, request);
        if (codigoFinal == null) {
            return new ModelAndView("redirect:/home");
        }


        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);
        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + codigoFinal);
        }

        Long usuarioSesionId = resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute("NOMBRE");

        if (usuarioSesionId == null && nombreSesion == null) {
            return new ModelAndView("redirect:/login");
        }

        Long jugadorSesionId = resolverJugadorSesionId(partidaReal, usuarioSesionId, nombreSesion);

        Map<String, Object> model = new HashMap<>();
        model.put("partida", partidaReal);
        model.put("usuarioSesionId", jugadorSesionId);
        model.put("turnoActualId", resolverTurnoActualId(partidaReal, codigoFinal));

        return new ModelAndView(VISTA_PARTIDA, model);
    }

    private String resolverCodigoPartida(String codigoUnico, HttpServletRequest request) {
        if (codigoUnico != null && !codigoUnico.isBlank()) {
            return codigoUnico;
        }
        String codigoSala = (String) request.getSession().getAttribute("codigoSala");
        return (codigoSala != null && !codigoSala.isBlank()) ? codigoSala : null;
    }

    private Long resolverUsuarioSesionId(HttpServletRequest request) {
        Long id = (Long) request.getSession().getAttribute("id");
        if (id == null) {
            id = (Long) request.getSession().getAttribute("IDUSUARIO");
        }
        return id;
    }

    private Long resolverJugadorSesionId(Partida partida, Long usuarioSesionId, String nombreSesion) {
        if (partida != null && partida.getJugadores() != null) {
            for (Jugador j : partida.getJugadores()) {
                if (nombreSesion != null && nombreSesion.equalsIgnoreCase(j.getApodo())) {
                    return (long) j.getId();
                }
                if (usuarioSesionId != null && usuarioSesionId.equals(j.getId().longValue())) {
                    return (long) j.getId();
                }
            }
        }
        return usuarioSesionId;
    }

    private Long resolverTurnoActualId(Partida partida, String codigo) {
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigo);
        if (jugador != null) {
            return (long) jugador.getId();
        }
        if (partida.getJugadorTurnoAleatorio() != null) {
            return (long) partida.getJugadorTurnoAleatorio().getId();
        }
        return null;
    }
    
    @PostMapping("/partida/{codigoPartida}/avanzar-turno")
    @ResponseBody
    public ResponseEntity<TurnoDTO> avanzarTurno(
            @PathVariable("codigoPartida") String codigoPartida,
            HttpServletRequest request
    ) {
        Long usuarioSesionId = resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute("NOMBRE");

        if (usuarioSesionId == null && nombreSesion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jugador jugadorActual = this.servicioPartida.obtenerJugadorActual(codigoPartida);
        if (jugadorActual == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean coincideId = usuarioSesionId != null && usuarioSesionId.equals(jugadorActual.getId().longValue());
        boolean coincideNombre = nombreSesion != null && nombreSesion.equalsIgnoreCase(jugadorActual.getApodo());

        if (!coincideId && !coincideNombre) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        this.servicioPartida.avanzarTurno(codigoPartida);

        Partida partidaActualizada = this.servicioPartida.obtenerPartida(codigoPartida);

        Jugador siguienteJugador = this.servicioPartida.obtenerJugadorActual(codigoPartida);

        TurnoDTO turnoDTO = new TurnoDTO(
                codigoPartida,
                siguienteJugador.getId(),
                siguienteJugador.getApodo(),
                siguienteJugador.getOrden(),
                "Es el turno de " + siguienteJugador.getApodo()
        );
        turnoDTO.setRondaActual(partidaActualizada.getRondaActual());

        this.notificadorWebSocket.convertAndSend("/topic/partida/" + codigoPartida + "/turno", turnoDTO);

        return ResponseEntity.ok(turnoDTO);
    }
}