package com.tallerwebi.dominio.obvservadoresTest;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ObservadorVictoriaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ObservadorVictoria observador;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        observador = new ObservadorVictoria(repositorioPartidaMock);
    }

    @Test
    public void dadoQueUnJugadorAbandonaYQuedaUnSoloJugadorActivoCuandoElObservadorRevisaEntoncesLaPartidaFinaliza() {
        // Preparación
        Partida partida = new Partida();
        partida.setJugadores(new ArrayList<>());
        partida.setEstado(EstadoPartida.EN_CURSO);

        Jugador jugadorQueAbandona = new Jugador();
        jugadorQueAbandona.setAbandono(true);

        Jugador jugadorGanador = new Jugador();
        jugadorGanador.setAbandono(false);
        jugadorGanador.setEnBancarrota(false);

        partida.getJugadores().add(jugadorQueAbandona);
        partida.getJugadores().add(jugadorGanador);

        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugadorQueAbandona);

        // 2. Ejecución (Act)
        observador.actualizar(evento);

        // 3. Validación (Assert)
        assertEquals(EstadoPartida.FINALIZADA, partida.getEstado(), "La partida debería estar finalizada porque quedó un solo jugador");
        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void dadoQueHayVariosJugadoresYUnoAbandonaCuandoElObservadorRevisaEntoncesLaPartidaNoFinaliza() {
        // Preparación
        Partida partida = new Partida();
        partida.setJugadores(new ArrayList<>());
        partida.setEstado(EstadoPartida.EN_CURSO);

        Jugador jugadorQueAbandona = new Jugador();
        jugadorQueAbandona.setAbandono(true);

        Jugador jugadorPrimero = new Jugador();
        jugadorPrimero.setAbandono(false);
        jugadorPrimero.setEnBancarrota(false);

        Jugador jugadorSegundo = new Jugador();
        jugadorSegundo.setAbandono(false);
        jugadorSegundo.setEnBancarrota(false);

        partida.getJugadores().add(jugadorQueAbandona);
        partida.getJugadores().add(jugadorPrimero);
        partida.getJugadores().add(jugadorSegundo);

        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugadorQueAbandona);

        // 2. Ejecución (Act)
        observador.actualizar(evento);

        // 3. Validación (Assert)
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado(), "La partida debería estar en curso porque quedan varios jugadores");
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }
}