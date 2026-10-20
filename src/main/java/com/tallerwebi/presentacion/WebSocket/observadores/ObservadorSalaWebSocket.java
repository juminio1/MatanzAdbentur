package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;
import org.springframework.stereotype.Component;

/** Adapta un evento de sala al mensaje WebSocket que ya esperan los clientes. */
@Component
public class ObservadorSalaWebSocket implements Observador<EventoSalaActualizada> {

    private final NotificadorSala notificadorSala;

    public ObservadorSalaWebSocket(NotificadorSala notificadorSala) {
        this.notificadorSala = notificadorSala;
    }

    @Override
    public void actualizar(EventoSalaActualizada evento) {
        SalaActualizadaDTO estado = new SalaActualizadaDTO(
            evento.codigo(), evento.usernames(), evento.fichasOcupadas()
        );
        estado.setFichasAsignadas(evento.fichasAsignadas());
        estado.setCreador(evento.creador());
        notificadorSala.notificarSalaActualizada(estado);
    }
}
