package com.tallerwebi.presentacion;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
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
        seleccion.setFichaSeleccionada(Ficha.ROJA);

        controladorWebSocket.seleccionarFicha(seleccion);

        verify(servicioSalaDeEsperaMock).seleccionarFicha("ABC123", 1L, Ficha.ROJA);

        verify(messagingTemplateMock).convertAndSend("/topic/partida/ABC123", seleccion);
    }

    @Test
    public void SeleccionarFichaOcupadaLanzaError() throws Exception {
        SeleccionFichaDTO seleccion = new SeleccionFichaDTO();
        seleccion.setCodigoUnicoPartida("ABC123");
        seleccion.setIdUsuario(2L);
        seleccion.setFichaSeleccionada(Ficha.ROJA);

        doThrow(new FichaOcupadaException("La ficha ya se encuentra seleccionada"))
            .when(servicioSalaDeEsperaMock)
            .seleccionarFicha("ABC123", 2L, Ficha.ROJA);

        controladorWebSocket.seleccionarFicha(seleccion);

        verify(messagingTemplateMock).convertAndSend(
            "/topic/partida/ABC123/errores",
            "La ficha ya se encuentra seleccionada"
        );
    }
}
