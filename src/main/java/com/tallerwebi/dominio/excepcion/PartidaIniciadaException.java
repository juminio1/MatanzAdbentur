package com.tallerwebi.dominio.excepcion;

public class PartidaIniciadaException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public PartidaIniciadaException() {
            super("La partida ya ha iniciado. No es posible unirse.");
        }
}
