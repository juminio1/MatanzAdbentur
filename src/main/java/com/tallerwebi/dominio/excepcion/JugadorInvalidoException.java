package com.tallerwebi.dominio.excepcion;

public class JugadorInvalidoException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public JugadorInvalidoException(String message) {
        super(message);
    }
}
