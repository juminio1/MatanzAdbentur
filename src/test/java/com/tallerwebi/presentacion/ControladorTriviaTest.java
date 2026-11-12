package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.AccionTriviaInvalidaException;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import com.tallerwebi.dominio.excepcion.TriviaNoEncontradaException;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.GestorTriviaPartida;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.ValidadorParticipanteTrivia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ControladorTriviaTest {

    private static final String CODIGO = "ABC123";
    private static final String URL = "/partida/" + CODIGO + "/trivia";

    private GestorTriviaPartida gestor;
    private ServicioPartida servicioPartida;
    private MockMvc mvc;
    private Jugador ana;
    private Trivia trivia;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void init() {
        gestor = mock(GestorTriviaPartida.class);
        servicioPartida = mock(ServicioPartida.class);
        ServicioParticipacionImpl participacion = new ServicioParticipacionImpl();
        mvc = MockMvcBuilders.standaloneSetup(new ControladorTrivia(gestor, servicioPartida, participacion,
                new ValidadorParticipanteTrivia(participacion))).build();

        ana = jugador(1L, "Ana");
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(ana, jugador(2L, "Luna")));
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);

        trivia = new Trivia();
        trivia.setDificultad(DificultadRuleta.DIFICIL);
        trivia.setRondaActual(2);
        trivia.getParticipantes().add(ana);
        trivia.setInicioRonda(Instant.parse("2026-11-12T12:00:00Z"));
        trivia.setPreguntaActual(new PreguntaGenerada("DIFICIL", "¿Cuál es la cabecera de La Matanza?",
                List.of("Ramos Mejía", "San Justo", "Morón", "Haedo"), 1));
        when(gestor.consultarEstado(eq(CODIGO), any())).thenAnswer(
                invocacion -> invocacion.<Function<Trivia, Object>>getArgument(1).apply(trivia));
        when(gestor.consultarPregunta(eq(CODIGO), any())).thenAnswer(
                invocacion -> invocacion.<Function<Trivia, Object>>getArgument(1).apply(trivia));
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        return jugador;
    }

    private MockHttpSession sesionDe(String nombre) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("NOMBRE", nombre);
        return sesion;
    }

    private ResultActions responder(String json) throws Exception {
        return mvc.perform(post(URL + "/respuesta").session(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions elegirPremio(String json) throws Exception {
        return mvc.perform(post(URL + "/premio").session(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }

    @Test
    public void deberiaDevolverElEstadoDeLaTrivia() throws Exception {
        String json = cuerpo(mvc.perform(get(URL).session(sesionDe("Ana"))).andExpect(status().isOk()));

        assertTrue(json.contains("\"ronda\":2"));
        assertTrue(json.contains("\"apodo\":\"Ana\""));
        assertTrue(json.contains("\"dificultad\":\"DIFICIL\""));
    }

    @Test
    public void deberiaDevolverLaPreguntaDeLaRondaSinLaRespuestaCorrecta() throws Exception {
        String json = cuerpo(mvc.perform(get(URL + "/pregunta").session(sesionDe("Ana"))).andExpect(status().isOk()));

        assertTrue(json.contains("¿Cuál es la cabecera de La Matanza?"));
        assertTrue(json.contains("\"id\":2"));
        assertFalse(json.contains("respuestaCorrecta"));
        assertFalse(json.contains("opcionCorrecta"));
    }

    @Test
    public void deberiaResponder404SiNoHayTriviaEnLaPartida() throws Exception {
        when(gestor.consultarPregunta(eq(CODIGO), any())).thenThrow(new TriviaNoEncontradaException("No hay trivia."));

        String json = cuerpo(mvc.perform(get(URL + "/pregunta").session(sesionDe("Ana"))).andExpect(status().isNotFound()));

        assertTrue(json.contains("No hay trivia."));
    }

    @Test
    public void deberiaResponder404SiLaPartidaNoExisteOTermino() throws Exception {
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(null);

        mvc.perform(get(URL).session(sesionDe("Ana"))).andExpect(status().isNotFound());
    }

    @Test
    public void deberiaResponder401SinSesion() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void deberiaResponder403SiElJugadorNoEsDeLaPartida() throws Exception {
        mvc.perform(get(URL).session(sesionDe("Intruso"))).andExpect(status().isForbidden());
    }

    @Test
    public void deberiaRegistrarLaRespuestaDelJugadorAutenticado() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 1)).thenReturn(true);

        String json = cuerpo(responder("{\"id\":2,\"opcion\":1}").andExpect(status().isOk()));

        assertEquals("{\"correcta\":true}", json);
        verify(gestor).responder(CODIGO, 1L, 2, 1);
    }

    @Test
    public void alResponderNoDeberiaRevelarCualEraLaOpcionCorrecta() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 3)).thenReturn(false);

        String json = cuerpo(responder("{\"id\":2,\"opcion\":3}").andExpect(status().isOk()));

        assertEquals("{\"correcta\":false}", json);
    }

    @Test
    public void deberiaResponder409SiElJugadorYaRespondio() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 1)).thenThrow(new AccionTriviaInvalidaException("Ya respondiste esta pregunta."));

        String json = cuerpo(responder("{\"id\":2,\"opcion\":1}").andExpect(status().isConflict()));

        assertTrue(json.contains("Ya respondiste esta pregunta."));
    }

    @Test
    public void deberiaResponder403SiElJugadorFueEliminadoDeLaTrivia() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 1)).thenThrow(new ParticipanteTriviaNoHabilitadoException("Eliminado."));

        responder("{\"id\":2,\"opcion\":1}").andExpect(status().isForbidden());
    }

    @Test
    public void deberiaRechazarLaRespuestaDeUnJugadorPresoSinLlegarAlGestor() throws Exception {
        ana.getEstadoCarcel().encarcelar();

        String json = cuerpo(responder("{\"id\":2,\"opcion\":1}").andExpect(status().isForbidden()));

        assertTrue(json.contains("presos"));
        verify(gestor, never()).responder(anyString(), anyLong(), any(), anyInt());
    }

    @Test
    public void deberiaResponder400SiNoSeEligioOpcion() throws Exception {
        responder("{\"id\":2}").andExpect(status().isBadRequest());
    }

    @Test
    public void deberiaResponder400SiLaOpcionNoExiste() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 7)).thenThrow(new IllegalArgumentException("La opción elegida no es válida."));

        responder("{\"id\":2,\"opcion\":7}").andExpect(status().isBadRequest());
    }

    @Test
    public void deberiaPermitirAlGanadorElegirSuPremio() throws Exception {
        elegirPremio("{\"premio\":\"ROBAR\",\"posicion\":9}").andExpect(status().isOk());

        verify(gestor).elegirPremio(CODIGO, 1L, "ROBAR", 9);
    }

    @Test
    public void deberiaResponder403SiQuienEligeElPremioNoEsElGanador() throws Exception {
        org.mockito.Mockito.doThrow(new ParticipanteTriviaNoHabilitadoException("Solo el ganador puede elegir el premio."))
                .when(gestor).elegirPremio(CODIGO, 1L, "PLATA", null);

        String json = cuerpo(elegirPremio("{\"premio\":\"PLATA\"}").andExpect(status().isForbidden()));

        assertTrue(json.contains("Solo el ganador"));
    }

    @Test
    public void deberiaResponder409SiElPremioYaFueEntregado() throws Exception {
        org.mockito.Mockito.doThrow(new AccionTriviaInvalidaException("El premio ya fue entregado."))
                .when(gestor).elegirPremio(CODIGO, 1L, "PLATA", null);

        elegirPremio("{\"premio\":\"PLATA\"}").andExpect(status().isConflict());
    }

    @Test
    public void deberiaResponder400SiElPremioNoEsValido() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("El premio elegido no es válido."))
                .when(gestor).elegirPremio(CODIGO, 1L, "AUTO", null);

        elegirPremio("{\"premio\":\"AUTO\"}").andExpect(status().isBadRequest());
    }

    @Test
    public void unErrorSinMensajeDeberiaDevolverUnTextoGenerico() throws Exception {
        when(gestor.responder(CODIGO, 1L, 2, 1)).thenThrow(new AccionTriviaInvalidaException(null));

        String json = cuerpo(responder("{\"id\":2,\"opcion\":1}").andExpect(status().isConflict()));

        assertTrue(json.contains("No se pudo completar la acción."));
    }
}
