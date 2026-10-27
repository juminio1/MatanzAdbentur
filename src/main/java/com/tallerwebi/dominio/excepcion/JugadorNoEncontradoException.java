package com.tallerwebi.dominio.excepcion;

public class JugadorNoEncontradoException extends Exception {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public JugadorNoEncontradoException(String mensaje) {
        super(mensaje); 
    }
}
