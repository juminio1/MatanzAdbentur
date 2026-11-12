package com.tallerwebi.presentacion.DTO;

/**
 * Respuesta que envía un jugador: la ronda que está contestando y el índice (0 a 3) de la opción elegida.
 * La ronda permite rechazar respuestas a una pregunta que ya no es la actual.
 */
public record RespuestaTriviaDTO(Integer ronda, Integer opcionElegida) {
}
