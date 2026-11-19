package com.tallerwebi.dominio.eventos;

/**
 * Se publica cuando un jugador le paga a otro una deuda que había quedado pendiente
 * (por ejemplo, un alquiler que pudo pagar después de vender propiedades),
 * o cuando al quebrar le entrega al acreedor todo el dinero que le quedaba.
 */
public record EventoDeudaPagada(
    String codigoPartida,
    Long idPagador,
    String pagador,
    Integer dineroPagador,
    Long idAcreedor,
    String acreedor,
    Integer dineroAcreedor,
    Integer posicion,
    Integer monto,
    Boolean porQuiebra
) {}
