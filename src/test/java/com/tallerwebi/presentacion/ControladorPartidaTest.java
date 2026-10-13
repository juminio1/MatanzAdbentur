package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;

import java.util.Collections;
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
        servicioPartidaMock = mock(ServicioPartida.class);

        controladorPartida = new ControladorPartida(servicioPartidaMock, notificadorWebSocketMock);

        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
    }

    @Test
    public void dadoQueNoHayCodigoCuandoSeIntentaVerElTableroDaComoResultadoRedireccionAHome() {
        ModelAndView mav = controladorPartida.verTablero(null, request);

        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    public void dadoQueElCodigoEsVacioCuandoSeIntentaVerElTableroDaComoResultadoRedireccionAHome() {
        ModelAndView mav = controladorPartida.verTablero("   ", request);

        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeIntentaVerElTableroDaComoResultadoRedireccionASala() {
        String codigoPrueba = "MATANZA123";

        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(null);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("redirect:/sala-de-espera?codigoUnico=" + codigoPrueba, mav.getViewName());

        verify(servicioPartidaMock, times(1)).obtenerPartida(codigoPrueba);
    }

    @Test
    public void dadoQueNoHayUsuarioEnSesionCuandoSeIntentaVerElTableroDaComoResultadoRedireccionALogin() {
        String codigoPrueba = "MATANZA123";
        Partida partidaMock = new Partida();

        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaMock);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("redirect:/login", mav.getViewName());
    }

    @Test
    public void dadoQueExisteUnUsuarioEnSesionCuandoSeConsultaElTableroDaComoResultadoLaVistaPartida() {
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
    public void dadoQueElCodigoYUsuarioEstanEnSesionCuandoSeConsultaElTableroDaComoResultadoLaVistaPartida() {
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
    public void dadoQueUnUsuarioNoTieneSesionCuandoIntentaAvanzarTurnoDaComoResultadoUnauthorized() {
        String codigoPartida = "PARTIDA123";

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(
                codigoPartida,
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verify(servicioPartidaMock, never()).avanzarTurno(codigoPartida);
    }

    @Test
    public void dadoQueUnUsuarioNoTieneElTurnoCuandoIntentaAvanzarDaComoResultadoForbidden() {
        String codigoPartida = "PARTIDA123";

        session.setAttribute("id", 99L);
        session.setAttribute("NOMBRE", "intruso");

        Jugador jugadorConTurno = new Jugador();
        jugadorConTurno.setId(1);
        jugadorConTurno.setApodo("pepito");

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(jugadorConTurno);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(
                codigoPartida,
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        verify(servicioPartidaMock, never()).avanzarTurno(codigoPartida);
    }

    @Test
    public void dadoQueUnJugadorTieneElTurnoCuandoLoFinalizaDaComoResultadoElSiguienteTurnoYLaRondaNotificada() {
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

        Partida partidaActualizada = new Partida();
        partidaActualizada.setRondaActual(2);

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida))
                .thenReturn(jugadorActual)
                .thenReturn(jugadorSiguiente);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partidaActualizada);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(
                codigoPartida,
                request
        );

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(2, respuesta.getBody().getIdJugadorActual());
        assertEquals("Messi", respuesta.getBody().getApodoJugadorActual());
        assertEquals(2, respuesta.getBody().getRondaActual());

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);

        verify(notificadorWebSocketMock, times(1)).convertAndSend(
                "/topic/partida/" + codigoPartida + "/turno",
                respuesta.getBody()
        );
    }

    @Test
    public void dadoQueElCodigoEnSesionEsVacioCuandoSeConsultaElTableroDaComoResultadoRedireccionAHome() {
        session.setAttribute("codigoSala", "   ");

        ModelAndView mav = controladorPartida.verTablero(null, request);

        assertEquals("redirect:/home", mav.getViewName());
    }

    @Test
    public void dadoQueElUsuarioSeIdentificaPorNombreCuandoConsultaElTableroDaComoResultadoSuJugador() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("NOMBRE", "MESSI");

        Jugador jugador = new Jugador();
        jugador.setId(7);
        jugador.setApodo("Messi");

        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ModelAndView mav = controladorPartida.verTablero(codigoPartida, request);

        assertEquals("partida", mav.getViewName());
        assertEquals(7L, mav.getModel().get("usuarioSesionId"));
    }

    @Test
    public void dadoQueLaListaDeJugadoresEsNulaCuandoSeConsultaElTableroDaComoResultadoElUsuarioEnSesion() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("id", 14L);

        Partida partida = new Partida();
        partida.setJugadores(null);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ModelAndView mav = controladorPartida.verTablero(codigoPartida, request);

        assertEquals("partida", mav.getViewName());
        assertEquals(14L, mav.getModel().get("usuarioSesionId"));
    }

    @Test
    public void dadoQueNoSeObtieneElTurnoActualCuandoSeConsultaElTableroDaComoResultadoElJugadorAleatorio() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("id", 4L);

        Jugador jugadorAleatorio = new Jugador();
        jugadorAleatorio.setId(4);
        jugadorAleatorio.setApodo("Jugador cuatro");

        Partida partida = new Partida();
        partida.setJugadores(List.of(jugadorAleatorio));
        partida.setJugadorTurnoAleatorio(jugadorAleatorio);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(null);

        ModelAndView mav = controladorPartida.verTablero(codigoPartida, request);

        assertEquals("partida", mav.getViewName());
        assertEquals(4L, mav.getModel().get("turnoActualId"));
    }

    @Test
    public void dadoQueNoSeEncuentraElJugadorActualCuandoSeIntentaAvanzarDaComoResultadoForbidden() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("id", 1L);

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(null);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(
                codigoPartida,
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        verify(servicioPartidaMock, never()).avanzarTurno(codigoPartida);
    }

    @Test
    public void dadoQueElNombreCoincideCuandoSeFinalizaElTurnoDaComoResultadoElSiguienteJugador() {
        String codigoPartida = "PARTIDA123";
        session.setAttribute("NOMBRE", "PEPITO");

        Jugador jugadorActual = new Jugador();
        jugadorActual.setId(1);
        jugadorActual.setApodo("pepito");
        jugadorActual.setOrden(0);

        Jugador jugadorSiguiente = new Jugador();
        jugadorSiguiente.setId(2);
        jugadorSiguiente.setApodo("Messi");
        jugadorSiguiente.setOrden(1);

        Partida partida = new Partida();
        partida.setRondaActual(1);

        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida))
                .thenReturn(jugadorActual)
                .thenReturn(jugadorSiguiente);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ResponseEntity<TurnoDTO> respuesta = controladorPartida.avanzarTurno(
                codigoPartida,
                request
        );

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(2, respuesta.getBody().getIdJugadorActual());
        assertEquals(1, respuesta.getBody().getRondaActual());

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
        verify(notificadorWebSocketMock, times(1)).convertAndSend(
                "/topic/partida/" + codigoPartida + "/turno",
                respuesta.getBody()
        );
    }

    @Test
    public void tirarDadosDeberiaDevolverResultadoDeTirada() throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        String codigoPrueba = "MATANZA123";
        int idJugador = 1;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);

        Partida partida = new Partida();
        partida.setJugadores(Collections.singletonList(jugador));

        ResultadoTiradaDTO resultadoEsperado = new ResultadoTiradaDTO(4, 5); // Suma 9

        when(servicioPartidaMock.tirarDados()).thenReturn(resultadoEsperado);
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partida);

        RespuestaMovimientoDTO resultado = controladorPartida.tirarDados(codigoPrueba, idJugador);

        assertThat(resultado, notNullValue());
        assertThat(resultado.getDado1(), equalTo(4));
        assertThat(resultado.getDado2(), equalTo(5));
        assertThat(resultado.getSuma(), equalTo(9));

        // SE VERIFICA SOBRE servicioPartidaMock
        verify(servicioPartidaMock, times(1)).tirarDados();
    }

    @Test
    public void tirarDadosDeberiaActualizarPosicion() throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        String codigoPrueba = "MATANZA123";
        int idJugador = 1;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(10);

        Partida partida = new Partida();
        partida.setJugadores(Collections.singletonList(jugador));

        ResultadoTiradaDTO resultadoEsperado = new ResultadoTiradaDTO(4, 5); // Suma 9

        when(servicioPartidaMock.tirarDados()).thenReturn(resultadoEsperado);
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partida);

        controladorPartida.tirarDados(codigoPrueba, idJugador);

        verify(servicioPartidaMock, times(1)).moverJugador(jugador, 9, 30);
    }

    @Test
    public void queLanceExcepcionSiElJugadorNoExisteEnLaPartida() {
        String codigoPrueba = "MATANZA123";
        int idJugadorInexistente = 99;

        Partida partidaVacia = new Partida();
        partidaVacia.setJugadores(Collections.emptyList());

        when(servicioPartidaMock.tirarDados()).thenReturn(new ResultadoTiradaDTO(2, 3));
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaVacia);

        assertThrows(JugadorNoEncontradoException.class, () -> {
            controladorPartida.tirarDados(codigoPrueba, idJugadorInexistente);
        });
    }
}
