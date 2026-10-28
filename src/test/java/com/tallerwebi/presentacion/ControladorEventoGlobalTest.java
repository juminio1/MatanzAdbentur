package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.excepcion.TiradasFaltantesException;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

public class ControladorEventoGlobalTest {

    private static final String CODIGO = "MATANZA123";

    private final ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);
    private final ServicioEvento servicioEventoMock = mock(ServicioEvento.class);
    private final ServicioParticipacion participacionMock = mock(ServicioParticipacion.class);
    private final ControladorEventoGlobal controladorEventoGlobal = new ControladorEventoGlobal(servicioPartidaMock, servicioEventoMock, participacionMock);

    private Partida crearPartidaConJugador() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setApodo("JugadorUno");

        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setJugadores(List.of(jugador));
        return partida;
    }

    private MockHttpServletRequest crearRequestAutenticado() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("NOMBRE", "JugadorUno");
        return request;
    }

    @Test
    public void dadoQueHayEventoGlobalActivoCuandoUnJugadorConsultaYDaElEstadoDelEvento() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "INICIADO", TipoEventoGlobal.PIQUETE, "Piquete", 0, 15, List.of(1L), Map.of(), null);

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.consultarEstadoEventoGlobal(CODIGO)).thenReturn(estado);

        // Ejecución
        ResponseEntity<EstadoEventoGlobal> respuesta = controladorEventoGlobal.consultarEvento(CODIGO, request);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(estado, respuesta.getBody());
        verify(servicioEventoMock).consultarEstadoEventoGlobal(CODIGO);
    }

    @Test
    public void dadoQueNoHayEventoGlobalActivoCuandoUnJugadorConsultaYDaNoContent() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.consultarEstadoEventoGlobal(CODIGO)).thenReturn(null);

        // Ejecución
        ResponseEntity<EstadoEventoGlobal> respuesta = controladorEventoGlobal.consultarEvento(CODIGO, request);

        // Verificación
        assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode());
        assertEquals(null, respuesta.getBody());
        verify(servicioEventoMock).consultarEstadoEventoGlobal(CODIGO);
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeConsultaElEventoYDaNotFound() {
        // Preparación
        MockHttpServletRequest request = crearRequestAutenticado();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(null);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.consultarEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("La partida no existe.", error.getReason());
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueNoHaySesionCuandoSeConsultaElEventoYDaUnauthorized() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.consultarEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Debés iniciar sesión.", error.getReason());
        verifyNoInteractions(participacionMock, servicioEventoMock);
    }

    @Test
    public void dadoQueElJugadorNoPerteneceCuandoConsultaElEventoYDaForbidden() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(false);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.consultarEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("No pertenecés a esta partida.", error.getReason());
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueElNombreEnSesionTieneOtrasMayusculasCuandoConsultaElEventoYDaAccesoCorrecto() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("NOMBRE", "jugadoruno");
        EstadoEventoGlobal estado = new EstadoEventoGlobal(CODIGO, "DESEMPATE", TipoEventoGlobal.INUNDACION, "Inundación", 1, 9, List.of(1L), Map.of(), null);

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.consultarEstadoEventoGlobal(CODIGO)).thenReturn(estado);

        // Ejecución
        ResponseEntity<EstadoEventoGlobal> respuesta = controladorEventoGlobal.consultarEvento(CODIGO, request);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("DESEMPATE", respuesta.getBody().fase());
        verify(participacionMock).pertenece(partida, 1L);
    }

    @Test
    public void dadoQueSoloExisteIdEnSesionCuandoConsultaElEventoYDaForbidden() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 1L);

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.consultarEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(participacionMock).pertenece(partida, null);
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueElJugadorParticipaCuandoTiraDadosDelEventoYDaElResultadoDeLosDosDados() throws Exception {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        ResultadoTirada tirada = new ResultadoTirada(4, 5);

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.tirarDadosEventoGlobal(CODIGO, 1L)).thenReturn(tirada);

        // Ejecución
        ResponseEntity<Map<String, Integer>> respuesta = controladorEventoGlobal.tirarDadosEvento(CODIGO, request);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(4, respuesta.getBody().get("dado1"));
        assertEquals(5, respuesta.getBody().get("dado2"));
        assertEquals(9, respuesta.getBody().get("suma"));
        verify(servicioEventoMock).tirarDadosEventoGlobal(CODIGO, 1L);
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoElJugadorIntentaTirarYDaNotFound() {
        // Preparación
        MockHttpServletRequest request = crearRequestAutenticado();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(null);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueElJugadorNoTieneSesionCuandoIntentaTirarYDaUnauthorized() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoIntentaTirarYDaForbidden() {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("NOMBRE", "Intruso");

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(participacionMock).pertenece(partida, null);
        verifyNoInteractions(servicioEventoMock);
    }

    @Test
    public void dadoQueElJugadorYaTiroCuandoIntentaTirarNuevamenteYDaTiradaDadoInvalidaException() throws Exception {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        TiradaDadoInvalidaException excepcion = new TiradaDadoInvalidaException("El jugador ya realizó su tirada");

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.tirarDadosEventoGlobal(CODIGO, 1L)).thenThrow(excepcion);

        // Ejecución
        TiradaDadoInvalidaException error = assertThrows(TiradaDadoInvalidaException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals("El jugador ya realizó su tirada", error.getMessage());
        verify(servicioEventoMock).tirarDadosEventoGlobal(CODIGO, 1L);
    }

    @Test
    public void dadoQueNoHayEventoGlobalDisponibleCuandoIntentaTirarYDaDatosEventoInvalidoException() throws Exception {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        DatosEnventoInvalidoExceptions excepcion = new DatosEnventoInvalidoExceptions("No hay un evento global activo");

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.tirarDadosEventoGlobal(CODIGO, 1L)).thenThrow(excepcion);

        // Ejecución
        DatosEnventoInvalidoExceptions error = assertThrows(DatosEnventoInvalidoExceptions.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals("No hay un evento global activo", error.getMessage());
        verify(servicioEventoMock).tirarDadosEventoGlobal(CODIGO, 1L);
    }

    @Test
    public void dadoQueElJugadorFueEliminadoDelDesempateCuandoIntentaTirarYDaTiradaInvalida() throws Exception {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        TiradaDadoInvalidaException excepcion = new TiradaDadoInvalidaException("El jugador no participa del desempate");

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.tirarDadosEventoGlobal(CODIGO, 1L)).thenThrow(excepcion);

        // Ejecución
        TiradaDadoInvalidaException error = assertThrows(TiradaDadoInvalidaException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals("El jugador no participa del desempate", error.getMessage());
        verify(servicioEventoMock).tirarDadosEventoGlobal(CODIGO, 1L);
    }

    @Test
    public void dadoQueFaltanTiradasCuandoElServicioRechazaLaOperacionYDaTiradasFaltantesException() throws Exception {
        // Preparación
        Partida partida = crearPartidaConJugador();
        MockHttpServletRequest request = crearRequestAutenticado();
        TiradasFaltantesException excepcion = new TiradasFaltantesException("Faltan tiradas");

        // When
        when(servicioPartidaMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(participacionMock.pertenece(partida, 1L)).thenReturn(true);
        when(servicioEventoMock.tirarDadosEventoGlobal(CODIGO, 1L)).thenThrow(excepcion);

        // Ejecución
        TiradasFaltantesException error = assertThrows(TiradasFaltantesException.class, () -> controladorEventoGlobal.tirarDadosEvento(CODIGO, request));

        // Verificación
        assertEquals("Faltan tiradas", error.getMessage());
        verify(servicioEventoMock).tirarDadosEventoGlobal(CODIGO, 1L);
    }

    @Test
    public void dadoQueLosDatosDelEventoSonInvalidosCuandoSeManejaLaExcepcionYDaConflict() {
        // Preparación
        DatosEnventoInvalidoExceptions excepcion = new DatosEnventoInvalidoExceptions("Evento no disponible");

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarEventoNoDisponible(excepcion);

        // Verificación
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Evento no disponible", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueLaTiradaEsInvalidaCuandoSeManejaLaExcepcionYDaConflict() {
        // Preparación
        TiradaDadoInvalidaException excepcion = new TiradaDadoInvalidaException("El jugador ya tiró");

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarEventoNoDisponible(excepcion);

        // Verificación
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("El jugador ya tiró", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueFaltanTiradasCuandoSeManejaLaExcepcionYDaConflict() {
        // Preparación
        TiradasFaltantesException excepcion = new TiradasFaltantesException("Faltan participantes");

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarEventoNoDisponible(excepcion);

        // Verificación
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Faltan participantes", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueLaExcepcionNoTieneMensajeCuandoSeManejaEventoNoDisponibleYDaMensajeGenerico() {
        // Preparación
        Exception excepcion = new Exception();

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarEventoNoDisponible(excepcion);

        // Verificación
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("No se pudo completar la operación", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueNoSeEncuentraElJugadorCuandoSeManejaLaExcepcionYDaForbidden() {
        // Preparación
        JugadorNoEncontradoException excepcion = new JugadorNoEncontradoException("Jugador inexistente");

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarJugadorNoEncontrado(excepcion);

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Jugador inexistente", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueLaExcepcionDelJugadorNoTieneMensajeCuandoSeManejaYDaMensajeGenerico() {
        // Preparación
        Exception excepcion = new Exception();

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarJugadorNoEncontrado(excepcion);

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("No se pudo completar la operación", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueNoExisteLaPartidaCuandoSeManejaLaExcepcionYDaNotFound() {
        // Preparación
        PartidaNoEncontradaException excepcion = new PartidaNoEncontradaException();

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarRecursoNoEncontrado(excepcion);

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Partida no encontrada", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueNoExisteElEventoCuandoSeManejaLaExcepcionYDaNotFound() {
        // Preparación
        EventoInexistenteException excepcion = new EventoInexistenteException("Evento no encontrado");

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarRecursoNoEncontrado(excepcion);

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Evento no encontrado", respuesta.getBody().get("error"));
    }

    @Test
    public void dadoQueLaExcepcionDelRecursoNoTieneMensajeCuandoSeManejaYDaMensajeGenerico() {
        // Preparación
        Exception excepcion = new Exception();

        // Ejecución
        ResponseEntity<Map<String, String>> respuesta = controladorEventoGlobal.manejarRecursoNoEncontrado(excepcion);

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Recurso no encontrado", respuesta.getBody().get("error"));
    }
}
