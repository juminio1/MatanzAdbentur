package com.tallerwebi.dominio.excepcion;

public class ContraseniaInvalidaException extends Exception {

  @java.io.Serial
  private static final long serialVersionUID = 1L;

  public ContraseniaInvalidaException(String mensaje) {
    super(mensaje);
  }
}
