
package com.tallerwebi.presentacion.WebSocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        messagingTemplateMock = mock(SimpMessagingTemplate.class);
        notificadorSala = new NotificadorSala(messagingTemplateMock);
    }

    @Test
    public void dadoQueExisteUnaSalaDeEsperaCuandoSeActualizaSeDebeNotificar() {
        SalaActualizadaDTO dto = new SalaActualizadaDTO("ABC123", List.of("Juli", "Kevin"));

        notificadorSala.notificarSalaActualizada(dto);

        verify(messagingTemplateMock, times(1)).convertAndSend("/topic/sala/ABC123", dto);
    }

    @Test
    public void dadoQueExistenDosSalasDeEsperaCuandoSeNotificanCadaUnaDebeRecibirSuNotificacion() {
        SalaActualizadaDTO sala1 = new SalaActualizadaDTO("ABC123", List.of("Juli"));

        SalaActualizadaDTO sala2 = new SalaActualizadaDTO("XYZ789", List.of("Kevin"));

        notificadorSala.notificarSalaActualizada(sala1);
        notificadorSala.notificarSalaActualizada(sala2);

        verify(messagingTemplateMock).convertAndSend("/topic/sala/ABC123", sala1);

        verify(messagingTemplateMock).convertAndSend("/topic/sala/XYZ789", sala2);

        verify(messagingTemplateMock, times(2)).convertAndSend(anyString(), any(Object.class));
    }
}
