package com.tallerwebi.dominio.obvservadoresTest;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class ObservadorTurnoAbandonoTest {

    private ServicioPartida servicioPartidaMock;
    private ObservadorTurnoAbandono observador;

    @BeforeEach
    public void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        observador = new ObservadorTurnoAbandono(servicioPartidaMock, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }


@Test
public void dadoQueHay3JugadoresYElDelMedioAbandonaEnSuTurnoCuandoElObservadorRevisaEntoncesAvanzaElTurno() {
    // Preparación
    Partida partida = new Partida();
    partida.setCodigoUnico("SALA-123");
    partida.setIndiceTurnoActual(1);
    partida.setJugadores(new ArrayList<>());

    Jugador jugador1 = new Jugador();
    jugador1.setId(1L);
    jugador1.setOrden(0);
    jugador1.setAbandono(false);

    Jugador jugador2QueAbandona = new Jugador();
    jugador2QueAbandona.setId(2L);
    jugador2QueAbandona.setOrden(1);
    jugador2QueAbandona.setAbandono(true);

    Jugador jugador3 = new Jugador();
    jugador3.setId(3L);
    jugador3.setOrden(2);
    jugador3.setAbandono(false);

    partida.getJugadores().add(jugador1);
    partida.getJugadores().add(jugador2QueAbandona);
    partida.getJugadores().add(jugador3);

    EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugador2QueAbandona);

    // Ejecución
    observador.actualizar(evento);

    // Verificación
    verify(servicioPartidaMock, times(1)).avanzarTurno("SALA-123");
}

}