package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.resultados.PosicionFicha;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static org.mockito.Mockito.*;

public class ControladorPosicionesWebSocketTest {

    @Test
    public void dadoQueSpringRecibeLaSolicitudCuandoResuelveElCodigoEntoncesPublicaLasPosiciones() throws Exception {
        ServicioPartida servicio = mock(ServicioPartida.class);
        SimpMessagingTemplate mensajeria = mock(SimpMessagingTemplate.class);
        List<PosicionFicha> posiciones = List.of(new PosicionFicha(1L, "Ana", "ROJO", 17));
        when(servicio.obtenerPosicionesFichas("ABC")).thenReturn(posiciones);
        try (StaticApplicationContext contexto = new StaticApplicationContext()) {
            contexto.getBeanFactory().registerSingleton("controladorPosiciones",
                    new ControladorPosicionesWebSocket(servicio, mensajeria));
            contexto.refresh();
            ExecutorSubscribableChannel entrada = new ExecutorSubscribableChannel();
            SimpAnnotationMethodMessageHandler manejador = new SimpAnnotationMethodMessageHandler(
                    entrada, new ExecutorSubscribableChannel(), mensajeria);
            manejador.setApplicationContext(contexto);
            manejador.setDestinationPrefixes(List.of("/app"));
            manejador.afterPropertiesSet();
            SimpMessageHeaderAccessor cabeceras = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            cabeceras.setSessionId("sesion-prueba");
            cabeceras.setSessionAttributes(java.util.Map.of());
            cabeceras.setDestination("/app/partida/ABC/solicitar-posiciones");

            manejador.handleMessage(MessageBuilder.createMessage(new byte[0], cabeceras.getMessageHeaders()));

            verify(mensajeria).convertAndSend("/topic/partida/ABC/posiciones", posiciones);
        }
    }

    // Verifica que el controlador obtenga las posiciones
    // del servicio y las envíe al canal WebSocket correcto.
    @Test
    public void dadoQueSeSolicitanLasPosicionesQuieroEnviarlasPorWebSocket() {

        ServicioPartida servicioPartida = mock(ServicioPartida.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        ControladorPosicionesWebSocket controlador =
                new ControladorPosicionesWebSocket(servicioPartida, messagingTemplate);

        String codigoPartida = "ABC123";

        PosicionFicha posicion = new PosicionFicha(
                59L,
                "Jugador1",
                "ROJO",
                5
        );

        List<PosicionFicha> posiciones = List.of(posicion);

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
