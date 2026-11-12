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
}
