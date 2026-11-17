package com.tallerwebi.dominio.excepcion;

/** La acción no se puede hacer en el estado actual de la trivia (ya respondió, venció el tiempo, etc.). */
public class AccionTriviaInvalidaException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public AccionTriviaInvalidaException(String message) {
        super(message);
    }
}
