package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

public class NotificadorPartidaTest {

    //Este test verifica el envío del movimiento
    @Test
    public void dadoUnMovimientoQuieroNotificarloPorWebSocket() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        NotificadorPartida notificador = new NotificadorPartida(template);

        RespuestaMovimientoDTO movimiento = new RespuestaMovimientoDTO();
        movimiento.setIdJugador(59L);
        movimiento.setDado1(3);
        movimiento.setDado2(2);
        movimiento.setSuma(5);
        movimiento.setNuevaPosicion(8);

        // Ejecución
        notificador.notificarMovimiento("ABC123", movimiento);

        // Verificación
        verify(template).convertAndSend(
                "/topic/partida/ABC123",
                movimiento
        );
    }
}