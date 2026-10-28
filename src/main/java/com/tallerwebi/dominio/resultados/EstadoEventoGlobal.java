package com.tallerwebi.dominio.resultados;

import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import java.util.List;
import java.util.Map;

/** Estado visible del evento global, sin exponer entidades JPA. */
public record EstadoEventoGlobal(
        String codigoPartida,
        String fase,
        TipoEventoGlobal tipo,
        String descripcion,
        Integer etapa,
        Integer segundosRestantes,
        List<Long> participantes,
        Map<Long, Integer> tiradas,
        Long idJugadorElegido,
        String detalle,
        Map<Long, Integer> posiciones,
        Map<Long, Integer> saldos) {

    // Conserva las llamadas existentes al constructor original de nueve argumentos.
    public EstadoEventoGlobal(String codigoPartida, String fase, TipoEventoGlobal tipo, String descripcion, Integer etapa, Integer segundosRestantes, List<Long> participantes, Map<Long, Integer> tiradas, Long idJugadorElegido) {
        this(codigoPartida, fase, tipo, descripcion, etapa, segundosRestantes, participantes, tiradas, idJugadorElegido,
                null, Map.of(), Map.of());
    }
}

