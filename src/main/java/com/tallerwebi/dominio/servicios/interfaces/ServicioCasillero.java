package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Partida;

import java.util.List;

public interface ServicioCasillero {
    List<Casillero> obtenerTodosLosCasilleros();
    void guardarCasillero(Casillero casillero);
    CasilleroPropiedad buscarCasilleroPropiedad(Partida partida, Integer posicion);
    List<CasilleroPropiedad> obtenerCasillerosPropiedadDePartida(Partida partida);
    Boolean existePosicionDePropiedad(Partida partida, Integer posicion);
    List<Integer> obtenerPosicionesPropiedad();
    CasilleroPropiedad buscarOCrearPropiedadEnPosicion(Partida partida, Integer posicion);
    Integer obtenerAlquilerBaseCatalogo(Integer posicion);
    List<String> obtenerDatosCatalogoPropiedad(Integer posicion);
    Boolean esCasilleroCarcel(Integer posicion);
}
