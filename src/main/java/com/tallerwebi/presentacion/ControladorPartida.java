package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ServicioDado servicioDado;
    private final SimpMessagingTemplate notificadorWebSocket;

    @Autowired
    public ControladorPartida(
            ServicioPartida servicioPartida,
            ServicioDado servicioDado,
            SimpMessagingTemplate notificadorWebSocket
    ) {
        this.servicioPartida = servicioPartida;
        this.servicioDado = servicioDado;
        this.notificadorWebSocket = notificadorWebSocket;
    }

    @GetMapping("/partida")
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
            return new ModelAndView("redirect:/home");
        }

        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);

        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + codigoFinal);
        }

        Long usuarioSesionId = (Long) request.getSession().getAttribute("id");

        if (usuarioSesionId == null) {
            usuarioSesionId = (Long) request.getSession().getAttribute("IDUSUARIO");
        }

        if (usuarioSesionId == null) {
            return new ModelAndView("redirect:/login");
        }

        Long turnoActualId = null;
        if (partidaReal.getJugadorTurnoAleatorio() != null) {
            turnoActualId = (long) partidaReal.getJugadorTurnoAleatorio().getId();
        }

        model.put("partida", partidaReal);
        model.put("usuarioSesionId", usuarioSesionId);
        model.put("turnoActualId", turnoActualId);

        return new ModelAndView(VISTA_PARTIDA, model);
    }

    @PostMapping("/partida/{codigoPartida}/avanzar-turno")
    @ResponseBody
    public TurnoDTO avanzarTurno(@PathVariable String codigoPartida) {
        this.servicioPartida.avanzarTurno(codigoPartida);
        Jugador jugadorActual = this.servicioPartida.obtenerJugadorActual(codigoPartida);

        TurnoDTO turnoDTO = new TurnoDTO(
                codigoPartida,
                jugadorActual.getId(),
                jugadorActual.getApodo(),
                jugadorActual.getOrden(),
                "Es el turno de " + jugadorActual.getApodo()
        );

        this.notificadorWebSocket.convertAndSend("/topic/partida/" + codigoPartida + "/turno", turnoDTO);

        return turnoDTO;
    }
}