package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {

    private ServicioPartida servicioPartidaMock;
    private ServicioDado servicioDadoMock;
    private SimpMessagingTemplate notificadorWebSocketMock;
    private ControladorPartida controladorPartida;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    public void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        servicioDadoMock = mock(ServicioDado.class);
        notificadorWebSocketMock = mock(SimpMessagingTemplate.class);

        controladorPartida = new ControladorPartida(
                servicioPartidaMock,
                servicioDadoMock,
                notificadorWebSocketMock
        );

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
        when(servicioPartidaMock.obtenerPartida(codigoEnSesion)).thenReturn(partidaMock);

        ModelAndView mav = controladorPartida.verTablero(null, request);

        assertEquals("partida", mav.getViewName());
        assertEquals(usuarioIdFallback, mav.getModel().get("usuarioSesionId"));
        assertNull(mav.getModel().get("turnoActualId"));
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

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
        verify(servicioPartidaMock, times(1)).obtenerJugadorActual(codigoPartida);

        assertNotNull(resultadoDTO);
        assertEquals(2, resultadoDTO.getIdJugadorActual());
        assertEquals("Messi", resultadoDTO.getApodoJugadorActual());

        verify(notificadorWebSocketMock, times(1)).convertAndSend(
                eq("/topic/partida/" + codigoPartida + "/turno"),
                any(TurnoDTO.class)
        );
    }
}