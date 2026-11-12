package com.tallerwebi.dominio.excepcion;

/** El jugador no puede participar de la trivia: no es de la partida, está preso o fue eliminado. */
public class ParticipanteTriviaNoHabilitadoException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public ParticipanteTriviaNoHabilitadoException(String message) {
        super(message);
    }
}
