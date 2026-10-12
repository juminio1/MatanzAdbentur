package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {

    private ServicioPartida servicioPartidaMock;
    private ControladorPartida controladorPartida;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    public void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        controladorPartida = new ControladorPartida(servicioPartidaMock);
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
    }

    @Test
    public void dadoQueNoHayCodigoEnParametroNiEnSesionDebeRedirigirAHome() {
        ModelAndView mav = controladorPartida.verTablero(null, request);

        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    public void dadoQueElCodigoEsVacioOSoloEspaciosDebeRedirigirAHome() {
        ModelAndView mav = controladorPartida.verTablero("   ", request);

        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    public void dadoQueExisteCodigoPeroLaPartidaNoExisteDebeRedirigirASalaDeEsperaConCodigo() {
        String codigoPrueba = "MATANZA123";
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(null);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("redirect:/sala-de-espera?codigoUnico=" + codigoPrueba, mav.getViewName());
        verify(servicioPartidaMock, times(1)).obtenerPartida(codigoPrueba);
    }

    @Test
    public void dadoQueExistePartidaPeroElUsuarioNoEstaEnSesionDebeRedirigirALogin() {
        String codigoPrueba = "MATANZA123";
        Partida partidaMock = new Partida();
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaMock);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("redirect:/login", mav.getViewName());
    }

    @Test
    public void dadoQueExistePartidaYUsuarioTieneAtributoIdEnSesionDebeMostrarPantallaPartidaExitosamente() {
        String codigoPrueba = "MATANZA123";
        Long usuarioId = 10L;

        session.setAttribute("id", usuarioId);

        Partida partidaMock = new Partida();
        Jugador jugadorMock = new Jugador();
        jugadorMock.setId(1);
        partidaMock.setJugadorTurnoAleatorio(jugadorMock);

        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaMock);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("partida", mav.getViewName());
        assertNotNull(mav.getModel().get("partida"));
        assertEquals(partidaMock, mav.getModel().get("partida"));
        assertEquals(usuarioId, mav.getModel().get("usuarioSesionId"));
        assertEquals(1L, mav.getModel().get("turnoActualId"));
    }

    @Test
    public void dadoQueElCodigoEstaEnSesionYUsuarioSoloTieneIDUSUARIOEnSesionDebeAccederExitosamente() {
        String codigoEnSesion = "MATANZA999";
        Long usuarioIdFallback = 25L;

        session.setAttribute("codigoSala", codigoEnSesion);
        session.setAttribute("IDUSUARIO", usuarioIdFallback);

        Partida partidaMock = new Partida();
        // Sin jugador de turno configurado para probar el caso de turnoActualId = null
        when(servicioPartidaMock.obtenerPartida(codigoEnSesion)).thenReturn(partidaMock);

        ModelAndView mav = controladorPartida.verTablero(null, request);

        assertEquals("partida", mav.getViewName());
        assertEquals(usuarioIdFallback, mav.getModel().get("usuarioSesionId"));
        assertNull(mav.getModel().get("turnoActualId"));
    }
}
