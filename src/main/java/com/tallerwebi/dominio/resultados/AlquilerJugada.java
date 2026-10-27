package com.tallerwebi.dominio.resultados;

public record AlquilerJugada(Long idJugadorPagador, String pagador, String propietario,
                                    String propiedad, Integer monto) {}
