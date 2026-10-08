package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.excepcion.UsuarioYaEstaEnPartidaException;
import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;

//@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioSalaDeEspera {

    Partida crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico, SalaDeEspera sala)
            throws UsuarioNoEncontradoException, PartidaNoEncontradaException, SalaDeEsperaLlenaException, PartidaIniciadaException, UsuarioYaEstaEnPartidaException;

    SalaActualizadaDTO obtenerEstadoSala(SalaDeEspera salaDeEspera) throws UsuarioNoEncontradoException;
}
