
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
import static org.mockito.Mockito.never;
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
    public void sortearNoDebeConsumirElTiempoDeLaPreguntaDuranteLaPresentacion() {
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);
        verify(gestorTriviaMock, never()).iniciar(any(), any());
    }

    @Test
    public void unaConsultaDeParticipantesFallidaLiberaLaReservaSinRepetirElSorteo() {
        var partidas = mock(com.tallerwebi.dominio.servicios.interfaces.ServicioPartida.class);
        when(partidas.obtenerPartida("ABC123")).thenThrow(new IllegalStateException("Sin base de datos"));
        gestorRuleta = new GestorRuletaPartida(servicioRuletaMock, notificadorMock, planificadorMock,
                gestorTriviaMock, partidas, mock(ServicioTriviaImpl.class));
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);
        gestorRuleta.reservarTrivia("ABC123");
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        Runnable automatico = capturarSorteoAutomatico();

        assertThrows(IllegalStateException.class, () -> gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));

        verify(gestorTriviaMock).liberarReserva("ABC123");
        EstadoRuletaDTO resultado = gestorRuleta.obtenerEstado("ABC123");
        assertEquals("CANCELADA", resultado.estado().name());
        verify(notificadorMock).notificarRuleta("ABC123", resultado);
        assertEquals(resultado, gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));
        automatico.run();
        gestorRuleta.confirmarPresentacion("ABC123", "ACT-1", 10L);
        verify(servicioRuletaMock, times(1)).sortearDificultad();
        verify(gestorTriviaMock, never()).iniciar(any(), any());
        verify(gestorTriviaMock, times(1)).liberarReserva("ABC123");
        gestorRuleta.reservarTrivia("ABC123");
        assertDoesNotThrow(() -> gestorRuleta.activarRuleta("ABC123", "ACT-2", 10L));
        automatico.run();
        assertThrows(IllegalStateException.class,
                () -> gestorRuleta.confirmarPresentacion("ABC123", "ACT-1", 10L));
        verify(gestorTriviaMock, times(1)).liberarReserva("ABC123");
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

        completarPresentacion(0);
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
        completarPresentacion(0);
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

        completarPresentacion(0);
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

        completarPresentacion(0);
        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.FACIL);
        verifyNoMoreInteractions(gestorTriviaMock);
    }

    @Test
    public void unaNuevaActivacionPermiteIniciarOtroTorneo() {
        when(servicioRuletaMock.sortearDificultad())
                .thenReturn(DificultadRuleta.FACIL, DificultadRuleta.MEDIO);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);
        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);

        completarPresentacion(0);
        gestorRuleta.activarRuleta("ABC123", "ACT-2", 20L);
        gestorRuleta.sortearDificultad("ABC123", "ACT-2", 20L);
        completarPresentacion(1);

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

        gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L);
        assertThrows(IllegalStateException.class, () -> completarPresentacion(0));
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

        completarPresentacion(0);
        verify(gestorTriviaMock, times(1)).iniciar("ABC123", DificultadRuleta.DIFICIL);
        verifyNoMoreInteractions(gestorTriviaMock);
        assertEquals(EstadoRuleta.SORTEADA, gestorRuleta.obtenerEstado("ABC123").estado());
    }

    @Test
    public void reservarLaTriviaBloqueaLaPartidaAntesDeActivarLaRuleta() {
        gestorRuleta.reservarTrivia("ABC123");

        verify(gestorTriviaMock).reservar("ABC123");
    }

    @Test
    public void unaActivacionInvalidaSinRuletaEnEsperaLiberaLaReserva() {
        assertThrows(IllegalArgumentException.class, () -> gestorRuleta.activarRuleta("ABC123", "", 10L));

        verify(gestorTriviaMock).liberarReserva("ABC123");
    }

    @Test
    public void unaActivacionRechazadaConOtraRuletaEnEsperaConservaLaReserva() {
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        assertThrows(IllegalStateException.class, () -> gestorRuleta.activarRuleta("ABC123", "ACT-2", 20L));

        verify(gestorTriviaMock, never()).liberarReserva(any());
    }
    private Runnable tareaPresentacion(long segundos, int indice) {
        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        verify(planificadorMock, org.mockito.Mockito.atLeastOnce()).schedule(tarea.capture(), eq(segundos), eq(TimeUnit.SECONDS));
        return tarea.getAllValues().get(indice);
    }

    @Test
    public void unaProgramacionFallidaCancelaLaTareaParcialYLiberaLaReserva() {
        var minimo = mock(java.util.concurrent.ScheduledFuture.class);
        org.mockito.Mockito.doReturn(minimo).when(planificadorMock)
                .schedule(any(Runnable.class), eq(7L), eq(TimeUnit.SECONDS));
        when(planificadorMock.schedule(any(Runnable.class), eq(45L), eq(TimeUnit.SECONDS)))
                .thenThrow(new java.util.concurrent.RejectedExecutionException("Planificador cerrado"));
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.MEDIO);
        gestorRuleta.activarRuleta("ABC123", "ACT-1", 10L);

        assertThrows(java.util.concurrent.RejectedExecutionException.class,
                () -> gestorRuleta.sortearDificultad("ABC123", "ACT-1", 10L));
        completarPresentacion(0);
        gestorRuleta.confirmarPresentacion("ABC123", "ACT-1", 10L);

        verify(minimo).cancel(false);
        verify(gestorTriviaMock).liberarReserva("ABC123");
        verify(gestorTriviaMock, never()).iniciar(any(), any());
    }

    private void completarPresentacion(int indice) {
        tareaPresentacion(7L, indice).run();
    }

    private void esperarJugadores(Long... ids) {
        var partidas = mock(com.tallerwebi.dominio.servicios.interfaces.ServicioPartida.class);
        var reglas = mock(ServicioTriviaImpl.class);
        var partida = new com.tallerwebi.dominio.entidades.Partida();
        var jugadores = java.util.Arrays.stream(ids).map(id -> {
            var jugador = new com.tallerwebi.dominio.entidades.Jugador(); jugador.setId(id); return jugador;
        }).toList();
        when(partidas.obtenerPartida("ABC123")).thenReturn(partida);
        when(reglas.participantesHabilitados(partida)).thenReturn(jugadores);
        gestorRuleta = new GestorRuletaPartida(servicioRuletaMock, notificadorMock, planificadorMock,
                gestorTriviaMock, partidas, reglas);
    }

    @Test
    public void esperaElMinimoYLasConfirmacionesDeTodosLosHabilitadosPeroNoDeEspectadores() {
        esperarJugadores(10L,20L);
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);
        gestorRuleta.activarRuleta("ABC123","ACT-1",10L);
        gestorRuleta.sortearDificultad("ABC123","ACT-1",10L);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",99L);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L);
        completarPresentacion(0);
        verify(gestorTriviaMock,never()).iniciar(any(),any());
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",20L);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",20L);
        tareaPresentacion(45L,0).run();
        verify(gestorTriviaMock,times(1)).iniciar("ABC123",DificultadRuleta.FACIL);
    }

    @Test
    public void confirmacionesAnticipadasNoPermitenEmpezarAntesDelMinimo() {
        esperarJugadores(10L);
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.MEDIO);
        gestorRuleta.activarRuleta("ABC123","ACT-1",10L);
        gestorRuleta.sortearDificultad("ABC123","ACT-1",10L);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L);
        verify(gestorTriviaMock,never()).iniciar(any(),any());
        completarPresentacion(0);
        verify(gestorTriviaMock).iniciar("ABC123",DificultadRuleta.MEDIO);
    }

    @Test
    public void laDesconexionNoBloqueaIndefinidamenteYElLimiteNoIniciaDosVeces() {
        esperarJugadores(10L,20L);
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.DIFICIL);
        gestorRuleta.activarRuleta("ABC123","ACT-1",10L);
        gestorRuleta.sortearDificultad("ABC123","ACT-1",10L);
        completarPresentacion(0);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L);
        verify(gestorTriviaMock,never()).iniciar(any(),any());
        Runnable limite = tareaPresentacion(45L,0);
        limite.run(); limite.run(); completarPresentacion(0);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",20L);
        verify(gestorTriviaMock,times(1)).iniciar("ABC123",DificultadRuleta.DIFICIL);
    }

    @Test
    public void unTemporizadorAnteriorNoIniciaElTorneoSiguiente() {
        esperarJugadores(10L);
        when(servicioRuletaMock.sortearDificultad()).thenReturn(DificultadRuleta.FACIL);
        gestorRuleta.activarRuleta("ABC123","ACT-1",10L);
        gestorRuleta.sortearDificultad("ABC123","ACT-1",10L);
        completarPresentacion(0);
        gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L);
        gestorRuleta.activarRuleta("ABC123","ACT-2",10L);
        gestorRuleta.sortearDificultad("ABC123","ACT-2",10L);
        tareaPresentacion(45L,0).run();
        completarPresentacion(1);
        verify(gestorTriviaMock,times(1)).iniciar(any(),any());
        assertThrows(IllegalStateException.class,() -> gestorRuleta.confirmarPresentacion("ABC123","ACT-1",10L));
        gestorRuleta.confirmarPresentacion("ABC123","ACT-2",10L);
        verify(gestorTriviaMock,times(2)).iniciar(any(),any());
    }

}
