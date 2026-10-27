package com.tallerwebi.dominio.excepcion;

public class CantidadMaximaJugadoresSuperadaException extends Exception {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public CantidadMaximaJugadoresSuperadaException(String mensaje) {
        super(mensaje);
    }
}
