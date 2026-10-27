package com.tallerwebi.dominio.resultados;

import com.tallerwebi.dominio.entidades.EventoCasillero;

/** Evento aplicado y premio por inicio acreditado durante su movimiento. */
public record ResultadoEvento(EventoCasillero evento, int dineroPorInicio) { }
