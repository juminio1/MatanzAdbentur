package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

public interface ServicioSalaDeEspera {
    Partida crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException;

    void seleccionarFicha(String codigoUnico, Long idUsuario, Ficha ficha)
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException, FichaOcupadaException;

    void abandonarSala(String codigoUnico, Long idUsuario)
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException;
}
