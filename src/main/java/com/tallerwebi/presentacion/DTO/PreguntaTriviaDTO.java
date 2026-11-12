package com.tallerwebi.presentacion.DTO;

import java.util.List;

/**
 * Pregunta de la ronda tal como la ve el jugador.
 * No incluye la respuesta correcta: esa queda solo en el backend.
 */
public record PreguntaTriviaDTO(Integer ronda, String enunciado, List<String> opciones,
        String dificultad, Long venceEn) {
}
