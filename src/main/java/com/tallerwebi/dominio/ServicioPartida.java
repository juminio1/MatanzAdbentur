package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioPartida {
  Partida crearPartida(Long idUsuario) throws UsuarioNoEncontradoException;
}
