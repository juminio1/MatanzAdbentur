
package com.tallerwebi.presentacion.WebSocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;

@Component
public class NotificadorSala { //Esta clase lo que hace es enviar información a los jugadores que esten en una sala

    private final SimpMessagingTemplate messagingTemplate;

    public NotificadorSala(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notificarSalaActualizada(SalaActualizadaDTO sala) {

        messagingTemplate.convertAndSend("/topic/sala/" + sala.getCodigo(), sala
        );
    }
}
