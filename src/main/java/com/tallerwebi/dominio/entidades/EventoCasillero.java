package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class EventoCasillero extends Evento {
    @Enumerated(EnumType.STRING)
    private TipoEfectoEvento tipoEfecto;

    private Integer monto;
    private Integer cantidadCasilleros;
    private Integer posicionDestino;

    public EventoCasillero() {
    }

    public EventoCasillero(Integer id, String nombre, String descripcion, TipoEfectoEvento tipoEfecto,
                           Integer monto, Integer cantidadCasilleros, Integer posicionDestino) {
        super(id, nombre, descripcion);
        this.tipoEfecto = tipoEfecto;
        this.monto = monto;
        this.cantidadCasilleros = cantidadCasilleros;
        this.posicionDestino = posicionDestino;
    }

    public TipoEfectoEvento getTipoEfecto() {
        return tipoEfecto;
    }

    public void setTipoEfecto(TipoEfectoEvento tipoEfecto) {
        this.tipoEfecto = tipoEfecto;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public Integer getCantidadCasilleros() {
        return cantidadCasilleros;
    }

    public void setCantidadCasilleros(Integer cantidadCasilleros) {
        this.cantidadCasilleros = cantidadCasilleros;
    }

    public Integer getPosicionDestino() {
        return posicionDestino;
    }

    public void setPosicionDestino(Integer posicionDestino) {
        this.posicionDestino = posicionDestino;
    }
}
