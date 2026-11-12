package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.presentacion.DTO.ParticipanteTriviaDTO;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.presentacion.DTO.EstadoTriviaDTO;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.mapeadores.MapeadorTrivia;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ObservadorTriviaWebSocketTest {

    @Test
    public void premioEntregadoNotificaUnaVezElEstadoPublicoDelGanadorSinDatosPrivados() {
        Trivia trivia = triviaFinalizadaConGanador();
        trivia.setPreguntaActual(new PreguntaGenerada("DIFICIL", "Pregunta privada",
                List.of("A", "B", "C", "D"), 2));
        trivia.registrarPremioEntregado();
        NotificadorPartida notificador = mock(NotificadorPartida.class);

        new ObservadorTriviaWebSocket(notificador).actualizar(new EventoTriviaActualizada(
                "ABC123", TipoAvisoTrivia.PREMIO_ENTREGADO, trivia, null));

        ArgumentCaptor<Map<String, Object>> contenido = ArgumentCaptor.forClass(Map.class);
        verify(notificador, times(1)).notificarTrivia(eq("ABC123"), contenido.capture());
        verifyNoMoreInteractions(notificador);
        Map<String, Object> mensaje = contenido.getValue();
        assertEquals(Set.of("tipo", "estado"), mensaje.keySet());
        assertEquals(TipoAvisoTrivia.PREMIO_ENTREGADO, mensaje.get("tipo"));
        EstadoTriviaDTO estado = assertInstanceOf(EstadoTriviaDTO.class, mensaje.get("estado"));
        assertEquals(MapeadorTrivia.estado("ABC123", trivia), estado);
        assertTrue(estado.finalizada());
        assertEquals(1L, estado.idGanador());
        assertEquals("Ana", estado.ganador());
        assertFalse(estado.premioPendiente());
        assertEquals(List.of(new ParticipanteTriviaDTO(1L, "Ana", 0L)), estado.enJuego());
        JsonNode json = new ObjectMapper().valueToTree(mensaje);
        assertFalse(json.toString().contains("Pregunta privada"));
        assertFalse(json.toString().contains("preguntaActual"));
        assertFalse(json.toString().contains("respuestaCorrecta"));
        assertFalse(json.toString().contains("opcionCorrecta"));
    }

    private EstadoTriviaDTO notificarFinalizada(Trivia trivia) {
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        ObservadorTriviaWebSocket observador = new ObservadorTriviaWebSocket(notificador);

        observador.actualizar(new EventoTriviaActualizada(
                "ABC123", TipoAvisoTrivia.FINALIZADA, trivia, null));

        ArgumentCaptor<Map<String, Object>> contenido = ArgumentCaptor.forClass(Map.class);
        verify(notificador, times(1)).notificarTrivia(eq("ABC123"), contenido.capture());
        verifyNoMoreInteractions(notificador);
        Map<String, Object> mensaje = contenido.getValue();
        assertEquals(Set.of("tipo", "estado"), mensaje.keySet());
        assertEquals(TipoAvisoTrivia.FINALIZADA, mensaje.get("tipo"));
        EstadoTriviaDTO estado = assertInstanceOf(EstadoTriviaDTO.class, mensaje.get("estado"));
        assertEquals("ABC123", estado.codigoPartida());
        assertTrue(estado.finalizada());
        assertNull(estado.venceEn());
        JsonNode json = new ObjectMapper().valueToTree(mensaje);
        assertEquals("FINALIZADA", json.path("tipo").asText());
        assertFalse(json.toString().contains("preguntaActual"));
        assertFalse(json.toString().contains("respuestaCorrecta"));
        assertFalse(json.toString().contains("opcionCorrecta"));
        for (Object participante : estado.enJuego()) {
            assertInstanceOf(ParticipanteTriviaDTO.class, participante);
        }
        return estado;
    }

    private Trivia triviaFinalizadaConGanador() {
        Trivia trivia = new Trivia();
        trivia.setDificultad(DificultadRuleta.DIFICIL);
        trivia.setRondaActual(5);
        Jugador ana = new Jugador();
        ana.setId(1L);
        ana.setApodo("Ana");
        trivia.getParticipantes().add(ana);
        trivia.declararGanador(ana);
        trivia.finalizar();
        return trivia;
    }

    @Test
    public void finalizadaConGanadorNotificaSuIdentidadYElPremioPendiente() {
        EstadoTriviaDTO estado = notificarFinalizada(triviaFinalizadaConGanador());

        assertEquals(1L, estado.idGanador());
        assertEquals("Ana", estado.ganador());
        assertTrue(estado.premioPendiente());
        assertEquals(List.of(new ParticipanteTriviaDTO(1L, "Ana", 0L)), estado.enJuego());
    }

    @Test
    public void finalizadaConPremioEntregadoNotificaQueNoQuedaPremioPendiente() {
        Trivia trivia = triviaFinalizadaConGanador();
        trivia.registrarPremioEntregado();

        EstadoTriviaDTO estado = notificarFinalizada(trivia);

        assertEquals(1L, estado.idGanador());
        assertEquals("Ana", estado.ganador());
        assertFalse(estado.premioPendiente());
    }

    @Test
    public void finalizadaSinGanadorNotificaIdentidadNulaYSinPremioPendiente() {
        Trivia trivia = new Trivia();
        trivia.finalizar();

        EstadoTriviaDTO estado = notificarFinalizada(trivia);

        assertNull(estado.idGanador());
        assertNull(estado.ganador());
        assertFalse(estado.premioPendiente());
    }

    @Test
    public void unaRondaCerradaNotificaUnaVezLaOpcionCorrectaSinExponerEntidadesDelDominio() {
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        ObservadorTriviaWebSocket observador = new ObservadorTriviaWebSocket(notificador);
        Trivia trivia = new Trivia();
        trivia.setDificultad(DificultadRuleta.DIFICIL);
        trivia.setRondaActual(5);
        trivia.setPreguntaActual(new PreguntaGenerada("DIFICIL", "Pregunta final",
                List.of("A", "B", "C", "D"), 2));
        Jugador ana = new Jugador();
        ana.setId(1L);
        ana.setApodo("Ana");
        trivia.getParticipantes().add(ana);
        trivia.declararGanador(ana);
        trivia.finalizar();

        observador.actualizar(new EventoTriviaActualizada(
                "ABC123", TipoAvisoTrivia.RONDA_CERRADA, trivia, 2));

        ArgumentCaptor<Map<String, Object>> contenido = ArgumentCaptor.forClass(Map.class);
        verify(notificador, times(1)).notificarTrivia(eq("ABC123"), contenido.capture());
        verifyNoMoreInteractions(notificador);
        Map<String, Object> mensaje = contenido.getValue();
        assertEquals(Set.of("tipo", "estado", "rondaCerrada", "opcionCorrecta", "eliminados"),
                mensaje.keySet());
        assertEquals(TipoAvisoTrivia.RONDA_CERRADA, mensaje.get("tipo"));
        assertEquals(5, mensaje.get("rondaCerrada"));
        assertEquals(2, mensaje.get("opcionCorrecta"));
        assertEquals(List.of(), mensaje.get("eliminados"));
        EstadoTriviaDTO estado = assertInstanceOf(EstadoTriviaDTO.class, mensaje.get("estado"));
        assertEquals(List.of(new ParticipanteTriviaDTO(1L, "Ana", 0L)), estado.enJuego());
        JsonNode json = new ObjectMapper().valueToTree(mensaje);
        JsonNode participante = json.path("estado").path("enJuego").path(0);
        Set<String> campos = new java.util.HashSet<>();
        participante.fieldNames().forEachRemaining(campos::add);
        assertEquals(Set.of("id", "apodo", "milisegundos"), campos);
        assertFalse(json.toString().contains("preguntaActual"));
        assertFalse(json.toString().contains("respuestaCorrecta"));
    }

    // Verifica que el observador envíe una nueva ronda de trivia
    // con la información necesaria, sin mostrar la respuesta correcta.
    @Test
    public void dadaUnaRondaNuevaNotificaUnaVezElEstadoYLaPreguntaPublicosSinLaRespuestaCorrecta() {
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        ObservadorTriviaWebSocket observador = new ObservadorTriviaWebSocket(notificador);
        List<String> opciones = List.of("Ramos Mejía", "San Justo", "Morón", "Haedo");
        Trivia trivia = new Trivia();
        trivia.setDificultad(DificultadRuleta.FACIL);
        trivia.setRondaActual(1);
        trivia.prepararPreguntas(List.of(new PreguntaGenerada(
                "FACIL", "¿Cuál es la cabecera de La Matanza?", opciones, 1)));
        trivia.setInicioRonda(Instant.parse("2026-11-12T12:00:00Z"));
        EventoTriviaActualizada evento = new EventoTriviaActualizada(
                "ABC123", TipoAvisoTrivia.RONDA_NUEVA, trivia, null);

        observador.actualizar(evento);

        ArgumentCaptor<Map<String, Object>> contenido = ArgumentCaptor.forClass(Map.class);
        verify(notificador, times(1)).notificarTrivia(eq("ABC123"), contenido.capture());
        verifyNoMoreInteractions(notificador);
        Map<String, Object> mensaje = contenido.getValue();
        assertEquals(Set.of("tipo", "estado", "pregunta"), mensaje.keySet());
        EstadoTriviaDTO estado = assertInstanceOf(EstadoTriviaDTO.class, mensaje.get("estado"));
        PreguntaTriviaDTO pregunta = assertInstanceOf(PreguntaTriviaDTO.class, mensaje.get("pregunta"));
        assertEquals(MapeadorTrivia.estado("ABC123", trivia), estado);
        assertEquals(MapeadorTrivia.pregunta(trivia), pregunta);
        assertEquals("ABC123", estado.codigoPartida());
        assertEquals(1, pregunta.ronda());
        assertEquals(1, pregunta.totalRondas());
        assertEquals("¿Cuál es la cabecera de La Matanza?", pregunta.enunciado());
        assertEquals(opciones, pregunta.opciones());
        assertEquals("FACIL", pregunta.dificultad());
        assertEquals(30, pregunta.segundosParaResponder());
        assertEquals(Instant.parse("2026-11-12T12:00:30Z").toEpochMilli(), pregunta.venceEn());

        JsonNode json = new ObjectMapper().valueToTree(mensaje);
        assertEquals("RONDA_NUEVA", json.path("tipo").asText());
        assertFalse(json.toString().contains("respuestaCorrecta"));
        assertFalse(json.toString().contains("opcionCorrecta"));
        assertFalse(json.toString().contains("preguntaActual"));
    }
}
