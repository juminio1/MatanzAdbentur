package com.tallerwebi.dominio.resultados;

import com.tallerwebi.dominio.enums.EstadoPartida;
import java.util.List;

public record ResultadosFinalesPartida(String codigoPartida, EstadoPartida estado,
        List<ResultadoFinalJugador> resultados) {
}
