package com.tallerwebi.dominio.excepcion;

public class CamposObligatoriosException extends Exception {

  private static final long serialVersionUID = 1L;

  public CamposObligatoriosException(String mensaje) {
    super(mensaje);
  }
}