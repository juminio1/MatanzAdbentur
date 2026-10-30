package com.tallerwebi.dominio.excepcion;

/** La oferta no existe, venció, ya se respondió o no está dirigida a ese jugador. */
public class OfertaNoDisponibleException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public OfertaNoDisponibleException(String message) {
        super(message);
    }
}
