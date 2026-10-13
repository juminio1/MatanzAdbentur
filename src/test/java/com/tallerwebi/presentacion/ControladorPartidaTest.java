package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {

    private ServicioPartida servicioPartidaMock;
    private SimpMessagingTemplate notificadorWebSocketMock;
    private ControladorPartida controladorPartida;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    public void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        notificadorWebSocketMock = mock(SimpMessagingTemplate.class);

        controladorPartida = new ControladorPartida(
                servicioPartidaMock,
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
        jugadorMock.setApodo("Messi");
        partidaMock.setJugadores(List.of(jugadorMock));
        partidaMock.setJugadorTurnoAleatorio(jugadorMock);

        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaMock);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPrueba)).thenReturn(jugadorMock);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("partida", mav.getViewName());
        assertNotNull(mav.getModel().get("partida"));
        assertEquals(partidaMock, mav.getModel().get("partida"));
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
    public void dadoQueUnUsuarioSinSesionIntentaAvanzarTurnoDebeRetornarUnauthorized() {
        String codigoPartida = "PARTIDA123";

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(codigoPartida, request);

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verify(servicioPartidaMock, never()).avanzarTurno(codigoPartida);
    }

    @Test
    public void dadoQueUnUsuarioSinTurnoIntentaAvanzarDebeRetornarForbidden() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("id", 99L);
        session.setAttribute("NOMBRE", "intruso");

        Jugador jugadorConTurno = new Jugador();
        jugadorConTurno.setId(1);
        jugadorConTurno.setApodo("pepito");

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(jugadorConTurno);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(codigoPartida, request);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        verify(servicioPartidaMock, never()).avanzarTurno(codigoPartida);
    }

    @Test
    public void alAvanzarTurnoElJugadorActualDeberiaAvanzarYNotificarPorWebSocket() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("id", 1L);
        session.setAttribute("NOMBRE", "pepito");

        Jugador jugadorActual = new Jugador();
        jugadorActual.setId(1);
        jugadorActual.setApodo("pepito");
        jugadorActual.setOrden(0);

        Jugador jugadorSiguiente = new Jugador();
        jugadorSiguiente.setId(2);
        jugadorSiguiente.setApodo("Messi");
        jugadorSiguiente.setOrden(1);

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida))
                .thenReturn(jugadorActual)
                .thenReturn(jugadorSiguiente);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(codigoPartida, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(2, respuesta.getBody().getIdJugadorActual());
        assertEquals("Messi", respuesta.getBody().getApodoJugadorActual());

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
        verify(notificadorWebSocketMock, times(1)).convertAndSend(
                eq("/topic/partida/" + codigoPartida + "/turno"),
                any(TurnoDTO.class)
        );
    }
}