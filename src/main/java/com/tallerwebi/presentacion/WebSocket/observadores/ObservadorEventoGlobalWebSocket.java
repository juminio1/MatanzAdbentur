package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoGlobalActualizado;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ObservadorEventoGlobalWebSocket {

    private static final String TOPIC_PARTIDA = "/topic/partida/";

    private final SimpMessagingTemplate template;

    public ObservadorEventoGlobalWebSocket(SimpMessagingTemplate template) {
        this.template = template;
    }

    @EventListener
    public void notificarEventoGlobal(EventoGlobalActualizado evento) {
        String destino = TOPIC_PARTIDA + evento.estado().codigoPartida() + "/evento-global";
        template.convertAndSend(destino, evento.estado());
    }
}
