package com.tallerwebi.dominio.excepcion;

public class EventoInexistenteException extends  Exception{

    private static final long serialVersionUID = 1L;
    public EventoInexistenteException(String message) {
        super(message);
    }

}
