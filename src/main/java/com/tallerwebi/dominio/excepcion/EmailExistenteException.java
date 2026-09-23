package com.tallerwebi.dominio.excepcion;

public class EmailExistenteException extends UsuarioExistenteException {

  public EmailExistenteException() {
    super("El email ya se encuentra registrado");
  }
}
