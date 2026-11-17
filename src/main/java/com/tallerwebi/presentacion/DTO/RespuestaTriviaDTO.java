package com.tallerwebi.presentacion.DTO;

/**
 * Respuesta que envía un jugador: el id de la pregunta (número de ronda) y el índice (0 a 3) de la opción.
 * El id permite rechazar respuestas a una pregunta que ya no está en juego.
 */
public record RespuestaTriviaDTO(Integer id, Integer opcion) {
}
