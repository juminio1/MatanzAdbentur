package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Embeddable;

/** Conserva las columnas y reglas de rondas existentes en Partida. */
@Embeddable
public class EstadoRondas {
    private Integer rondaActual = 1;
    private Integer turnosFinalizadosEnRonda = 0;

    public Integer getRondaActual() { return rondaActual; }
    public void setRondaActual(Integer rondaActual) { this.rondaActual = rondaActual; }
    public Integer getTurnosFinalizadosEnRonda() { return turnosFinalizadosEnRonda; }
    public void setTurnosFinalizadosEnRonda(Integer turnosFinalizadosEnRonda) {
        this.turnosFinalizadosEnRonda = turnosFinalizadosEnRonda;
    }
}
