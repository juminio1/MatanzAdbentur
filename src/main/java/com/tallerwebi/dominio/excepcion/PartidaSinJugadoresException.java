package com.tallerwebi.dominio.excepcion;

public class PartidaSinJugadoresException extends Exception{
    
    private static final long serialVersionUID = 1L;

    public PartidaSinJugadoresException(String message) {
        super(message);
    }


}
