package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.servicios.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPartida {

    private final ServicioDado servicioDado;
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;

    @Autowired
    public ControladorPartida(ServicioDado servicioDado, ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket) {
        this.servicioDado = servicioDado;
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;

    }

    @GetMapping("/partida")
    public ModelAndView vistaTableroPartida() {
        return new ModelAndView("partida");
    }

    @PostMapping("/partida/tirar-dados")
    public ResultadoTirada tirarDados() {
        ResultadoTirada resultado = servicioDado.tirarDados();
        return resultado;
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
