package com.tallerwebi.dominio.excepcion;

/** La oferta tiene datos inválidos: participantes o precio. */
public class OfertaInvalidaException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public OfertaInvalidaException(String message) {
        super(message);
    }
}
