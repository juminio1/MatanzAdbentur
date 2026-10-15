package com.tallerwebi.dominio.excepcion;

public class PropiedadNoMejorableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PropiedadNoMejorableException(String message) {
        super(message);
    }
}
