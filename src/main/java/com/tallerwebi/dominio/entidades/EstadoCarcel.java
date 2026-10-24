package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Embeddable;

@Embeddable
public class EstadoCarcel {
    
    private Boolean encarcelado = false;
    private Integer intentosSalirCarcel = 0;

    public EstadoCarcel() {
        // Constructor vacío requerido por Hibernate
    }

    public void encarcelar() {
        this.encarcelado = true;
        this.intentosSalirCarcel = 0;
    }

    public void liberar() {
        this.encarcelado = false;
        this.intentosSalirCarcel = 0;
    }

    public void registrarIntentoFallido() {
        this.intentosSalirCarcel++;
    }

    public Boolean getEncarcelado() {
        return encarcelado;
    }

    public void setEncarcelado(Boolean encarcelado) {
        this.encarcelado = encarcelado;
    }

    public Integer getIntentosSalirCarcel() {
        return intentosSalirCarcel;
    }

    public void setIntentosSalirCarcel(Integer intentosSalirCarcel) {
        this.intentosSalirCarcel = intentosSalirCarcel;
    }
}