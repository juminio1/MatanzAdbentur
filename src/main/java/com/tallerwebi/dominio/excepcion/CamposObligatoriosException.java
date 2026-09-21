package com.tallerwebi.dominio.excepcion;

public class CamposObligatoriosException extends Exception {

  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  private static final long serialVersionUID = 1L;

  public CamposObligatoriosException(String mensaje) {
    super(mensaje);
  }
}
