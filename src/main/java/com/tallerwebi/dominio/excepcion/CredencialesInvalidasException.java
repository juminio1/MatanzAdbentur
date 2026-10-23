package com.tallerwebi.dominio.excepcion;

public class CredencialesInvalidasException extends Exception {

  @java.io.Serial
  private static final long serialVersionUID = 1L;

  public CredencialesInvalidasException(String mensaje) {
    super(mensaje);
  }
}
