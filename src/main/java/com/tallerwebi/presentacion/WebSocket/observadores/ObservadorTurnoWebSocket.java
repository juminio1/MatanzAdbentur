package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.springframework.stereotype.Component;

/** Envío los cambios de turno por WebSocket. */
@Component
public class ObservadorTurnoWebSocket implements Observador<EventoTurnoCambiado> {

    private final NotificadorPartida notificadorPartida;

    public ObservadorTurnoWebSocket(NotificadorPartida notificadorPartida) {
        this.notificadorPartida = notificadorPartida;
    }

    @Override
    public void actualizar(EventoTurnoCambiado evento) {
        TurnoDTO turno = new TurnoDTO(
            evento.codigoPartida(), evento.idJugadorActual(), evento.apodoJugadorActual(),
            evento.orden(), "Es el turno de " + evento.apodoJugadorActual(), evento.duracionSegundos()
        );
        turno.setRondaActual(evento.rondaActual());
        notificadorPartida.notificarCambioDeTurno(evento.codigoPartida(), turno);
    }
}
