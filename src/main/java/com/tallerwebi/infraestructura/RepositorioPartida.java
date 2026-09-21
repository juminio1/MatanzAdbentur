package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Partida;

public interface RepositorioPartida {
  void guardarPartida(Partida partida);

  Partida buscarPartidaActiva();

  Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico);
}
