package com.tallerwebi.dominio.obvservadores;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ObservadorVictoriaTest {
    private ServicioFinalizacionPartida finalizacion;
    private ObservadorVictoria observador;

    @BeforeEach
    public void init() {
        finalizacion = mock(ServicioFinalizacionPartida.class);
        observador = new ObservadorVictoria(finalizacion);
    }

    @Test
    public void abandonoDelegaUnaSolaVezSinEvaluarJugadoresNiCambiarElEstado() {
        Partida partida = spy(new Partida());
        partida.setEstado(EstadoPartida.EN_CURSO);
        clearInvocations(partida);
        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, new Jugador());

        observador.actualizar(evento);

        verify(finalizacion).finalizarSiCorresponde(partida);
        verifyNoMoreInteractions(finalizacion);
        verify(partida, never()).getJugadores();
        verify(partida, never()).setEstado(any());
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
    }

    @Test
    public void unResultadoPositivoDelServicioNoProvocaOtroCambioDirectoDeEstado() {
        Partida partida = spy(new Partida());
        when(finalizacion.finalizarSiCorresponde(partida)).thenReturn(true);

        observador.actualizar(new EventoJugadorAbandonado(partida, new Jugador()));

        verify(finalizacion).finalizarSiCorresponde(partida);
        verifyNoMoreInteractions(finalizacion);
        verify(partida, never()).setEstado(any());
        verify(partida, never()).getJugadores();
    }

    @Test
    public void propagaLaMismaExcepcionDelServicioSinReintentar() {
        Partida partida = new Partida();
        IllegalStateException fallo = new IllegalStateException("Fallo de finalizacion");
        when(finalizacion.finalizarSiCorresponde(partida)).thenThrow(fallo);

        assertSame(fallo, assertThrows(IllegalStateException.class,
                () -> observador.actualizar(new EventoJugadorAbandonado(partida, new Jugador()))));

        verify(finalizacion).finalizarSiCorresponde(partida);
        verifyNoMoreInteractions(finalizacion);
    }

    @Test
    public void dadoQueUnJugadorAbandonaYQuedaUnSoloJugadorActivoCuandoElObservadorRevisaEntoncesLaPartidaFinaliza() {
        // Preparación
        Partida partida = new Partida();
        partida.setJugadores(new java.util.ArrayList<>());
        partida.setEstado(EstadoPartida.EN_CURSO);

        Jugador jugadorQueAbandona = new Jugador();
        jugadorQueAbandona.setAbandono(true);

        Jugador jugadorGanador = new Jugador();
        jugadorGanador.setAbandono(false);
        jugadorGanador.setEnBancarrota(false);

        partida.getJugadores().add(jugadorQueAbandona);
        partida.getJugadores().add(jugadorGanador);

        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugadorQueAbandona);

        com.tallerwebi.infraestructura.RepositorioPartida repositorioPartidaMock = dadoElServicioFinalDeMain(partida);

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
        partida.setJugadores(new java.util.ArrayList<>());
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

        com.tallerwebi.infraestructura.RepositorioPartida repositorioPartidaMock = dadoElServicioFinalDeMain(partida);

        // 2. Ejecución (Act)
        observador.actualizar(evento);

        // 3. Validación (Assert)
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado(), "La partida debería estar en curso porque quedan varios jugadores");
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    private com.tallerwebi.infraestructura.RepositorioPartida dadoElServicioFinalDeMain(Partida partida) {
        partida.setId(1L);
        partida.setCodigoUnico("VICTORIA");
        var repositorio = mock(com.tallerwebi.infraestructura.RepositorioPartida.class);
        var resultados = mock(com.tallerwebi.infraestructura.RepositorioFinalizarPartida.class);
        long id = 1;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setId(id++);
            jugador.setDinero(1500);
            if (jugador.getAbandono()) {
                var guardado = new com.tallerwebi.dominio.entidades.FinalizarPartida();
                guardado.setJugador(jugador);
                guardado.setPartida(partida);
                guardado.setEstadoParticipante(com.tallerwebi.dominio.enums.EstadoParticipante.ABANDONADO);
                when(resultados.buscarPorPartidaYJugador(1L, jugador.getId())).thenReturn(guardado);
            }
        }
        var servicio = new com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl(
                mock(com.tallerwebi.infraestructura.RepositorioPropiedad.class),
                mock(com.tallerwebi.infraestructura.RepositorioDeudaPendiente.class), resultados,
                new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl(), repositorio,
                mock(com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida.class));
        observador = new ObservadorVictoria(servicio);
        return repositorio;
    }
}
