
package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;
import com.tallerwebi.presentacion.DTO.EstadoRuletaDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

public class GestorRuletaPartidaTest {

    private ServicioRuleta servicioRuletaMock;
    private NotificadorPartida notificadorMock;
    private ScheduledExecutorService planificadorMock;
    private GestorRuletaPartida gestorRuleta;
    private GestorTriviaPartida gestorTriviaMock;

    @BeforeEach
    public void init() {

        servicioRuletaMock = mock(ServicioRuleta.class);
        notificadorMock = mock(NotificadorPartida.class);
        planificadorMock = mock(ScheduledExecutorService.class);
        gestorTriviaMock = mock(GestorTriviaPartida.class);

        gestorRuleta = new GestorRuletaPartida(
                servicioRuletaMock,
                notificadorMock,
                planificadorMock,
                gestorTriviaMock
        );
    }

    @Test
    public void deberiaSortearUnaSolaVezAnteSolicitudesDuplicadas() {

        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);

        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        EstadoRuletaDTO primero =
                gestorRuleta.sortearDificultad(
                        "ABC123", "ACT-1", 10L
                );

        EstadoRuletaDTO segundo =
                gestorRuleta.sortearDificultad(
                        "ABC123", "ACT-1", 10L
                );

        assertEquals(DificultadRuleta.FACIL, primero.dificultad());
        assertEquals(EstadoRuleta.SORTEADA, primero.estado());
        assertEquals(primero, segundo);
        assertEquals(primero, gestorRuleta.obtenerEstado("ABC123"));
        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.FACIL);
        verifyNoMoreInteractions(gestorTriviaMock);

        verify(servicioRuletaMock, times(1)).sortearDificultad();

        // Una notificación al abrir y otra al sortear.
        verify(notificadorMock, times(2))
                .notificarRuleta(
                        eq("ABC123"),
                        any(EstadoRuletaDTO.class)
                );
    }

    @Test
    public void noDeberiaPermitirQueOtroJugadorSortee() {

        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        assertThrows(
                SecurityException.class,
                () -> gestorRuleta.sortearDificultad(
                        "ABC123", "ACT-1", 20L
                )
        );
    }

    @Test
    public void deberiaRechazarUnaActivacionInexistente() {

        assertThrows(
                IllegalStateException.class,
                () -> gestorRuleta.sortearDificultad(
                        "ABC123", "ACT-99", 10L
                )
        );
    }

    @Test
    public void deberiaProgramarElGiroAutomatico() {

        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        verify(planificadorMock).schedule(
                any(Runnable.class),
                eq(10L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    public void deberiaReutilizarUnaActivacionRepetida() {

        EstadoRuletaDTO primero =
                gestorRuleta.activarRuleta(
                        "ABC123", "ACT-1", 10L
                );

        EstadoRuletaDTO segundo =
                gestorRuleta.activarRuleta(
                        "ABC123", "ACT-1", 10L
                );

        assertEquals(primero, segundo);
        assertEquals(EstadoRuleta.ESPERANDO, primero.estado());

        verify(planificadorMock, times(1)).schedule(
                any(Runnable.class),
                eq(10L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    public void noDeberiaSortearDosVecesSiElTemporizadorSeEjecutaDespuesDelGiroManual() {

        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.MEDIO);

        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        ArgumentCaptor<Runnable> tarea =
                ArgumentCaptor.forClass(Runnable.class);

        verify(planificadorMock).schedule(
                tarea.capture(),
                eq(10L),
                eq(TimeUnit.SECONDS)
        );

        // Simula el vencimiento del temporizador del servidor.
        tarea.getValue().run();
        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.MEDIO);
        verifyNoMoreInteractions(gestorTriviaMock);

        // Aunque llegue el temporizador, no debe repetir el sorteo.
        verify(servicioRuletaMock, times(1))
                .sortearDificultad();

        verify(notificadorMock, times(2))
                .notificarRuleta(
                        eq("ABC123"),
                        any(EstadoRuletaDTO.class)
                );
    }

    @Test
    public void deberiaMantenerElEstadoSorteado() {

        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.DIFICIL);

        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        EstadoRuletaDTO estado = gestorRuleta.obtenerEstado("ABC123");

        assertEquals(EstadoRuleta.SORTEADA, estado.estado());
        assertEquals(DificultadRuleta.DIFICIL, estado.dificultad());
    }

    private Runnable capturarSorteoAutomatico() {
        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        verify(planificadorMock).schedule(tarea.capture(), eq(10L), eq(TimeUnit.SECONDS));
        return tarea.getValue();
    }

    @Test
    public void sorteoManualNotificaLaDificultadAntesDeIniciarTrivia() {
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.DIFICIL);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        verifyNoInteractions(gestorTriviaMock);

        EstadoRuletaDTO resultado = gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        InOrder orden = inOrder(notificadorMock, gestorTriviaMock);
        orden.verify(notificadorMock).notificarRuleta("ABC123", resultado);
        orden.verify(gestorTriviaMock).iniciar("ABC123", DificultadRuleta.DIFICIL);
        verifyNoMoreInteractions(gestorTriviaMock);
    }

    @Test
    public void sorteoAutomaticoIniciaUnaTriviaAunqueSeRepitaElTemporizador() {
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        Runnable sorteo = capturarSorteoAutomatico();

        sorteo.run();
        sorteo.run();
        gestorRuleta.obtenerEstado("ABC123");
        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.FACIL);
        verifyNoMoreInteractions(gestorTriviaMock);
    }

    @Test
    public void unaNuevaActivacionPermiteIniciarOtroTorneo() {
        when(servicioRuletaMock.sortearDificultad())
                .thenReturn(DificultadRuleta.FACIL, DificultadRuleta.MEDIO);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        gestorRuleta.activarRuleta("ABC123", "ACT-2", 20L);
        gestorRuleta.sortearDificultad("ABC123", "ACT-2", 20L);

        verify(gestorTriviaMock).iniciar("ABC123", DificultadRuleta.FACIL);
        verify(gestorTriviaMock).iniciar("ABC123", DificultadRuleta.MEDIO);
        verifyNoMoreInteractions(gestorTriviaMock);
    }

    @Test
    public void unInicioFallidoConservaElSorteoYNoSeReintentaAutomaticamente() {
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.MEDIO);
        when(gestorTriviaMock.iniciar("ABC123", DificultadRuleta.MEDIO))
                .thenThrow(new IllegalStateException("No se pudo iniciar trivia"));
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        Runnable sorteo = capturarSorteoAutomatico();

        assertThrows(IllegalStateException.class,
                () -> gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));
        EstadoRuletaDTO resultado = gestorRuleta.obtenerEstado("ABC123");
        assertEquals(EstadoRuleta.SORTEADA, resultado.estado());
        assertEquals(DificultadRuleta.MEDIO, resultado.dificultad());
        assertDoesNotThrow(sorteo::run);
        assertEquals(resultado, gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));

        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.MEDIO);
        verifyNoMoreInteractions(gestorTriviaMock);
        verify(servicioRuletaMock, times(1)).sortearDificultad();
    }

    @Test
    public void unaNotificacionDeRuletaFallidaNoImpideIntentarElInicioDeTrivia() {
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.DIFICIL);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        doThrow(new IllegalStateException("Sin conexion WebSocket"))
                .when(notificadorMock).notificarRuleta(eq("ABC123"), any(EstadoRuletaDTO.class));

        assertThrows(IllegalStateException.class,
                () -> gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));

        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.DIFICIL);
        verifyNoMoreInteractions(gestorTriviaMock);
        assertEquals(EstadoRuleta.SORTEADA, gestorRuleta.obtenerEstado("ABC123").estado());
    }
}
