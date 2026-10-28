package com.tallerwebi.dominio.excepcion;

/** El jugador no puede aceptar ofertas en la partida (por ejemplo, porque quebró). */
public class OfertaNoPermitidaException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public OfertaNoPermitidaException(String message) {
        super(message);
    }
}
