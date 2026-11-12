package com.tallerwebi.presentacion.DTO;

/**
 * Resultado que recibe el jugador al responder. La opción correcta no se informa acá:
 * se envía a todos por WebSocket cuando se cierra la ronda.
 */
public record ResultadoRespuestaTriviaDTO(Boolean correcta) {
}
