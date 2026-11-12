package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Jugador;

public interface RepositorioPartida {
    void bloquearPartida(Partida partida);
    void guardarPartida(Partida partida);
    void actualizarJugador(Jugador jugador);
    Jugador buscarJugadorPorId(Long idJugador);
    Partida buscarPartidaPorJugador(Long idJugador);

    Partida buscarPartidaActiva();

    Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico);
    Partida buscarPartidaPorCodigoUnico(String codigoUnico);
}
