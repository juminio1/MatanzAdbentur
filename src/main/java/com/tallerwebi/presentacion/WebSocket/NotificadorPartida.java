package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificadorPartida {

    private final SimpMessagingTemplate template;

    @Autowired
    public NotificadorPartida(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notificarCambioDeTurno(String codigoPartida, TurnoDTO turno) {
        this.template.convertAndSend("/topic/partida/" + codigoPartida + "/turno", turno);
    }

    public void notificarMovimiento(String codigoPartida, RespuestaMovimientoDTO movimiento){
        this.template.convertAndSend("/topic/partida/" + codigoPartida, movimiento);

    }

    public static void notificarSaldos(SimpMessagingTemplate template, ServicioPartida servicioPartida,
                                       String codigoPartida) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || partida.getJugadores() == null) return;
        for (Jugador jugador : partida.getJugadores()) {
            Map<String, Object> saldo = new HashMap<>();
            saldo.put("idJugador", jugador.getId());
            saldo.put("dinero", jugador.getDinero());
            template.convertAndSend("/topic/partida/" + codigoPartida + "/dinero", saldo);
        }
    }
}
