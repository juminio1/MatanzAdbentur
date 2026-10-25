package com.tallerwebi.dominio.excepcion;

public class UsuarioNoEncontradoException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public UsuarioNoEncontradoException() {
        super("El usuario no fue encontrado.");
    }

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
