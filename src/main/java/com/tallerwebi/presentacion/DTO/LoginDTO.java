package com.tallerwebi.presentacion.DTO;

public class LoginDTO {

  private String credencial;
  private String password;

  public LoginDTO() {}

  public LoginDTO(String credencial, String password) {
    this.credencial = credencial;
    this.password = password;
  }

  public String getCredencial() {
    return credencial;
  }

  public void setCredencial(String credencial) {
    this.credencial = credencial;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
