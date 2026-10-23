
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import org.springframework.stereotype.Component;

@Component
//cuando recibe EventoJugadorQuebrado, le pide a ObservadorVictoria que verifique si la partida debe finalizar..
public class ObservadorVictoriaQuiebra
        implements Observador<EventoJugadorQuebrado> {

    private final ObservadorVictoria observadorVictoria;

    public ObservadorVictoriaQuiebra(
            ObservadorVictoria observadorVictoria) {
        this.observadorVictoria = observadorVictoria;
    }

    @Override
    public void actualizar(EventoJugadorQuebrado evento) {
        observadorVictoria.verificarVictoria(evento.partida());
    }
}
