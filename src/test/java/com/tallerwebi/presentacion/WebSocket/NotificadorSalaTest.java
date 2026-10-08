
package com.tallerwebi.presentacion.WebSocket;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;

public class NotificadorSalaTest {

    private NotificadorSala notificadorSala;
    private SimpMessagingTemplate messagingTemplateMock;

    @BeforeEach
    public void init() {

        this.messagingTemplateMock = mock(SimpMessagingTemplate.class);

        this.notificadorSala = new NotificadorSala(
                messagingTemplateMock
        );
    }

    @Test
    public void notificarActualizacionDeSala() {

        SalaActualizadaDTO sala = new SalaActualizadaDTO(
                "ABC123",
                List.of("jugador1", "jugador2")
        );

        notificadorSala.notificarSalaActualizada(sala);

        verify(messagingTemplateMock, times(1))
                .convertAndSend("/topic/sala/ABC123", sala);
    }
}
