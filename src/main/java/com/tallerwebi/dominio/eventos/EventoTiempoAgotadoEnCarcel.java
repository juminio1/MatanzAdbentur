package com.tallerwebi.dominio.eventos;

/**
 * Se publica cuando a un jugador preso se le acaba el tiempo del turno sin tirar.
 * El turno cuenta como intento fallido; si era el último, se le cobra la fianza y queda liberado.
 */
public record EventoTiempoAgotadoEnCarcel(
    String codigoPartida,
    Long idJugador,
    Boolean encarcelado,
    Integer intentosCarcel,
    Integer dinero
) {}
