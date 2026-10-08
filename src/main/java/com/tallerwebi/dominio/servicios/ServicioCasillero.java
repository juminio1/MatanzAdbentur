package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;

import java.util.List;

public interface ServicioCasillero {
    //operaciones de admi
    Casillero buscarPorId(Long id);
    List<Casillero> buscarTodos();

    // comportamiento del juego
    boolean comprarPropiedad(Jugador jugador, Propiedad propiedad);
    void cobrarAlquiler(Jugador jugador, Propiedad propiedad);
    boolean aplicarEvento(CasilleroEvento evento, Jugador jugador);

}
