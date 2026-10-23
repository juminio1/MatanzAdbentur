
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

//Detecta si debe avanzar el turno cuando quiebra un jugador.

@Component
//permite al spring registre el observador
public class ObservadorTurnoQuiebra implements Observador<EventoJugadorQuebrado> {

    private final ServicioPartida servicioPartida;
    private static final Long MINIMO_JUGADORES_PARA_CONTINUAR = 2L;

    public ObservadorTurnoQuiebra(@Lazy ServicioPartida servicioPartida) {
        this.servicioPartida = servicioPartida;
    }

    @Override
    public void actualizar(EventoJugadorQuebrado evento) {

        Partida partida = evento.partida();

        Long jugadoresActivos = partida.getJugadores().stream()
                .filter(jugador -> !Boolean.TRUE.equals(jugador.getAbandono()))
                .filter(jugador -> !Boolean.TRUE.equals(jugador.getEnBancarrota()))
                .count();

        if (jugadoresActivos < MINIMO_JUGADORES_PARA_CONTINUAR) {
            return;
        }

        if (partida.getIndiceTurnoActual() != null
                && partida.getIndiceTurnoActual().equals(evento.jugador().getOrden())) {
            servicioPartida.avanzarTurno(partida.getCodigoUnico());
        }
    }
}
