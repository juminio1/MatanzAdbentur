package com.tallerwebi.dominio.Servicios;

import com.tallerwebi.dominio.Entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;

public interface ServicioLogin {
  Usuario consultarUsuario(String email);
  Usuario autenticar(String email, String password) throws CredencialesInvalidasException; // o DatosInvalidosException
}
