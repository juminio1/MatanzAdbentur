package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Embeddable;

@Embeddable
public class EstadoJugador {

    private Boolean tiroEnTurno = false;
    private Boolean pierdeTurno = false;
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
        return pierdeTurno;
    }

    public void setPierdeTurno(Boolean pierdeTurno) {
        this.pierdeTurno = pierdeTurno;
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
