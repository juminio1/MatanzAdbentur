package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Embeddable;

@Embeddable
public class EstadoCarcel {

    public static final int MAX_INTENTOS = 3;
    public static final int COSTO_FIANZA = 100;

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

    // Suma un intento fallido. Devuelve true si ya usó todos los intentos y hay que cobrarle la fianza.
    public boolean registrarIntentoFallido() {
        this.intentosSalirCarcel = (this.intentosSalirCarcel == null ? 0 : this.intentosSalirCarcel) + 1;
        return this.intentosSalirCarcel >= MAX_INTENTOS;
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
