package com.tallerwebi.dominio.excepcion;

public class ContraseniaInvalida extends Exception {

  private static final long serialVersionUID = 1L;

  public ContraseniaInvalida(String mensaje) {
    super(mensaje);
  }
}
