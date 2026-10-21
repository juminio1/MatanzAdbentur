package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.servicios.ServicioPartida;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTurnoAbandono implements Observador<EventoJugadorAbandonado> {

    private final ServicioPartida servicioPartida;

    public ObservadorTurnoAbandono(@Lazy ServicioPartida servicioPartida) {
        this.servicioPartida = servicioPartida;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        Partida partida = evento.partida();
        Jugador jugadorQueAbandona = evento.jugador();

        // Validamos si el jugador que acaba de irse era el que estaba jugando actualmente
        if (partida.getIndiceTurnoActual() != null && 
            partida.getIndiceTurnoActual().equals(jugadorQueAbandona.getOrden())) {
            
            // Reutilizamos tu lógica existente para saltar el turno y reiniciar timers
            servicioPartida.avanzarTurno(partida.getCodigoUnico());
        }
    }
}
