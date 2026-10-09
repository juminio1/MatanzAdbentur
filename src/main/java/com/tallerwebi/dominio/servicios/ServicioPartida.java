package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

public interface ServicioPartida {

    Partida obtenerPartida(String codigoUnico);//da la info del tablero y sus posiciones
    void restarDinero (Jugador jugador, Integer monto);
    void sumarDinero (Jugador jugador, Integer monto);
    void inicializarTurnos(String codigoUnico);
    Jugador obtenerJugadorActual(String codigoUnico);
    void avanzarTurno(String codigoUnico);

}