package com.tallerwebi.presentacion.WebSocket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioSalaDeEspera;
import com.tallerwebi.presentacion.DTO.SeleccionFichaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class ControladorSalaEsperaWebSocketTest {

    private ServicioSalaDeEspera servicioSalaDeEsperaMock;
    private SimpMessagingTemplate messagingTemplateMock;
    private ControladorSalaEsperaWebSocket controladorWebSocket;

    @BeforeEach
    public void setUp() {
        servicioSalaDeEsperaMock = mock(ServicioSalaDeEspera.class);
        messagingTemplateMock = mock(SimpMessagingTemplate.class);

        controladorWebSocket = new ControladorSalaEsperaWebSocket(
            servicioSalaDeEsperaMock,
            messagingTemplateMock
        );
    }

    @Test
    public void SeleccionarFichaExitosa() throws Exception {
        SeleccionFichaDTO seleccion = new SeleccionFichaDTO();
        seleccion.setCodigoUnicoPartida("ABC123");
        seleccion.setIdUsuario(1L);
        seleccion.setFichaSeleccionada(ColorFicha.ROJO);

        controladorWebSocket.seleccionarFicha(seleccion);

        verify(servicioSalaDeEsperaMock).seleccionarFicha("ABC123", 1L, ColorFicha.ROJO);

        verify(messagingTemplateMock).convertAndSend("/topic/partida/ABC123", seleccion);
    }

    @Test
    public void SeleccionarFichaOcupadaLanzaError() throws Exception {
        SeleccionFichaDTO seleccion = new SeleccionFichaDTO();
        seleccion.setCodigoUnicoPartida("ABC123");
        seleccion.setIdUsuario(2L);
        seleccion.setFichaSeleccionada(ColorFicha.ROJO);

        doThrow(new FichaOcupadaException("La ficha ya se encuentra seleccionada"))
            .when(servicioSalaDeEsperaMock)
            .seleccionarFicha("ABC123", 2L, ColorFicha.ROJO);

        controladorWebSocket.seleccionarFicha(seleccion);

        verify(messagingTemplateMock).convertAndSend(
            "/topic/partida/ABC123/errores",
            "La ficha ya se encuentra seleccionada"
        );
    }

    
@Test
public void SeleccionarFichaExitosaSinMessagingTemplate() throws Exception {
    ControladorSalaEsperaWebSocket controladorSinMessaging =
        new ControladorSalaEsperaWebSocket(
            servicioSalaDeEsperaMock,
            null
        );

    SeleccionFichaDTO seleccion = new SeleccionFichaDTO();
    seleccion.setCodigoUnicoPartida("ABC123");
    seleccion.setIdUsuario(1L);
    seleccion.setFichaSeleccionada(ColorFicha.ROJO);

    assertDoesNotThrow(() ->
        controladorSinMessaging.seleccionarFicha(seleccion)
    );

    verify(servicioSalaDeEsperaMock).seleccionarFicha(
        "ABC123",
        1L,
        ColorFicha.ROJO
    );

    verifyNoInteractions(messagingTemplateMock);
}

@Test
public void SeleccionarFichaOcupadaSinMessagingTemplate() throws Exception {
    ControladorSalaEsperaWebSocket controladorSinMessaging =
        new ControladorSalaEsperaWebSocket(
            servicioSalaDeEsperaMock,
            null
        );

    SeleccionFichaDTO seleccion = new SeleccionFichaDTO();
    seleccion.setCodigoUnicoPartida("ABC123");
    seleccion.setIdUsuario(2L);
    seleccion.setFichaSeleccionada(ColorFicha.ROJO);

    doThrow(new FichaOcupadaException("La ficha ya se encuentra seleccionada"))
        .when(servicioSalaDeEsperaMock)
        .seleccionarFicha("ABC123", 2L, ColorFicha.ROJO);

    assertDoesNotThrow(() ->
        controladorSinMessaging.seleccionarFicha(seleccion)
    );

    verify(servicioSalaDeEsperaMock).seleccionarFicha(
        "ABC123",
        2L,
        ColorFicha.ROJO
    );

    verifyNoInteractions(messagingTemplateMock);
}


}
