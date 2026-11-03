package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

/** Reglas compartidas de pertenencia y participación en una partida. */
public interface ServicioParticipacion {
    void validarPartidaEnCurso(Partida partida);

    default void validarAccionOrdinaria(Partida partida) {
        validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo()) {
            throw new IllegalStateException("La partida está pausada por un evento global.");
        }
    }

    default void validarCompraEnPartida(Partida partida)
            throws com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException {
        validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo()) {
            throw new com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException(
                    "La partida está pausada por un evento global.");
        }
    }
    boolean estaActivo(Jugador jugador);
    boolean pertenece(Partida partida, Long idJugador);
    boolean permiteAdministrarPropiedades(Partida partida, Jugador jugador, Long idJugador);
}
