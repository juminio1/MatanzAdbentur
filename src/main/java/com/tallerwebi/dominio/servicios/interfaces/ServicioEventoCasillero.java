package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.resultados.ResultadoEvento;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;

import java.util.Optional;

@FunctionalInterface
public interface ServicioEventoCasillero {
    Optional<ResultadoEvento> aplicarSiCorresponde(
            String codigoPartida,
            Jugador jugador) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
}
