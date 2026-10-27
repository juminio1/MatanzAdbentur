
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.springframework.stereotype.Component;

@Component
public class ObservadorVictoria implements Observador<EventoJugadorAbandonado> {
    private final ServicioParticipacion participacion;


    private static final Long CANTIDAD_JUGADORES_VICTORIA = 1L;

    private final RepositorioPartida repositorioPartida;

    public ObservadorVictoria(RepositorioPartida repositorioPartida, ServicioParticipacion participacion) {
        this.participacion = participacion;
        this.repositorioPartida = repositorioPartida;
    }

    //Se ejecuta cuando el jugador abandona. Y llama a verificarVictoria
    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        verificarVictoria(evento.partida());
    }

    //cuenta cuantos jugadores siguen activos, excluyendo a los que abadonaron o entraron en quiebra.
    //si queda uno solo finaliza la partida.
    public void verificarVictoria(Partida partida) {

        Long jugadoresActivos = partida.getJugadores().stream()
                .filter(participacion::estaActivo)
                .count();

        if (jugadoresActivos.equals(CANTIDAD_JUGADORES_VICTORIA)) {
            partida.setEstado(EstadoPartida.FINALIZADA);
            repositorioPartida.guardarPartida(partida);
        }
    }
}
