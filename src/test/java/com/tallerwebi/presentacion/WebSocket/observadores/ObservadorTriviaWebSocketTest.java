package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ObservadorTriviaWebSocketTest {

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
