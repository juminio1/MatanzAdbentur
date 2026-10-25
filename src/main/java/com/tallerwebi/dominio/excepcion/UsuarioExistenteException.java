package com.tallerwebi.dominio.excepcion;

public class UsuarioExistenteException extends Exception {

  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  @java.io.Serial
  private static final long serialVersionUID = 1L;

  public UsuarioExistenteException(String mensaje) {
    super(mensaje);
  }
}
