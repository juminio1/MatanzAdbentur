package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Partida;

public interface RepositorioPartida {
  void guardarPartida(Partida partida);

    Partida buscarPartidaActiva();

    Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico);

}
