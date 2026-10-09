package com.tallerwebi.dominio.excepcion;

public class SalaNoEncontradaException extends Exception {

    private static final long serialVersionUID = 1L;

    public SalaNoEncontradaException() {
        super("La sala de espera solicitada no existe");
    }

    public SalaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
