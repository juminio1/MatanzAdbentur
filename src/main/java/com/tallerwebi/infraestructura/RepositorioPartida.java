package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Jugador;

public interface RepositorioPartida {
    void guardarPartida(Partida partida);
    void actualizarJugador(Jugador jugador);
    Jugador buscarJugadorPorId(Long idJugador);

    Partida buscarPartidaActiva();

    Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico);
}
