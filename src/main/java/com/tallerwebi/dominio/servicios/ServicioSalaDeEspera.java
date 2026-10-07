package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

//@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioSalaDeEspera {

    Partida crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, PartidaNoEncontradaException, SalaDeEsperaLlenaException;

}
