package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTurnoAbandono implements Observador<EventoJugadorAbandonado> {

    private final ServicioPartida servicioPartida;

    public ObservadorTurnoAbandono(@Lazy ServicioPartida servicioPartida) {
        //Lazytown
        this.servicioPartida = servicioPartida;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) throws ActivacionEventoInvalidoException {
        Partida partida = evento.partida();
        Jugador jugadorQueAbandona = evento.jugador();

        long jugadoresRestantes = partida.getJugadores().stream().filter(jugador -> !jugador.getId().equals(jugadorQueAbandona.getId())).filter(jugador -> !Boolean.TRUE.equals(jugador.getAbandono())).count();

        if (jugadoresRestantes > 1 && partida.getIndiceTurnoActual() != null && partida.getIndiceTurnoActual().equals(jugadorQueAbandona.getOrden())) {
            servicioPartida.avanzarTurno(partida.getCodigoUnico());
        }
    }
}
