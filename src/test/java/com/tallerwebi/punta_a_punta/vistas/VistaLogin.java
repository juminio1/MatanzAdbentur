package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;

public class VistaLogin extends VistaWeb {

  public VistaLogin(Page page) {
    super(page);
    page.navigate("http://localhost:8080/spring/login");
  }

  public String obtenerTituloLogin() {
    return this.obtenerTextoDelElemento(".login-form-title h2").trim();
  }

  public String obtenerMensajeDeError() {
    return this.obtenerTextoDelElemento(".login-error span:last-child").trim();
  }

  public void escribirEMAIL(String email) {
    this.escribirEnElElemento("#credencial", email);
  }

  public void escribirClave(String clave) {
    this.escribirEnElElemento("#password", clave);
  }

  public void darClickEnIniciarSesion() {
    this.darClickEnElElemento("#btn-login");
  }

  public void darClickEnRegistrarse() {
    this.darClickEnElElemento("#btn-register");
  }
}
