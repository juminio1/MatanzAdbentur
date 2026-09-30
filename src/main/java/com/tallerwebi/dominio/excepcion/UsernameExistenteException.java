package com.tallerwebi.dominio.excepcion;

public class UsernameExistenteException extends UsuarioExistenteException {

  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  private static final long serialVersionUID = 1L;

  public UsernameExistenteException() {
    super("El username ya se encuentra registrado");
  }
}
