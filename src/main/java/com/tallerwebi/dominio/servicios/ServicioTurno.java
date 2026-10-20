package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

public interface ServicioTurno {
    Jugador obtenerJugadorActual(Partida partida);
    void incrementarRondas(Partida partida);
    Integer obtenerIndiceTurnoActual(Partida partida);
    Jugador resolverSiguienteJugador(Partida partida, Integer indiceActual);
    void asignarOrdenInicialJugadores(Partida partida);
}
