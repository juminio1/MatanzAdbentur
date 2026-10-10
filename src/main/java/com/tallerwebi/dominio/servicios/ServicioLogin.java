package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;

public interface ServicioLogin {
  Usuario consultarUsuario(String email);
  Usuario autenticar(String credencial, String password) throws CredencialesInvalidasException; // o DatosInvalidosException
  Usuario buscarPorId(Long id);
  void actualizarAvatar(Long userId, String avatarUrl);
}
