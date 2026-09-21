package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;

public interface ServicioLogin {
  Usuario consultarUsuario(String email);
  Usuario autenticar(String email, String password) throws CredencialesInvalidasException; // o DatosInvalidosException
   Usuario consultarUsuarioPorUsername(String username);
}
