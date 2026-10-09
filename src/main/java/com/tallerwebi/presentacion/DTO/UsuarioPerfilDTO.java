package com.tallerwebi.presentacion.DTO;

public class UsuarioPerfilDTO {

  private String username;
  private String rol;
  private String avatar;

  public UsuarioPerfilDTO() {
  }

  public UsuarioPerfilDTO(String username, String rol, String avatar) {
    this.username = username;
    this.rol = rol;
    this.avatar = avatar;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRol() {
    return rol;
  }

  public void setRol(String rol) {
    this.rol = rol;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }
}