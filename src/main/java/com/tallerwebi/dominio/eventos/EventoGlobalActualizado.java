package com.tallerwebi.dominio.eventos;

import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;

/** Notifica al frontend un cambio de estado del evento global. */
public record EventoGlobalActualizado(EstadoEventoGlobal estado) {
}
