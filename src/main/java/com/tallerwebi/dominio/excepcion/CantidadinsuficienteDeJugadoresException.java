package com.tallerwebi.dominio.excepcion;

public class CantidadinsuficienteDeJugadoresException extends Exception {

  private static final long serialVersionUID = 1L;

  public CantidadinsuficienteDeJugadoresException(String message) {
    super(message);
  }
}
