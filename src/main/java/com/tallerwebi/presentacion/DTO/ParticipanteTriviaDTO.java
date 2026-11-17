package com.tallerwebi.presentacion.DTO;

/** Jugador que sigue en la trivia, con el tiempo que lleva acumulado al responder. */
public record ParticipanteTriviaDTO(Long id, String apodo, Long milisegundos) {
}
