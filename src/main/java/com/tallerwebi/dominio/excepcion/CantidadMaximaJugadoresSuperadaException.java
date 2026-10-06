package com.tallerwebi.dominio.excepcion;

public class CantidadMaximaJugadoresSuperadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public CantidadMaximaJugadoresSuperadaException(String mensaje) {
        super(mensaje);
    }
}
