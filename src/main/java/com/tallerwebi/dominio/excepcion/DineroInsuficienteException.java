package com.tallerwebi.dominio.excepcion;

public class DineroInsuficienteException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DineroInsuficienteException(String message) {
        super(message);
    }
}
