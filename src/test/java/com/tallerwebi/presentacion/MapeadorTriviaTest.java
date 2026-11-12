package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.GestorTriviaPartida;
import com.tallerwebi.presentacion.DTO.EstadoTriviaDTO;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import com.tallerwebi.presentacion.mapeadores.MapeadorTrivia;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MapeadorTriviaTest {

    private static final long VENCE_EN = 1_800_000_000_000L;

    private PreguntaGenerada pregunta() {
        List<String> opciones = new ArrayList<>(List.of("Ramos Mejía", "San Justo", "Morón", "Haedo"));
        return new PreguntaGenerada("DIFICIL", "¿Cuál es la cabecera del partido de La Matanza?", opciones, 1);
    }

    @Test
    public void deberiaCopiarElEnunciadoLasOpcionesYLaDificultadDeLaPregunta() {
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, 5, VENCE_EN);

        assertEquals("¿Cuál es la cabecera del partido de La Matanza?", dto.enunciado());
        assertEquals(List.of("Ramos Mejía", "San Justo", "Morón", "Haedo"), dto.opciones());
        assertEquals("DIFICIL", dto.dificultad());
    }

    @Test
    public void deberiaIncluirLaRondaYElVencimientoDeLaPregunta() {
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, 5, VENCE_EN);

        assertEquals(2, dto.id());
        assertEquals(2, dto.ronda());
        assertEquals(5, dto.totalRondas());
        assertEquals(GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA, dto.segundosParaResponder());
        assertEquals(VENCE_EN, dto.venceEn());
    }

    @Test
    public void nuncaDeberiaEnviarLaRespuestaCorrectaAlNavegador() throws Exception {
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, 5, VENCE_EN);

        String json = new ObjectMapper().writeValueAsString(dto);

        assertFalse(json.contains("respuestaCorrecta"));
        assertFalse(json.contains("correcta"));
        assertTrue(json.contains("San Justo"));
    }

    @Test
    public void noDeberiaVerseAfectadoSiLuegoSeModificanLasOpcionesDeLaPregunta() {
        PreguntaGenerada pregunta = pregunta();
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta, 2, 5, VENCE_EN);

        pregunta.getOpciones().set(0, "Opción cambiada");

        assertEquals("Ramos Mejía", dto.opciones().get(0));
    }

    @Test
    public void deberiaRechazarUnaPreguntaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> MapeadorTrivia.desdePregunta(null, 1, 5, VENCE_EN));
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        return jugador;
    }

    private Trivia trivia() {
        Trivia trivia = new Trivia();
        trivia.setDificultad(DificultadRuleta.DIFICIL);
        trivia.setRondaActual(3);
        trivia.setInicioRonda(Instant.ofEpochMilli(VENCE_EN));
        trivia.setPreguntaActual(pregunta());
        return trivia;
    }

    @Test
    public void deberiaArmarLaPreguntaEnJuegoDesdeLaTrivia() {
        PreguntaTriviaDTO dto = MapeadorTrivia.pregunta(trivia());

        assertEquals(3, dto.ronda());
        assertEquals(VENCE_EN + GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA * 1000L, dto.venceEn());
    }

    @Test
    public void deberiaArmarElEstadoConLosParticipantesYSusTiempos() {
        Trivia trivia = trivia();
        Jugador ana = jugador(1L, "Ana");
        trivia.getParticipantes().add(ana);
        trivia.registrarTiempoRespuesta(ana, 4200L);

        EstadoTriviaDTO estado = MapeadorTrivia.estado("ABC123", trivia);

        assertEquals("ABC123", estado.codigoPartida());
        assertEquals(3, estado.ronda());
        assertEquals("DIFICIL", estado.dificultad());
        assertFalse(estado.finalizada());
        assertEquals("Ana", estado.enJuego().get(0).apodo());
        assertEquals(4200L, estado.enJuego().get(0).milisegundos());
        assertNull(estado.ganador());
        assertFalse(estado.premioPendiente());
    }

    @Test
    public void conGanadorYPremioSinEntregarDeberiaIndicarQueElPremioEstaPendiente() {
        Trivia trivia = trivia();
        trivia.declararGanador(jugador(1L, "Ana"));
        trivia.finalizar();

        EstadoTriviaDTO estado = MapeadorTrivia.estado("ABC123", trivia);

        assertEquals(1L, estado.idGanador());
        assertEquals("Ana", estado.ganador());
        assertTrue(estado.premioPendiente());
        assertNull(estado.venceEn());

        trivia.registrarPremioEntregado();
        assertFalse(MapeadorTrivia.estado("ABC123", trivia).premioPendiente());
    }

    @Test
    public void elEstadoNuncaDeberiaIncluirLaRespuestaCorrecta() throws Exception {
        String json = new ObjectMapper().writeValueAsString(MapeadorTrivia.estado("ABC123", trivia()));

        assertFalse(json.contains("correcta"));
        assertFalse(json.contains("San Justo"));
    }

    @Test
    public void unaTriviaSinDificultadNiInicioNoDeberiaFallar() {
        EstadoTriviaDTO estado = MapeadorTrivia.estado("ABC123", new Trivia());

        assertNull(estado.dificultad());
        assertNull(estado.venceEn());
    }
}
