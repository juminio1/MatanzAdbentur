package com.tallerwebi.dominio.resultados;

import com.tallerwebi.dominio.enums.EstadoParticipante;

public record ResultadoFinalJugador(Long idJugador, String apodo,
        EstadoParticipante estadoParticipante, Boolean ganador, Integer dineroFinal,
        Integer cantidadPropiedades, Integer cantidadMejoras, Integer valorPropiedades,
        Integer deudaPendiente, Integer patrimonioTotal) {
}
