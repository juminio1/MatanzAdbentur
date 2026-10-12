package com.tallerwebi.dominio.excepcion;

public class EmailExistenteException extends UsuarioExistenteException {

  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  private static final long serialVersionUID = 1L;

  public EmailExistenteException() {
    super("El email ya se encuentra registrado");
  }
}
