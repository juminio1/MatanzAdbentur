package com.tallerwebi.dominio.excepcion;

public class ActivacionEventoInvalidoException extends Exception{

    private static final long serialVersionUID = 1L;

  public ActivacionEventoInvalidoException(String mensaje) {
    super(mensaje);
  }

}
