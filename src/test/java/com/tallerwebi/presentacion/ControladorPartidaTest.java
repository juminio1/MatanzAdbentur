package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.servicios.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioDado;

import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class ControladorPartidaTest {

    private ControladorPartida controladorPartida;
    private ServicioDado servicioDadoMock;
    private ServicioPartida servicioPartidaMock;
    private SimpMessagingTemplate notificadorWebSocketMock;

    @BeforeEach
    public void init() {
        servicioDadoMock = mock(ServicioDado.class);
        servicioPartidaMock = mock(ServicioPartida.class);
        notificadorWebSocketMock = mock(SimpMessagingTemplate.class);

        controladorPartida = new ControladorPartida(
                servicioDadoMock,
                servicioPartidaMock,
                notificadorWebSocketMock
        );
    }

    @Test
    public void tirarDadosDeberiaDevolverResultadoDeTirada() {

        // preparacion
        ResultadoTirada resultadoEsperado = new ResultadoTirada(4, 5);

        when(servicioDadoMock.tirarDados())
                .thenReturn(resultadoEsperado);

        // ejecucion
        ResultadoTirada resultado =
                controladorPartida.tirarDados();

        // validacion
        assertThat(resultado, equalTo(resultadoEsperado));

        verify(servicioDadoMock, times(1))
                .tirarDados();
    }

    @Test
    public void alAvanzarTurnoDeberiaLlamarAlServicioYNotificarPorWebSocket() {
        String codigoPartida = "PARTIDA123";
        Jugador jugadorSiguiente = new Jugador();
        jugadorSiguiente.setId(2);
        jugadorSiguiente.setApodo("Messi");
        jugadorSiguiente.setOrden(1);

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(jugadorSiguiente);

        TurnoDTO resultadoDTO = controladorPartida.avanzarTurno(codigoPartida);

        // Verifica que se llamó a avanzar turno en el servicio
        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
        verify(servicioPartidaMock, times(1)).obtenerJugadorActual(codigoPartida);

        // Verifica el DTO devuelto
        assertNotNull(resultadoDTO);
        assertEquals(2, resultadoDTO.getIdJugadorActual());
        assertEquals("Messi", resultadoDTO.getApodoJugadorActual());

        // Verifica que se envió el mensaje por WebSocket al topic correspondiente
        verify(notificadorWebSocketMock, times(1)).convertAndSend(
                eq("/topic/partida/" + codigoPartida + "/turno"),
                any(TurnoDTO.class)
        );
    }
}
