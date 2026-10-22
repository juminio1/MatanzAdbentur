
package com.tallerwebi.dominio.eventos;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
// representa un acontecimiento dentro del juego. Un jugador se declaro en quiebra.
public record EventoJugadorQuebrado(
        //indica en que partida ocurrio
        Partida partida,
        //indentifica quien quebro.
        Jugador jugador) {
}
