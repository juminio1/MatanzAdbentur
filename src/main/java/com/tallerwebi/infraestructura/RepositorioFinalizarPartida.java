package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.FinalizarPartida;
import java.util.List;

public interface RepositorioFinalizarPartida {
    void guardar(FinalizarPartida resultado);
    FinalizarPartida buscarPorPartidaYJugador(Long idPartida, Long idJugador);
    List<FinalizarPartida> buscarPorPartida(Long idPartida);
}
