package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

public interface ServicioPartida {
  Partida crearPartida(Long idUsuario) throws UsuarioNoEncontradoException;

  Partida unirseAPartida(Long idUsuario, String codigoUnico)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException;

}
