
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import org.springframework.stereotype.Component;

@Component
//Escucha EventoJugadorQuebrado.
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
