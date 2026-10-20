package com.tallerwebi.dominio.eventos;

/** Estado del turno que reciben los observadores de una partida. */
public record EventoTurnoCambiado(
    String codigoPartida,
    Long idJugadorActual,
    String apodoJugadorActual,
    Integer orden,
    Integer duracionSegundos,
    Integer rondaActual
) {}
