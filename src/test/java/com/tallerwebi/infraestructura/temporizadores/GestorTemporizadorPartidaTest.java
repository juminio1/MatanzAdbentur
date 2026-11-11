package com.tallerwebi.infraestructura.temporizadores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class GestorTemporizadorPartidaTest {
    private PlatformTransactionManager transacciones;
    private TransactionStatus estadoTransaccion;
    private ScheduledExecutorService planificador;
    private ScheduledFuture<?> tarea;
    private GestorTemporizadorPartida gestor;

    @BeforeEach
    void preparar() {
        transacciones = mock(PlatformTransactionManager.class);
        estadoTransaccion = mock(TransactionStatus.class);
        when(transacciones.getTransaction(any())).thenReturn(estadoTransaccion);
        planificador = mock(ScheduledExecutorService.class);
        tarea = mock(ScheduledFuture.class);
        doReturn(tarea).when(planificador).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
        gestor = new GestorTemporizadorPartida(transacciones);
        ReflectionTestUtils.setField(gestor, "scheduler", planificador);
    }

    @AfterEach
    void cerrarTemporizadores() {
        gestor.destroy();
    }

    @Test
    void dadoQueYaHayUnaEtapaNuevaCuandoLlegaUnaProgramacionAnteriorDaComoResultadoElPlazoVigente() {
        // Preparación
        when(tarea.getDelay(TimeUnit.MILLISECONDS)).thenReturn(30000L);
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 30, () -> {});

        // Ejecución
        gestor.programarTemporizadorEvento("evento:ABC", 3L, 0, () -> {});
        Integer segundos = gestor.obtenerSegundosRestantes("evento:ABC");
        gestor.cancelarTemporizadorEvento("evento:ABC", 5L);
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 30, () -> {});

        // Verificación
        assertEquals(30, segundos);
        assertNull(gestor.obtenerSegundosRestantes("evento:ABC"));
        verify(planificador, times(1)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        verify(tarea).cancel(false);
    }

    @Test
    void dadoQueVenceUnaTareaCuandoSeEjecutaDaComoResultadoLaAccionEnUnaTransaccionConfirmada() {
        // Preparación
        Runnable accion = mock(Runnable.class);
        gestor.programarTemporizador("ABC", 30, accion);
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(planificador).schedule(vencimiento.capture(), eq(30L), eq(TimeUnit.SECONDS));

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        org.mockito.InOrder orden = inOrder(transacciones, accion);
        orden.verify(transacciones).getTransaction(any());
        orden.verify(accion).run();
        orden.verify(transacciones).commit(estadoTransaccion);
        verify(transacciones, never()).rollback(any());
    }

    @Test
    void dadoQueHayUnaTareaVigenteCuandoSeCancelaDaComoResultadoSuCancelacionSinEjecutarLaAccion() {
        // Preparación
        Runnable accion = mock(Runnable.class);
        gestor.programarTemporizador("ABC", 30, accion);

        // Ejecución
        gestor.cancelarTemporizador("ABC");

        // Verificación
        verify(tarea).cancel(false);
        assertNull(gestor.obtenerSegundosRestantes("ABC"));
        verifyNoInteractions(accion, transacciones);
    }

    @Test
    void dadoQueLaAccionFallaCuandoVenceLaTareaDaComoResultadoRollbackYUnaExcepcionConLaCausaOriginal() {
        // Preparación
        IllegalArgumentException causa = new IllegalArgumentException("Error simulado");
        Runnable accion = mock(Runnable.class);
        doThrow(causa).when(accion).run();
        gestor.programarTemporizador("ABC", 30, accion);
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(planificador).schedule(vencimiento.capture(), eq(30L), eq(TimeUnit.SECONDS));

        // Ejecución
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> vencimiento.getValue().run());

        // Verificación
        assertSame(causa, error.getCause());
        verify(transacciones).rollback(estadoTransaccion);
        verify(transacciones, never()).commit(any());
    }

    @Test
    void dadoQueLaRevisionSigueVigenteCuandoSeRepiteLaProgramacionDaComoResultadoUnaSolaTarea() {
        // Preparación
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 30, () -> {});

        // Ejecución
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 1, () -> {});
        gestor.cancelarTemporizadorEvento("evento:ABC", 3L);

        // Verificación
        verify(planificador, times(1)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        verify(tarea, never()).cancel(anyBoolean());
    }

    @Test
    void dadoQueLaTareaDelEventoYaTerminoCuandoSeReprogramaLaMismaRevisionDaComoResultadoUnNuevoTemporizador() {
        // Preparación
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 30, () -> {});
        when(tarea.isDone()).thenReturn(true);

        // Ejecución
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 10, () -> {});

        // Verificación
        verify(planificador).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        verify(planificador).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));
        verify(tarea, never()).cancel(anyBoolean());
    }

    @Test
    void dadoQueHayDosPartidasCuandoSeCancelaUnaDaComoResultadoElTemporizadorDeLaOtraConservado() {
        // Preparación
        ScheduledFuture<?> otraTarea = mock(ScheduledFuture.class);
        doReturn(tarea, otraTarea).when(planificador).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
        when(otraTarea.getDelay(TimeUnit.MILLISECONDS)).thenReturn(1501L);
        gestor.programarTemporizadorEvento("evento:ABC", 4L, 30, () -> {});
        gestor.programarTemporizadorEvento("evento:DEF", 1L, 30, () -> {});

        // Ejecución
        gestor.cancelarTemporizadorEvento("evento:ABC", 5L);

        // Verificación
        assertNull(gestor.obtenerSegundosRestantes("evento:ABC"));
        assertEquals(2, gestor.obtenerSegundosRestantes("evento:DEF"));
        verify(tarea).cancel(false);
        verify(otraTarea, never()).cancel(anyBoolean());
    }

    @Test
    void dadoQueUnaTareaYaTerminoCuandoSeSolicitaOtraDaComoResultadoUnNuevoTemporizador() {
        // Preparación
        assertTrue(gestor.programarTemporizadorSiNoExiste("ABC", 30, () -> {}));
        assertFalse(gestor.programarTemporizadorSiNoExiste("ABC", 30, () -> {}));
        when(tarea.isDone()).thenReturn(true);

        // Ejecución
        Integer segundos = gestor.obtenerSegundosRestantes("ABC");
        Boolean programado = gestor.programarTemporizadorSiNoExiste("ABC", 30, () -> {});

        // Verificación
        assertEquals(0, segundos);
        assertTrue(programado);
        verify(planificador, times(2)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        verify(tarea, never()).cancel(anyBoolean());
    }
}
