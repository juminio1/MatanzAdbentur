package com.tallerwebi.dominio.obvservadores;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.dominio.observadores.ObservadorVictoriaQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservadorVictoriaQuiebraTest {
    private ObservadorVictoria victoria;
    private ServicioFinalizacionPartida finalizacion;
    private Consumer<EventoJugadorQuebrado> notificacion;
    private ObservadorVictoriaQuiebra observador;
    private EventoJugadorQuebrado evento;

    @BeforeEach
    void preparar() {
        finalizacion = mock(ServicioFinalizacionPartida.class);
        victoria = spy(new ObservadorVictoria(finalizacion));
        notificacion = mock(Consumer.class);
        observador = new ObservadorVictoriaQuiebra(victoria, notificacion);
        evento = new EventoJugadorQuebrado(new Partida(), new Jugador());
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void verificaVictoriaYNotificaUnaVezDespuesDelCommit() {
        observador.actualizar(evento);

        verify(victoria).verificarVictoria(evento.partida());
        verify(finalizacion).finalizarSiCorresponde(evento.partida());
        verifyNoMoreInteractions(finalizacion);
        verifyNoInteractions(notificacion);
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(notificacion).accept(evento);
        verifyNoMoreInteractions(notificacion);
    }

    @Test
    void rollbackNoNotifica() {
        observador.actualizar(evento);
        TransactionSynchronizationManager.getSynchronizations().forEach(sincronizacion ->
                sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(notificacion);
    }

    @Test
    void sinTransaccionNoEvaluaNiEnviaUnAvisoAnticipado() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class, () -> observador.actualizar(evento));
        verifyNoInteractions(victoria, notificacion);
    }

    @Test
    void sinSincronizacionNoEvaluaNiEnviaUnAvisoAnticipado() {
        TransactionSynchronizationManager.clearSynchronization();
        assertThrows(IllegalStateException.class, () -> observador.actualizar(evento));
        verifyNoInteractions(victoria, notificacion);
    }

    @Test
    void falloDelServicioSePropagaSinRegistrarNotificacionPosterior() {
        IllegalStateException fallo = new IllegalStateException("Fallo de finalizacion");
        when(finalizacion.finalizarSiCorresponde(evento.partida())).thenThrow(fallo);

        assertSame(fallo, assertThrows(IllegalStateException.class, () -> observador.actualizar(evento)));

        verify(finalizacion).finalizarSiCorresponde(evento.partida());
        verifyNoMoreInteractions(finalizacion);
        verifyNoInteractions(notificacion);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }
}
