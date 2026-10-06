package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;

public interface ServicioPartida {
    Partida obtenerPartida(String codigoUnico); //da la info del tablero y sus posiciones
    void iniciarPartida()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException;
}
