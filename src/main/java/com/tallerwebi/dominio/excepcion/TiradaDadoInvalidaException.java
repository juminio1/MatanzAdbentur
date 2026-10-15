package com.tallerwebi.dominio.excepcion;

public class TiradaDadoInvalidaException extends Exception {

    private static final long serialVersionUID = 1L;

    public TiradaDadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
