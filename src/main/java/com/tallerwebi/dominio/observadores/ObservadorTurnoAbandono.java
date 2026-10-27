package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTurnoAbandono implements Observador<EventoJugadorAbandonado> {
    private final ServicioParticipacion participacion;


    private final ServicioPartida servicioPartida;

    public ObservadorTurnoAbandono(@Lazy ServicioPartida servicioPartida, ServicioParticipacion participacion) {
        this.participacion = participacion;
        //Lazytown
        this.servicioPartida = servicioPartida;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = evento.partida();
        Jugador jugadorQueAbandona = evento.jugador();

        long jugadoresRestantes = partida.getJugadores().stream().filter(jugador -> !jugador.getId().equals(jugadorQueAbandona.getId())).filter(participacion::estaActivo).count();

        if (jugadoresRestantes > 1 && partida.getIndiceTurnoActual() != null && partida.getIndiceTurnoActual().equals(jugadorQueAbandona.getOrden())) {
            servicioPartida.avanzarTurno(partida.getCodigoUnico());
        }
    }
}
