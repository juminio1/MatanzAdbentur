package com.tallerwebi.presentacion;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import com.tallerwebi.dominio.servicios.ServicioJugadaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.dominio.servicios.ServicioCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {
    private final ServicioEconomia economia = org.mockito.Mockito.spy(new ServicioEconomiaImpl());
    private final ServicioMovimiento movimiento = org.mockito.Mockito.spy(new ServicioMovimientoImpl(economia));

    private ServicioPartida servicioPartidaMock;
    private SimpMessagingTemplate notificadorWebSocketMock;
    private ServicioCasillero servicioCasilleroMock;
    private ServicioPropiedad servicioPropiedadMock;
    private ControladorPartida controladorPartida;
    private MockHttpServletRequest request;
    private MockHttpSession session;
    private NotificadorPartida notificadorPartidaMock;
    private ServicioGestionQuiebra servicioGestionQuiebraMock;

    @BeforeEach
    public void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        notificadorWebSocketMock = mock(SimpMessagingTemplate.class);
        servicioCasilleroMock = spy(new ServicioCasilleroImpl(mock(RepositorioCasillero.class)));
        servicioPropiedadMock = mock(ServicioPropiedad.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        servicioGestionQuiebraMock = mock(ServicioGestionQuiebra.class);

        controladorPartida = new ControladorPartida(servicioPartidaMock,
                notificadorWebSocketMock,
                servicioCasilleroMock,
                servicioPropiedadMock,
                notificadorPartidaMock,
                servicioGestionQuiebraMock,
                new ServicioJugadaImpl(servicioPartidaMock, servicioCasilleroMock, servicioPropiedadMock, null, servicioGestionQuiebraMock, movimiento));

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
        jugadorMock.setId(1L);
        jugadorMock.setApodo("Messi");
        partidaMock.setJugadores(List.of(jugadorMock));
        partidaMock.setJugadorTurnoAleatorio(jugadorMock);

        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaMock);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPrueba)).thenReturn(jugadorMock);

        ModelAndView mav = controladorPartida.verTablero(codigoPrueba, request);

        assertEquals("partida", mav.getViewName());
        assertNotNull(mav.getModel().get("partida"));
        assertEquals(partidaMock, mav.getModel().get("partida"));
        assertEquals(usuarioId, mav.getModel().get("usuarioSesionId"));
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
        jugadorConTurno.setId(1L);
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
        jugadorActual.setId(1L);
        jugadorActual.setApodo("pepito");
        jugadorActual.setOrden(0);

        Jugador jugadorSiguiente = new Jugador();
        jugadorSiguiente.setId(2L);
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
        assertEquals(2L, respuesta.getBody().getIdJugadorActual());
        assertEquals("Messi", respuesta.getBody().getApodoJugadorActual());
        assertEquals(2, respuesta.getBody().getRondaActual());

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
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
        jugador.setId(7L);
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
        jugadorAleatorio.setId(4L);
        jugadorAleatorio.setApodo("Jugador cuatro");

        Partida partida = new Partida();
        partida.setJugadores(List.of(jugadorAleatorio));
        partida.setJugadorTurnoAleatorio(jugadorAleatorio);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPartida)).thenReturn(jugadorAleatorio);

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
        jugadorActual.setId(1L);
        jugadorActual.setApodo("pepito");
        jugadorActual.setOrden(0);

        Jugador jugadorSiguiente = new Jugador();
        jugadorSiguiente.setId(2L);
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
        assertEquals(2L, respuesta.getBody().getIdJugadorActual());
        assertEquals(1, respuesta.getBody().getRondaActual());

        verify(servicioPartidaMock, times(1)).avanzarTurno(codigoPartida);
    }

    @Test
    public void tirarDadosDeberiaDevolverResultadoDeTirada() throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        String codigoPrueba = "MATANZA123";
        Long idJugador = 1L;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);

        Partida partida = new Partida();
        partida.setJugadores(Collections.singletonList(jugador));

        ResultadoTirada resultadoEsperado = new ResultadoTirada(4, 5); // Suma 9

        when(servicioPartidaMock.tirarDados()).thenReturn(resultadoEsperado);
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPrueba)).thenReturn(jugador);

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
        Long idJugador = 1L;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(10);

        Partida partida = new Partida();
        partida.setJugadores(Collections.singletonList(jugador));

        ResultadoTirada resultadoEsperado = new ResultadoTirada(4, 5); // Suma 9

        when(servicioPartidaMock.tirarDados()).thenReturn(resultadoEsperado);
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigoPrueba)).thenReturn(jugador);

        controladorPartida.tirarDados(codigoPrueba, idJugador);

        verify(movimiento, times(1)).moverJugador(jugador, 9, 30);
    }

    @Test
    public void queAlPasarPorInicioSePaguenEInformenLosDoscientos() throws Exception {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setPosicionActual(28);
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(servicioPartidaMock.obtenerPartida("INICIO")).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual("INICIO")).thenReturn(jugador);
        when(servicioPartidaMock.tirarDados()).thenReturn(new ResultadoTirada(2, 3));
        doAnswer(invocacion -> {
            jugador.setPosicionActual(3);
            return null;
        }).when(movimiento).moverJugador(jugador, 5, 30);

        RespuestaMovimientoDTO respuesta = controladorPartida.tirarDados("INICIO", 1L);

        assertEquals(200, respuesta.getDineroPorInicio());
        assertEquals(3, respuesta.getNuevaPosicion());
        verify(economia, times(1)).sumarDinero(jugador, 200);
        verify(notificadorWebSocketMock).convertAndSend("/topic/partida/INICIO", respuesta);
    }

    @Test
    public void queElPremioPorCruzarInicioEnUnEventoSeInformeSinPagarloDeNuevo() throws Exception {
        ServicioEventoCasillero servicioEvento = mock(ServicioEventoCasillero.class);
        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                notificadorWebSocketMock,
                servicioCasilleroMock,
                servicioPropiedadMock,
                notificadorPartidaMock,
                servicioGestionQuiebraMock,
                new ServicioJugadaImpl(servicioPartidaMock, servicioCasilleroMock, servicioPropiedadMock, servicioEvento, servicioGestionQuiebraMock, movimiento));
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setPosicionActual(26);
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(servicioPartidaMock.obtenerPartida("EVENTO")).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual("EVENTO")).thenReturn(jugador);
        when(servicioPartidaMock.tirarDados()).thenReturn(new ResultadoTirada(1, 1));
        doAnswer(invocacion -> {
            jugador.setPosicionActual(28);
            return null;
        }).when(movimiento).moverJugador(jugador, 2, 30);
        when(servicioEvento.aplicarSiCorresponde("EVENTO", jugador)).thenAnswer(invocacion -> {
            jugador.setPosicionActual(0);
            EventoCasillero evento = new EventoCasillero();
            evento.setTipoEfecto(com.tallerwebi.dominio.enums.TipoEfectoEvento.AVANZA_CASILLEROS);
            evento.setCantidadCasilleros(2);
            return Optional.of(evento);
        });

        RespuestaMovimientoDTO respuesta = controlador.tirarDados("EVENTO", 1L);

        assertEquals(200, respuesta.getDineroPorInicio());
        assertEquals(28, respuesta.getPosicionCaidaEvento());
        assertEquals(0, respuesta.getNuevaPosicion());
        verify(economia, never()).sumarDinero(any(), any());
    }

    @Test
    public void queLanceExcepcionSiElJugadorNoExisteEnLaPartida() {
        String codigoPrueba = "MATANZA123";
        Long idJugadorInexistente = 99L;

        Partida partidaVacia = new Partida();
        partidaVacia.setJugadores(Collections.emptyList());

        when(servicioPartidaMock.tirarDados()).thenReturn(new ResultadoTirada(2, 3));
        when(servicioPartidaMock.obtenerPartida(codigoPrueba)).thenReturn(partidaVacia);

        assertThrows(JugadorNoEncontradoException.class, () -> {
            controladorPartida.tirarDados(codigoPrueba, idJugadorInexistente);
        });
    }

    //test abandonar partida
    @Test
    public void dadoQueLaPartidaNoExisteCuandoUnJugadorIntentaAbandonarEntoncesDevuelveNotFound() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        MockHttpServletRequest request = new MockHttpServletRequest();

        when(servicioPartidaMock.obtenerPartida(codigoPartida))
                .thenReturn(null);

        ResponseEntity<Map<String, Object>> respuesta =
                controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("La partida no existe.", respuesta.getBody().get("error"));

        verify(servicioPartidaMock, never()).abandonarPartida(anyString(), anyLong());

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoIntentaAbandonarEntoncesDevuelveForbidden() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugador = new Jugador();
        jugador.setId(1L);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugador));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 99L);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("No pertenecés a esta partida.", respuesta.getBody().get("error"));

        verify(servicioPartidaMock, never()).abandonarPartida(anyString(), anyLong());

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueElJugadorPerteneceALaPartidaCuandoAbandonaEntoncesSeNotificaCorrectamente() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";
        Long idJugador = 1L;

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setApodo("Messi10");

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugador));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", idJugador);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("Abandonaste la partida correctamente.", respuesta.getBody().get("mensaje"));

        InOrder orden = inOrder(servicioPartidaMock, notificadorPartidaMock); //Se usa InOrder porque se quiere comprobar que primero abandona la partida y después se le notifica a los demás jugadores

        orden.verify(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        orden.verify(notificadorPartidaMock).notificarAbandonoPorWebSocket(partida, jugador);
    }

    @Test
    public void dadoQueElServicioNoEncuentraLaPartidaCuandoSeIntentaAbandonarEntoncesDevuelveNotFound() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";
        Long idJugador = 1L;

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugador));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", idJugador);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        doThrow(new PartidaNoEncontradaException()).when(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("No se encontró la partida.", respuesta.getBody().get("error"));

        verify(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueElServicioNoEncuentraAlJugadorCuandoSeIntentaAbandonarEntoncesDevuelveBadRequest() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";
        Long idJugador = 1L;

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugador));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", idJugador);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        doThrow(new IllegalArgumentException("El jugador no existe.")).when(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("No se encontró el jugador.", respuesta.getBody().get("error"));

        verify(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueElJugadorTieneSuNombreEnSesionCuandoAbandonaEntoncesSeNotificaCorrectamente() throws PartidaNoEncontradaException {

        String codigoPartida = "MATANZA123";
        Long idJugador = 1L;

        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);

        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setApodo("Messi10");

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugador));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("NOMBRE", "Messi10");

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());

        verify(servicioPartidaMock).abandonarPartida(codigoPartida, idJugador);

        verify(notificadorPartidaMock).notificarAbandonoPorWebSocket(partida, jugador);
    }

    @Test
    public void dadoQueUnJugadorPerteneceALaPartidaCuandoSeLoBuscaParaAbandonarEntoncesSeEncuentraCorrectamente() throws PartidaNoEncontradaException {
        // Preparación
        String codigoPartida = "ABC123";
        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
        NotificadorPartida notificadorPartidaMock = mock(NotificadorPartida.class);
        ControladorPartida controlador = new ControladorPartida(servicioPartidaMock,
                mock(SimpMessagingTemplate.class),
                mock(ServicioCasillero.class),
                mock(ServicioPropiedad.class),
                notificadorPartidaMock,
                org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), mock(ServicioPropiedad.class), null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento));

        Jugador jugadorUno = new Jugador();
        jugadorUno.setId(1L);
        jugadorUno.setApodo("Messi10");

        Jugador jugadorDos = new Jugador();
        jugadorDos.setId(2L);
        jugadorDos.setApodo("Prueba3");

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugadorUno, jugadorDos));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 2L);

        when(servicioPartidaMock.obtenerPartida(codigoPartida)).thenReturn(partida);

        // Ejecución
        ResponseEntity<Map<String, Object>> respuesta = controlador.abandonarPartida(codigoPartida, request);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(servicioPartidaMock, times(1)).abandonarPartida(codigoPartida, 2L);
        verify(notificadorPartidaMock, times(1)).notificarAbandonoPorWebSocket(partida, jugadorDos);
    }

    @Test
    void queUnJugadorEnQuiebraNoPuedaTirarDados() {
        String codigo = "PARTIDA123";

        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setEnBancarrota(true);

        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> controladorPartida.tirarDados(codigo, jugador.getId())
        );

        assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode());
        verify(servicioPartidaMock, never()).tirarDados();
    }

    // Verifica que una deuda pendiente impida tirar dados hasta resolverla.
    @Test
    void queUnJugadorConDeudaPendienteNoPuedaTirarDados() {
        String codigo = "PARTIDA123";

        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        jugador.setEnBancarrota(false);

        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        DeudaPendiente deuda = mock(DeudaPendiente.class);

        when(servicioGestionQuiebraMock.obtenerDeudaPendiente(codigo, jugador.getId()))
                .thenReturn(deuda);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> controladorPartida.tirarDados(codigo, jugador.getId())
        );

        assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode());

        verify(servicioPartidaMock, never()).tirarDados();
    }
}
