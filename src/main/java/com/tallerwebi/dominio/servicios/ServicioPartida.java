package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;

@FunctionalInterface
public interface ServicioPartida {

    Partida obtenerPartida(String codigoUnico);//da la info del tablero y sus posiciones
    }