
package com.tallerwebi.dominio.eventos;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
// Avisa que un jugador quebró.
public record EventoJugadorQuebrado(
        //indica en que partida ocurrio
        Partida partida,
        //indentifica quien quebro.
        Jugador jugador) {
}
