package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

public class ControladorPosicionesWebSocketTest {

    // Verifica que el controlador obtenga las posiciones
    // del servicio y las envíe al canal WebSocket correcto.
    @Test
    public void dadoQueSeSolicitanLasPosicionesQuieroEnviarlasPorWebSocket() {

        ServicioPartida servicioPartida = mock(ServicioPartida.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        ControladorPosicionesWebSocket controlador =
                new ControladorPosicionesWebSocket(servicioPartida, messagingTemplate);

        String codigoPartida = "ABC123";

        PosicionFichaDTO posicion = new PosicionFichaDTO(
                59L,
                "Jugador1",
                "ROJO",
                5
        );

        List<PosicionFichaDTO> posiciones = List.of(posicion);

        when(servicioPartida.obtenerPosicionesFichas(codigoPartida))
                .thenReturn(posiciones);
        controlador.solicitarPosiciones(codigoPartida);

        // Verifica que se consulten las posiciones de la partida.
        verify(servicioPartida).obtenerPosicionesFichas(codigoPartida);

        // Verifica que las posiciones se envíen al canal correspondiente.
        verify(messagingTemplate).convertAndSend(
                "/topic/partida/ABC123/posiciones",
                posiciones
        );
    }
}
