package com.tallerwebi.presentacion.WebSocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;

@Component 
public class NotificadorSala { //Esta clase lo que hace es mandar información a los usuarios conectados a una sala

    private final SimpMessagingTemplate messagingTemplate; //Es una herramineta que tiene Spring para enviar mensajes

    public NotificadorSala(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

      public void notificarSalaActualizada(SalaActualizadaDTO sala) {
        messagingTemplate.convertAndSend("/topic/sala/" + sala.getCodigo(), sala);

}
}