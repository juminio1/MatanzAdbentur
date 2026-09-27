package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CantidadinsuficienteDeJugadoresException;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoCreadorException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

public interface ServicioPartida {
  Partida crearPartida(Long idUsuario) throws UsuarioNoEncontradoException;

  Partida unirseAPartida(Long idUsuario, String codigoUnico)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException;

  void iniciarPartida(Long idUsuario, String codigoUnico)
    throws UsuarioNoCreadorException, CantidadinsuficienteDeJugadoresException, PartidaNoEncontradaException;

  void seleccionarFicha(Long idUsuario, String codigoUnico, Ficha ficha)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException, FichaOcupadaException;
}
