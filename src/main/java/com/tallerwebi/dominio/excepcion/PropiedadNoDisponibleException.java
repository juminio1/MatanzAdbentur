package com.tallerwebi.dominio.excepcion;

public class PropiedadNoDisponibleException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public PropiedadNoDisponibleException(String message) {
        super(message);
    }
}
