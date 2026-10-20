package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.excepcion.*;

public interface ServicioSalaDeEspera {


    SalaDeEspera crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException;

    SalaDeEspera unirseASalaDeEspera(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException;

    void abandonarSala(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException;

    SalaDeEspera obtenerSalaPorCodigo(String codigoUnico) throws SalaNoEncontradaException;

    void seleccionarFicha(String codigoUnico, Long idUsuario, ColorFicha ficha)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException, FichaOcupadaException;
}
