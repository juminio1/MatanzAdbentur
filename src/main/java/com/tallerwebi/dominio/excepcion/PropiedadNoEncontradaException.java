package com.tallerwebi.dominio.excepcion;

public class PropiedadNoEncontradaException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public PropiedadNoEncontradaException(String message) {
        super(message);
    }

}
