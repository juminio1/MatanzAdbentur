package com.tallerwebi.dominio.excepcion;

public class JugadorNoEncontradoException extends Exception {

    private static final long serialVersionUID = 1L;

    public JugadorNoEncontradoException(String mensaje) {
        super("No se encontró el jugador en esta partida");
    }
}
