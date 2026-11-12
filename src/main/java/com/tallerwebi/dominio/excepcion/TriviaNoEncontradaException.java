package com.tallerwebi.dominio.excepcion;

/** La partida no tiene una trivia en juego. */
public class TriviaNoEncontradaException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public TriviaNoEncontradaException(String message) {
        super(message);
    }
}
