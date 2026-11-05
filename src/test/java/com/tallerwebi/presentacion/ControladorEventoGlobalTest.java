package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ControladorEventoGlobalTest {
    @Test
    void dadoQueNoHaySesionCuandoSeConsultaElEventoDaComoResultadoAccesoNoAutorizado() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.consultar("ABC", new MockHttpServletRequest()));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void dadoQueSeEnviaOtroIdCuandoSeTiraDaComoResultadoLaIdentidadDeLaSesion() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.tirar("ABC", 10L, 5, 2)).thenReturn(Map.of("activo", true));
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "Ana");
        solicitud.addParameter("idJugador", "20");
        assertEquals(HttpStatus.OK, controlador.tirar("ABC", 5, 2, solicitud).getStatusCode());
        verify(eventos).tirar("ABC", 10L, 5, 2);
    }

    @Test
    void dadoQueLaSesionPerteneceALaPartidaCuandoConsultaDaComoResultadoElEstadoSinCache() {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(participacion.pertenece(partida, 10L)).thenReturn(true);
        Map<String, Object> estado = Map.of("activo", true, "ronda", 5, "etapa", 1);
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.consultar("ABC", 10L)).thenReturn(estado);
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "ana");
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);

        // Ejecución
        var respuesta = controlador.consultar("ABC", solicitud);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(estado, respuesta.getBody());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verify(eventos).consultar("ABC", 10L);
        verify(participacion).pertenece(partida, 10L);
    }

    @Test
    void dadoQueSeEnviaOtroIdCuandoConfirmaDaComoResultadoLaConfirmacionDelJugadorAutenticado() {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(participacion.pertenece(partida, 10L)).thenReturn(true);
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.confirmar("ABC", 10L, 5)).thenReturn(Map.of("activo", false));
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "Ana");
        solicitud.getSession().setAttribute("id", 999L);
        solicitud.addParameter("idJugador", "20");
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);

        // Ejecución
        var respuesta = controlador.continuar("ABC", 5, solicitud);

        // Verificación
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(Map.of("activo", false), respuesta.getBody());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verify(eventos).confirmar("ABC", 10L, 5);
        verify(eventos, never()).confirmar("ABC", 20L, 5);
    }

    @Test
    void dadoQueNoHaySesionCuandoSeIntentaTirarDaComoResultadoRechazoSinLlamarAlServicio() {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controlador.tirar("ABC", 5, 1, new MockHttpServletRequest()));

        // Verificación
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void dadoQueNoHaySesionCuandoSeIntentaConfirmarDaComoResultadoRechazoSinReanudarLaPartida() {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controlador.continuar("ABC", 5, new MockHttpServletRequest()));

        // Verificación
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void dadoQueLaSesionEsAjenaCuandoConsultaDaComoResultadoAccesoProhibidoSinConsultarEventos() {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Partida partida = new Partida();
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "Intruso");
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);

        // Ejecución
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controlador.consultar("ABC", solicitud));

        // Verificación
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void dadoQueElServicioRechazaUnaTiradaCuandoSeSolicitaPorHttpDaComoResultadoConflictoJson() throws Exception {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(participacion.pertenece(partida, 10L)).thenReturn(true);
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.tirar("ABC", 10L, 5, 1)).thenThrow(new IllegalStateException("La tirada ya fue registrada."));
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controlador).build();

        // Ejecución
        var respuesta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/partida/ABC/evento-global/tirar").param("ronda", "5").param("etapa", "1")
            .sessionAttr("NOMBRE", "Ana").param("idJugador", "999")).andReturn().getResponse();

        // Verificación
        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentType().startsWith("application/json"));
        assertEquals("La tirada ya fue registrada.", new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(respuesta.getContentAsString()).get("error").asText());
        verify(eventos).tirar("ABC", 10L, 5, 1);
    }

    @Test
    void dadoQueElServicioRechazaAlParticipanteCuandoConfirmaPorHttpDaComoResultadoProhibidoJson() throws Exception {
        // Preparación
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(participacion.pertenece(partida, 10L)).thenReturn(true);
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.confirmar("ABC", 10L, 5)).thenThrow(new SecurityException("Ya no participás del evento."));
        var controlador = new ControladorEventoGlobal(eventos, partidas, participacion);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controlador).build();

        // Ejecución
        var respuesta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/partida/ABC/evento-global/continuar").param("ronda", "5")
            .sessionAttr("NOMBRE", "Ana")).andReturn().getResponse();

        // Verificación
        assertEquals(403, respuesta.getStatus());
        assertTrue(respuesta.getContentType().startsWith("application/json"));
        assertEquals("Ya no participás del evento.", new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(respuesta.getContentAsString()).get("error").asText());
        verify(eventos).confirmar("ABC", 10L, 5);
    }
}
