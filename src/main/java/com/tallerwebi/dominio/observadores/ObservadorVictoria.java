
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import org.springframework.stereotype.Component;

@Component
public class ObservadorVictoria implements Observador<EventoJugadorAbandonado> {
    private final ServicioFinalizacionPartida finalizacion;

    public ObservadorVictoria(ServicioFinalizacionPartida finalizacion) {
        this.finalizacion = finalizacion;
    }

    //Se ejecuta cuando el jugador abandona. Y llama a verificarVictoria
    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        verificarVictoria(evento.partida());
    }

    public void verificarVictoria(Partida partida) {
        finalizacion.finalizarSiCorresponde(partida);
    }
}
