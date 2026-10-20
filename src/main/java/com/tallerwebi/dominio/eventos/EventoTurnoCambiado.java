package com.tallerwebi.dominio.eventos;

public record EventoTurnoCambiado(
    String codigoPartida,
    Long idJugadorActual,
    String apodoJugadorActual,
    Integer orden,
    Integer duracionSegundos,
    Integer rondaActual
) {}
