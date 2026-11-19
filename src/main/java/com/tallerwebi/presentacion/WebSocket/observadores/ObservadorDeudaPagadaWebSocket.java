package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoDeudaPagada;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.springframework.stereotype.Component;

/** Avisa por WebSocket cuando se paga una deuda entre jugadores, para animar el pago. */
@Component
public class ObservadorDeudaPagadaWebSocket implements Observador<EventoDeudaPagada> {

    private final NotificadorPartida notificadorPartida;

    public ObservadorDeudaPagadaWebSocket(NotificadorPartida notificadorPartida) {
        this.notificadorPartida = notificadorPartida;
    }

    @Override
    public void actualizar(EventoDeudaPagada evento) {
        notificadorPartida.notificarDeudaPagada(evento);
    }
}
