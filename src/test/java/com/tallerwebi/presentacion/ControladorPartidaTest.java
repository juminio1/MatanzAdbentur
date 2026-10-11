package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {

    private ControladorPartida controladorPartida;
    private ServicioPartida servicioPartidaMock;
    private HttpServletRequest requestMock;
    private HttpSession sessionMock;

    @BeforeEach
    public void setUp() {
        servicioPartidaMock = mock(ServicioPartida.class);
        requestMock = mock(HttpServletRequest.class);
        sessionMock = mock(HttpSession.class);

        when(requestMock.getSession()).thenReturn(sessionMock);

        controladorPartida = new ControladorPartida(servicioPartidaMock);
    }

    @Test
    public void queSePuedaVerElTableroDeUnaPartidaExistentePorCodigo() {
        // Dado
        String codigoSala = "ABC123";
        Long usuarioId = 1L;

        Partida partidaMock = new Partida();
        Jugador jugadorTurno = new Jugador();
        jugadorTurno.setId(10);
        partidaMock.setJugadorTurnoAleatorio(jugadorTurno);

        when(servicioPartidaMock.obtenerPartida(codigoSala)).thenReturn(partidaMock);
        when(sessionMock.getAttribute("IDUSUARIO")).thenReturn(usuarioId);

        // Cuando
        ModelAndView modelAndView = controladorPartida.verTablero(codigoSala, requestMock);

        // Entonces
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), equalToIgnoringCase("partida"));
        assertEquals(partidaMock, modelAndView.getModel().get("partida"));
        assertEquals(usuarioId, modelAndView.getModel().get("usuarioSesionId"));
        assertEquals(10L, modelAndView.getModel().get("turnoActualId"));
        verify(servicioPartidaMock, times(1)).obtenerPartida(codigoSala);
    }

    @Test
    public void queObtengaElCodigoDeLaSesionSiNoVieneEnLaURL() {
        // Dado
        String codigoSalaSesion = "XYZ987";
        Long usuarioId = 2L;

        Partida partidaMock = new Partida();
        when(sessionMock.getAttribute("codigoSala")).thenReturn(codigoSalaSesion);
        when(sessionMock.getAttribute("IDUSUARIO")).thenReturn(usuarioId);
        when(servicioPartidaMock.obtenerPartida(codigoSalaSesion)).thenReturn(partidaMock);

        // Cuando (se pasa parámetro nulo)
        ModelAndView modelAndView = controladorPartida.verTablero(null, requestMock);

        // Entonces
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), equalToIgnoringCase("partida"));
        assertEquals(partidaMock, modelAndView.getModel().get("partida"));
        verify(servicioPartidaMock, times(1)).obtenerPartida(codigoSalaSesion);
    }

    @Test
    public void queRedirijaASalaDeEsperaSiNoExistePartidaONoHayCodigo() {
        // Dado
        when(requestMock.getSession().getAttribute("codigoSala")).thenReturn(null);

        // Cuando
        ModelAndView modelAndView = controladorPartida.verTablero(null, requestMock);

        // Entonces
        assertNotNull(modelAndView);
        assertEquals("redirect:/sala-espera", modelAndView.getViewName());
    }
}
