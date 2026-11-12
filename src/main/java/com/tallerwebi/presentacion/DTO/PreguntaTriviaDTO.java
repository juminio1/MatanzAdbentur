package com.tallerwebi.presentacion.DTO;

import java.util.List;

/**
 * Pregunta de la ronda tal como la ve el jugador.
 * No incluye la respuesta correcta: esa queda solo en el backend.
 * El id es el número de ronda; el front lo devuelve al responder.
 */
public record PreguntaTriviaDTO(Integer id, Integer ronda, Integer totalRondas, String enunciado,
        List<String> opciones, String dificultad, Integer segundosParaResponder, Long venceEn) {
}
