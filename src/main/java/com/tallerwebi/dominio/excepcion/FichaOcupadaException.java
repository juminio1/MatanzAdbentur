package com.tallerwebi.dominio.excepcion;

public class FichaOcupadaException extends Exception {

  private static final long serialVersionUID = 1L;

  public FichaOcupadaException() {
    super("La ficha seleccionada ya está ocupada por otro jugador.");
  }
}
