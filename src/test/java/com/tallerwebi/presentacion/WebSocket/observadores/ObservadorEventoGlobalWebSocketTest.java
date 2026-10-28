package com.tallerwebi.presentacion.WebSocket.observadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.eventos.EventoGlobalActualizado;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class ObservadorEventoGlobalWebSocketTest {

    private static final String CODIGO = "MATANZA123";

    private final SimpMessagingTemplate templateMock = mock(SimpMessagingTemplate.class);
    private final ObservadorEventoGlobalWebSocket observador = new ObservadorEventoGlobalWebSocket(templateMock);

    @Test
    public void dadoQueComienzaUnEventoGlobalCuandoElObservadorRecibeElInicioYDaLaNotificacionPorWebSocket() {

        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "INICIADO", TipoEventoGlobal.INUNDACION, "Se inundó la zona", 0, 15, List.of(1L, 2L), Map.of(), null);
        EventoGlobalActualizado evento = new EventoGlobalActualizado(estado);

        // Ejecución
        observador.notificarEventoGlobal(evento);

        // Comprobación
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", estado);
        verifyNoMoreInteractions(templateMock);
    }

    @Test
    public void dadoQueUnJugadorRealizaUnaTiradaCuandoElObservadorRecibeElResultadoYDaLaNotificacionPorWebSocket() {

        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "TIRADA", TipoEventoGlobal.PIQUETE, "Piquete", 0, 10, List.of(1L, 2L), Map.of(1L, 8), null);
        EventoGlobalActualizado evento = new EventoGlobalActualizado(estado);

        // Ejecución
        observador.notificarEventoGlobal(evento);

        // Comprobación
        assertEquals(Integer.valueOf(8), estado.tiradas().get(1L));
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", estado);
        verifyNoMoreInteractions(templateMock);
    }

    @Test
    public void dadoQueHayJugadoresEmpatadosCuandoElObservadorRecibeElDesempateYDaLaNotificacionPorWebSocket() {

        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "DESEMPATE", TipoEventoGlobal.TRAMITES_AFIP, "Trámites en AFIP", 1, 15, List.of(2L, 3L), Map.of(), null);
        EventoGlobalActualizado evento = new EventoGlobalActualizado(estado);

        // Ejecución
        observador.notificarEventoGlobal(evento);

        // Comprobación
        assertEquals(Integer.valueOf(1), estado.etapa());
        assertEquals(List.of(2L, 3L), estado.participantes());
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", estado);
        verifyNoMoreInteractions(templateMock);
    }

    @Test
    public void dadoQueFinalizaUnEventoGlobalCuandoElObservadorRecibeLaFinalizacionYDaLaNotificacionPorWebSocket() {

        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "FINALIZADO", TipoEventoGlobal.PIQUETE, "Piquete", 1, 0, List.of(1L, 2L), Map.of(1L, 7, 2L, 9), 1L);
        EventoGlobalActualizado evento = new EventoGlobalActualizado(estado);

        // Ejecución
        observador.notificarEventoGlobal(evento);

        // Comprobación
        assertEquals(Long.valueOf(1L), estado.idJugadorElegido());
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", estado);
        verifyNoMoreInteractions(templateMock);
    }

    @Test
    public void dadoQueHayDosPartidasCuandoSeNotificanSusEventosYDaUnMensajeEnCadaCanal() {

        // Preparación
        EstadoEventoGlobal estadoPrimera = new EstadoEventoGlobal("SALA001", "INICIADO", TipoEventoGlobal.PIQUETE, "Piquete", 0, 15, List.of(1L, 2L), Map.of(), null);
        EstadoEventoGlobal estadoSegunda = new EstadoEventoGlobal("SALA002", "INICIADO", TipoEventoGlobal.INUNDACION, "Inundación", 0, 15, List.of(3L, 4L), Map.of(), null);

        // Ejecución
        observador.notificarEventoGlobal(new EventoGlobalActualizado(estadoPrimera));
        observador.notificarEventoGlobal(new EventoGlobalActualizado(estadoSegunda));

        // Comprobación
        verify(templateMock).convertAndSend("/topic/partida/SALA001/evento-global", estadoPrimera);
        verify(templateMock).convertAndSend("/topic/partida/SALA002/evento-global", estadoSegunda);
        verifyNoMoreInteractions(templateMock);
    }

    @Test
    public void dadoQueUnEventoPasaPorVariasFasesCuandoElObservadorRecibeLasActualizacionesYDaTodasLasNotificaciones() {

        // Preparación
        EstadoEventoGlobal iniciado = new EstadoEventoGlobal(CODIGO, "INICIADO", TipoEventoGlobal.PIQUETE, "Piquete", 0, 15, List.of(1L, 2L), Map.of(), null);
        EstadoEventoGlobal tirada = new EstadoEventoGlobal(CODIGO, "TIRADA", TipoEventoGlobal.PIQUETE, "Piquete", 0, 8, List.of(1L, 2L), Map.of(1L, 6), null);
        EstadoEventoGlobal finalizado = new EstadoEventoGlobal(CODIGO, "FINALIZADO", TipoEventoGlobal.PIQUETE, "Piquete", 0, 0, List.of(1L, 2L), Map.of(1L, 6, 2L, 9), 1L);

        // Ejecución
        observador.notificarEventoGlobal(new EventoGlobalActualizado(iniciado));
        observador.notificarEventoGlobal(new EventoGlobalActualizado(tirada));
        observador.notificarEventoGlobal(new EventoGlobalActualizado(finalizado));

        // Comprobación
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", iniciado);
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", tirada);
        verify(templateMock).convertAndSend("/topic/partida/MATANZA123/evento-global", finalizado);
        verify(templateMock, times(3)).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/partida/MATANZA123/evento-global"), org.mockito.ArgumentMatchers.any(EstadoEventoGlobal.class));
        verifyNoMoreInteractions(templateMock);
    }
}
