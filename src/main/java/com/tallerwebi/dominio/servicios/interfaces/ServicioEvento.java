package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Partida;
import java.util.Map;

/** Coordina los eventos globales y su pausa de la partida. */
public interface ServicioEvento {
    boolean activarSiCorresponde(Partida partida);
    Map<String, Object> consultar(String codigo, Long jugador);
    Map<String, Object> tirar(String codigo, Long jugador, int ronda, int etapa);
    Map<String, Object> confirmar(String codigo, Long jugador, int ronda);
    void participantesCambiaron(Partida partida);
}
