package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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
}