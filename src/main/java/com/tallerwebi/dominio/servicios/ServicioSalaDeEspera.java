package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.excepcion.UsuarioYaEstaEnPartidaException;
import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;

public interface ServicioSalaDeEspera {

    SalaDeEspera crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;
    
    SalaDeEspera unirseASalaDeEspera(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException;

    void abandonarSala(Long idUsuario, String codigoUnico)
            throws UsuarioNoEncontradoException, SalaNoEncontradaException;

    SalaDeEspera obtenerSalaPorCodigo(String codigoUnico) throws SalaNoEncontradaException;
}
