package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificadorSala {

    //Esta clase lo que hace es enviar información a los jugadores que esten en una sala

    private final SimpMessagingTemplate messagingTemplate;

    public NotificadorSala(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notificarSalaActualizada(SalaActualizadaDTO sala) {
        messagingTemplate.convertAndSend("/topic/sala/" + sala.getCodigo(), sala);
    }
}
