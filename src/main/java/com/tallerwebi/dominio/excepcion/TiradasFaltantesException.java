package com.tallerwebi.dominio.excepcion;

public class TiradasFaltantesException extends Exception{
 private static final long serialVersionUID = 1L;
    public TiradasFaltantesException(String message) {
        super(message);
    }
}