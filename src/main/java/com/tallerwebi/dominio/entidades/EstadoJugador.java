package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Embeddable;

@Embeddable
public class EstadoJugador {

    private Boolean tiroEnTurno = false;
    private Boolean pierdeTurno = false;
    // Permite acumular penalizaciones de casillero y eventos globales.
    private Integer turnosPenalizadosPendientes = 0;
    private Boolean enBancarrota = false;
    private Boolean abandono = false;
    private Integer ultimaRondaConOportunidad = 0;

    public Integer getUltimaRondaConOportunidad() {
        return ultimaRondaConOportunidad;
    }

    public void setUltimaRondaConOportunidad(Integer ultimaRondaConOportunidad) {
        this.ultimaRondaConOportunidad = ultimaRondaConOportunidad;
    }

    public EstadoJugador() {
        // Constructor vacío requerido por Hibernate
    }

    public Boolean getTiroEnTurno() {
        return tiroEnTurno;
    }

    public void setTiroEnTurno(Boolean tiroEnTurno) {
        this.tiroEnTurno = tiroEnTurno;
    }

    public Boolean getPierdeTurno() {
        return Boolean.TRUE.equals(pierdeTurno) || pendientes() > 0;
    }

    // true agrega una penalización; false consume exactamente una oportunidad.
    public void setPierdeTurno(Boolean valor) {
        int cantidad = Math.max(pendientes(), Boolean.TRUE.equals(pierdeTurno) ? 1 : 0);
        turnosPenalizadosPendientes = Boolean.TRUE.equals(valor)
                ? cantidad + 1 : Math.max(0, cantidad - 1);
        pierdeTurno = turnosPenalizadosPendientes > 0;
    }

    public void limpiarTurnosPenalizados() {
        turnosPenalizadosPendientes = 0;
        pierdeTurno = false;
    }

    private int pendientes() {
        return turnosPenalizadosPendientes == null ? 0 : turnosPenalizadosPendientes;
    }

    public Boolean getEnBancarrota() {
        return enBancarrota;
    }

    public void setEnBancarrota(Boolean enBancarrota) {
        this.enBancarrota = enBancarrota;
    }

    public Boolean getAbandono() {
        return abandono;
    }

    public void setAbandono(Boolean abandono) {
        this.abandono = abandono;
    }
}
