package com.tallerwebi.infraestructura.temporizadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

public class GestorTemporizadorPartidaTest {

    private static final String CODIGO = "MATANZA123";
    private static final Integer DURACION_EVENTO_SEGUNDOS = 15;
    private static final Integer DURACION_TURNO_SEGUNDOS = 30;

    private PlatformTransactionManager transactionManagerMock;
    private TransactionStatus transactionStatusMock;
    private ScheduledExecutorService schedulerMock;
    private GestorTemporizadorPartida gestorTemporizador;

    @BeforeEach
    public void init() {
        transactionManagerMock = mock(PlatformTransactionManager.class);
        transactionStatusMock = mock(TransactionStatus.class);
        schedulerMock = mock(ScheduledExecutorService.class);

        when(transactionManagerMock.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatusMock);

        gestorTemporizador = new GestorTemporizadorPartida(transactionManagerMock);
        ReflectionTestUtils.setField(gestorTemporizador, "scheduler", schedulerMock);
    }

    @Test
    public void dadoQueNoHayTemporizadorCuandoSeProgramaYDaUnaTareaRegistrada() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.getDelay(TimeUnit.MILLISECONDS)).thenReturn(15000L);

        gestorTemporizador.programarTemporizador(CODIGO, DURACION_EVENTO_SEGUNDOS, () -> {});

        assertEquals(15, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void dadoQueHayTemporizadorActivoCuandoSeProgramaOtroYDaLaCancelacionDelAnterior() {
        ScheduledFuture<?> primeraTarea = mock(ScheduledFuture.class);
        ScheduledFuture<?> segundaTarea = mock(ScheduledFuture.class);

        doReturn(primeraTarea, segundaTarea).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        when(primeraTarea.isDone()).thenReturn(false);
        when(segundaTarea.getDelay(TimeUnit.MILLISECONDS)).thenReturn(30000L);

        gestorTemporizador.programarTemporizador(CODIGO, DURACION_TURNO_SEGUNDOS, () -> {});
        gestorTemporizador.programarTemporizador(CODIGO, DURACION_TURNO_SEGUNDOS, () -> {});

        assertEquals(30, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(primeraTarea).cancel(false);
        verify(schedulerMock, times(2)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void dadoQueLaTareaAnteriorFinalizoCuandoSeProgramaOtraYDaUnaNuevaSinCancelarLaAnterior() {
        ScheduledFuture<?> primeraTarea = mock(ScheduledFuture.class);
        ScheduledFuture<?> segundaTarea = mock(ScheduledFuture.class);

        doReturn(primeraTarea, segundaTarea).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(primeraTarea.isDone()).thenReturn(true);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});
        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        verify(primeraTarea, never()).cancel(false);
        verify(schedulerMock, times(2)).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void dadoQueNoExisteTemporizadorCuandoSeProgramaSiNoExisteYDaVerdadero() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));

        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(CODIGO, DURACION_TURNO_SEGUNDOS, () -> {});

        assertEquals(true, iniciado);
        verify(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void dadoQueExisteTemporizadorActivoCuandoSeProgramaSiNoExisteYDaFalso() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        when(tareaMock.isDone()).thenReturn(false);

        gestorTemporizador.programarTemporizador(CODIGO, 30, () -> {});
        
        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(CODIGO, 30, () -> {});

        assertEquals(false, iniciado);
        verify(schedulerMock, times(1)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        verify(tareaMock, never()).cancel(false);
    }

    @Test
    public void dadoQueExisteTemporizadorFinalizadoCuandoSeProgramaSiNoExisteYDaVerdadero() {
        ScheduledFuture<?> primeraTarea = mock(ScheduledFuture.class);
        ScheduledFuture<?> segundaTarea = mock(ScheduledFuture.class);

        doReturn(primeraTarea, segundaTarea).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(primeraTarea.isDone()).thenReturn(true);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});
        
        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(CODIGO, 15, () -> {});

        assertEquals(true, iniciado);
        verify(primeraTarea, never()).cancel(false);
        verify(schedulerMock, times(2)).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void dadoQueNoExisteTemporizadorCuandoSeCancelaYDaLaAusenciaDeTarea() {
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(schedulerMock, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void dadoQueExisteTemporizadorActivoCuandoSeCancelaYDaLaCancelacionDeLaTarea() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        when(tareaMock.isDone()).thenReturn(false);

        gestorTemporizador.programarTemporizador(CODIGO, 30, () -> {});
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(tareaMock).cancel(false);
    }

    @Test
    public void dadoQueExisteTemporizadorFinalizadoCuandoSeCancelaYDaLaEliminacionSinCancelar() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.isDone()).thenReturn(true);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(tareaMock, never()).cancel(false);
    }

    @Test
    public void dadoQueYaSeCanceloElTemporizadorCuandoSeCancelaOtraVezYDaUnaSolaCancelacion() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.isDone()).thenReturn(false);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});
        gestorTemporizador.cancelarTemporizador(CODIGO);
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(tareaMock, times(1)).cancel(false);
    }

    @Test
    public void dadoQueNoHayTareaCuandoSeConsultanSegundosRestantesYDaNulo() {
        Integer segundos = gestorTemporizador.obtenerSegundosRestantes(CODIGO);

        assertNull(segundos);
    }

    @Test
    public void dadoQueLaTareaFinalizoCuandoSeConsultanSegundosRestantesYDaCero() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.isDone()).thenReturn(true);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        assertEquals(0, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        verify(tareaMock, never()).getDelay(TimeUnit.MILLISECONDS);
    }

    @Test
    public void dadoQueQuedanMilQuinientosMilisegundosCuandoSeConsultanSegundosYDaDos() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.getDelay(TimeUnit.MILLISECONDS)).thenReturn(1500L);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        assertEquals(2, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
    }

    @Test
    public void dadoQueQuedanMilMilisegundosCuandoSeConsultanSegundosYDaUno() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.getDelay(TimeUnit.MILLISECONDS)).thenReturn(1000L);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        assertEquals(1, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
    }

    @Test
    public void dadoQueQuedaUnMilisegundoCuandoSeConsultanSegundosYDaUno() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.getDelay(TimeUnit.MILLISECONDS)).thenReturn(1L);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        assertEquals(1, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
    }

    @Test
    public void dadoQueElTiempoYaVencioCuandoSeConsultanSegundosYDaCero() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doReturn(tareaMock).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaMock.getDelay(TimeUnit.MILLISECONDS)).thenReturn(-100L);

        gestorTemporizador.programarTemporizador(CODIGO, 15, () -> {});

        assertEquals(0, gestorTemporizador.obtenerSegundosRestantes(CODIGO));
    }

    @Test
    public void dadoQueVenceUnTemporizadorCuandoSeEjecutaLaAccionYDaUnaTransaccionConfirmada() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);
        AtomicInteger ejecuciones = new AtomicInteger();

        doAnswer(invocacion -> {
                Runnable accionProgramada = invocacion.getArgument(0);
                accionProgramada.run();
                return tareaMock;
            }).when(schedulerMock).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));

        gestorTemporizador.programarTemporizador(CODIGO, 0, ejecuciones::incrementAndGet);

        assertEquals(1, ejecuciones.get());
        verify(transactionManagerMock).getTransaction(any(TransactionDefinition.class));
        verify(transactionManagerMock).commit(transactionStatusMock);
        verify(transactionManagerMock, never()).rollback(transactionStatusMock);
    }

    @Test
    public void dadoQueFallaLaAccionCuandoVenceTemporizadorYDaIllegalStateExceptionConCausa() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        doAnswer(invocacion -> {
                Runnable accionProgramada = invocacion.getArgument(0);
                accionProgramada.run();
                return tareaMock;
            }).when(schedulerMock)
              .schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> gestorTemporizador.programarTemporizador(CODIGO, 0, () -> { throw new IllegalArgumentException("Error de prueba"); }));

        assertEquals("Error al ejecutar accion diferida del temporizador", error.getMessage());
        assertEquals(IllegalArgumentException.class, error.getCause().getClass());
        verify(transactionManagerMock).rollback(transactionStatusMock);
        verify(transactionManagerMock, never()).commit(transactionStatusMock);
    }

    @Test
    public void dadoQueFallaLaTransaccionCuandoVenceElTemporizadorYDaIllegalStateException() {
        ScheduledFuture<?> tareaMock = mock(ScheduledFuture.class);

        when(transactionManagerMock.getTransaction(any(TransactionDefinition.class)))
            .thenThrow(new IllegalArgumentException("No se pudo abrir la transaccion"));
        doAnswer(invocacion -> {
                Runnable accionProgramada = invocacion.getArgument(0);
                accionProgramada.run();
                return tareaMock;}).when(schedulerMock).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> gestorTemporizador.programarTemporizador(CODIGO, 0, () -> {}));

        assertEquals(IllegalArgumentException.class, error.getCause().getClass());
        verify(transactionManagerMock, never()).commit(transactionStatusMock);
    }

    @Test
    public void dadoQueHayDosPartidasCuandoSeCancelaUnaYDaLaOtraTodaviaActiva() {
        ScheduledFuture<?> primeraTarea = mock(ScheduledFuture.class);
        ScheduledFuture<?> segundaTarea = mock(ScheduledFuture.class);
        String otraPartida = "MATANZA456";

        doReturn(primeraTarea, segundaTarea).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        when(primeraTarea.isDone()).thenReturn(false);
        when(segundaTarea.getDelay(TimeUnit.MILLISECONDS)).thenReturn(30000L);

        gestorTemporizador.programarTemporizador(CODIGO, 30, () -> {});
        gestorTemporizador.programarTemporizador(otraPartida, 30, () -> {});
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        assertEquals(30, gestorTemporizador.obtenerSegundosRestantes(otraPartida));
        verify(primeraTarea).cancel(false);
        verify(segundaTarea, never()).cancel(false);
    }

    @Test
    public void dadoQueHayRelojNormalYEventoGlobalCuandoSeCancelaElNormalYDaElEventoActivo() {
        ScheduledFuture<?> tareaTurno = mock(ScheduledFuture.class);
        ScheduledFuture<?> tareaEvento = mock(ScheduledFuture.class);
        String claveEvento = "evento-global:" + CODIGO;

        doReturn(tareaTurno).when(schedulerMock).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        doReturn(tareaEvento).when(schedulerMock).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
        when(tareaTurno.isDone()).thenReturn(false);
        when(tareaEvento.getDelay(TimeUnit.MILLISECONDS)).thenReturn(15000L);

        gestorTemporizador.programarTemporizador(CODIGO, 30, () -> {});
        gestorTemporizador.programarTemporizador(claveEvento, 15, () -> {});
        gestorTemporizador.cancelarTemporizador(CODIGO);

        assertNull(gestorTemporizador.obtenerSegundosRestantes(CODIGO));
        assertEquals(15, gestorTemporizador.obtenerSegundosRestantes(claveEvento));
        verify(tareaTurno).cancel(false);
        verify(tareaEvento, never()).cancel(false);
    }
}
