package com.tallerwebi.dominio.excepcion;

public class PropiedadNoDisponibleException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PropiedadNoDisponibleException(String message) {
        super(message);
    }
}
