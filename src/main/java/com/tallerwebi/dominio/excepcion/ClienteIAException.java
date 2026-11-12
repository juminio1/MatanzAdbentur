package com.tallerwebi.dominio.excepcion;

public class ClienteIAException extends RuntimeException{

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public ClienteIAException(String mensaje) {
        super(mensaje);
    }

    public ClienteIAException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

}
