package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import com.tallerwebi.presentacion.mapeadores.MapeadorTrivia;
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
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, VENCE_EN);

        assertEquals("¿Cuál es la cabecera del partido de La Matanza?", dto.enunciado());
        assertEquals(List.of("Ramos Mejía", "San Justo", "Morón", "Haedo"), dto.opciones());
        assertEquals("DIFICIL", dto.dificultad());
    }

    @Test
    public void deberiaIncluirLaRondaYElVencimientoDeLaPregunta() {
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, VENCE_EN);

        assertEquals(2, dto.ronda());
        assertEquals(VENCE_EN, dto.venceEn());
    }

    @Test
    public void nuncaDeberiaEnviarLaRespuestaCorrectaAlNavegador() throws Exception {
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta(), 2, VENCE_EN);

        String json = new ObjectMapper().writeValueAsString(dto);

        assertFalse(json.contains("respuestaCorrecta"));
        assertFalse(json.contains("correcta"));
        assertTrue(json.contains("San Justo"));
    }

    @Test
    public void noDeberiaVerseAfectadoSiLuegoSeModificanLasOpcionesDeLaPregunta() {
        PreguntaGenerada pregunta = pregunta();
        PreguntaTriviaDTO dto = MapeadorTrivia.desdePregunta(pregunta, 2, VENCE_EN);

        pregunta.getOpciones().set(0, "Opción cambiada");

        assertEquals("Ramos Mejía", dto.opciones().get(0));
    }

    @Test
    public void deberiaRechazarUnaPreguntaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> MapeadorTrivia.desdePregunta(null, 1, VENCE_EN));
    }
}
