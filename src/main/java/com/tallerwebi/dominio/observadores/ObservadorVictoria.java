package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.springframework.stereotype.Component;

@Component
public class ObservadorVictoria implements Observador<EventoJugadorAbandonado> {

    private final RepositorioPartida repositorioPartida;

    public ObservadorVictoria(RepositorioPartida repositorioPartida) {
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        Partida partida = evento.partida();

        long jugadoresActivos = partida.getJugadores().stream()
                .filter(jugador -> !jugador.getAbandono() && !jugador.getEnBancarrota())
                .count();

        if (jugadoresActivos == 1) {
            partida.setEstado(EstadoPartida.FINALIZADA);
            repositorioPartida.guardarPartida(partida);
        }
    }
}
