package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

/** Reglas compartidas de pertenencia y participación en una partida. */
public interface ServicioParticipacion {
    boolean estaActivo(Jugador jugador);
    boolean pertenece(Partida partida, Long idJugador);
    boolean permiteAdministrarPropiedades(Partida partida, Jugador jugador, Long idJugador);
}
