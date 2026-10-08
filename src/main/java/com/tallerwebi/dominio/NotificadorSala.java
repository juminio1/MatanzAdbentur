package com.tallerwebi.dominio;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component 
public class NotificadorSala { //Esta clase slo que hace es mandar información a los usuarios conectados a una sala

    private final SimpMessagingTemplate messagingTemplate; //Es una herramineta que tiene Spring para enviar mensajes

    public NotificadorSala(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    

    public void notificarSalaActualizada(String codigoSala){

        

    }


}
