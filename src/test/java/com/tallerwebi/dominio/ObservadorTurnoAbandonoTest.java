package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono;
import com.tallerwebi.dominio.servicios.ServicioPartida;
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
        observador = new ObservadorTurnoAbandono(servicioPartidaMock);
    }

    @Test
    public void dadoQueHay3JugadoresYElDelMedioAbandonaEnSuTurnoCuandoElObservadorRevisaEntoncesAvanzaElTurno() {
        // 1. Preparación (Arrange)
        Partida partida = new Partida();
        partida.setCodigoUnico("SALA-123");
        partida.setIndiceTurnoActual(1); // Es el turno del jugador del medio
        partida.setJugadores(new ArrayList<>());

        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        jugador1.setAbandono(false);

        Jugador jugador2QueAbandona = new Jugador();
        jugador2QueAbandona.setOrden(1); // Mismo índice que la partida
        jugador2QueAbandona.setAbandono(true);

        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);
        jugador3.setAbandono(false);

        partida.getJugadores().add(jugador1);
        partida.getJugadores().add(jugador2QueAbandona);
        partida.getJugadores().add(jugador3);

        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugador2QueAbandona);

        // 2. Ejecución (Act)
        observador.actualizar(evento);

        // 3. Validación (Assert)
        // El observador cumple su trabajo si manda a avanzar el turno.
        // La lógica de a quién le toca realmente vive en ServicioTurno.
        verify(servicioPartidaMock, times(1)).avanzarTurno("SALA-123");
    }
}