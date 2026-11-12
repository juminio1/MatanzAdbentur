package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTiempoAgotadoEnCarcel;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.springframework.stereotype.Component;

/** Avisa por WebSocket cuando a un jugador preso se le acaba el tiempo del turno. */
@Component
public class ObservadorCarcelWebSocket implements Observador<EventoTiempoAgotadoEnCarcel> {

    private final NotificadorPartida notificadorPartida;

    public ObservadorCarcelWebSocket(NotificadorPartida notificadorPartida) {
        this.notificadorPartida = notificadorPartida;
    }

    @Override
    public void actualizar(EventoTiempoAgotadoEnCarcel evento) {
        notificadorPartida.notificarTiempoAgotadoEnCarcel(evento);
    }
}
