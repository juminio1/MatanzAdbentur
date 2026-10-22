package com.tallerwebi.dominio.excepcion;

@SuppressWarnings("serial")
public class EventoCasilleroNoEncontradoException extends RuntimeException {
    public EventoCasilleroNoEncontradoException() {
        super("No hay eventos de casillero cargados en la base de datos.");
    }
}
