package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;

public interface ServicioLogin {
  Usuario consultarUsuario(String email);
  Usuario autenticar(String email, String password) throws CredencialesInvalidasException; // o DatosInvalidosException
}
