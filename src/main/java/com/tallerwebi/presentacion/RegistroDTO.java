package com.tallerwebi.presentacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroDTO {

  @NotBlank(message = "El username es obligatorio")
  @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
  @Pattern(
    regexp = "^[\\p{L}\\p{N}_ ]+$",
    message = "El username solo puede contener letras, números, espacios y guiones bajos"
  )
  private String username;

  @NotBlank(message = "El email es obligatorio")
  @Email(message = "El email no tiene un formato válido")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
  @Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
    message = "Debe contener una mayúscula, una minúscula, un número y un carácter especial"
  )
  private String password;

  @NotBlank(message = "Debe repetir la contraseña")
  private String passwordRepetido;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPasswordRepetido() {
    return passwordRepetido;
  }

  public void setPasswordRepetido(String passwordRepetido) {
    this.passwordRepetido = passwordRepetido;
  }
}
