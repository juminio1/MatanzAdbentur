package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;

import java.util.List;

public interface ServicioCasillero {
    List<Casillero> obtenerTodosLosCasilleros();
    void guardarCasillero(Casillero casillero);
}
