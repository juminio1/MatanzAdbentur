package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;

public interface ServicioSalaDeEspera {

    SalaDeEspera crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    SalaDeEspera unirseASalaDeEspera(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException;

    void abandonarSala(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, SalaNoEncontradaException;

    SalaDeEspera obtenerSalaPorCodigo(String codigoUnico) throws SalaNoEncontradaException;
}
