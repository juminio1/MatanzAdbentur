package com.tallerwebi.dominio.excepcion;

public class UsuarioYaEstaEnPartidaException extends Exception {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public UsuarioYaEstaEnPartidaException() {
        super("El usuario ya está en la partida. No puede unirse nuevamente.");
    }
}
