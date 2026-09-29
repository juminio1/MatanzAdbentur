package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

public interface ServicioSalaDeEspera {

    Partida CrearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, PartidaNoEncontradaException;

}
