package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

/** Reglas compartidas de participación, independientes de HTTP y persistencia. */
public final class ReglasParticipacion {
    private ReglasParticipacion() { }
    public static boolean estaActivo(Jugador jugador) {
        return jugador != null && !Boolean.TRUE.equals(jugador.getEnBancarrota()) && !Boolean.TRUE.equals(jugador.getAbandono());
    }
    public static boolean pertenece(Partida partida, Long idJugador) {
        return partida != null && idJugador != null && partida.getJugadores() != null
                && partida.getJugadores().stream().anyMatch(j -> idJugador.equals(j.getId()));
    }
    public static boolean permiteAdministrarPropiedades(Partida partida, Jugador jugador, Long idJugador) {
        return estaActivo(jugador) && pertenece(partida, idJugador);
    }
}
