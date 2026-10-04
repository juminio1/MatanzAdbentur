package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.TipoCasillero;
import jakarta.persistence.*;

public class Casillero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer posicion;

    @Enumerated(EnumType.STRING)
    private TipoCasillero tipo;

    public Casillero() {
    }

    public Casillero(Integer posicion, TipoCasillero tipo, String descripcion) {
        this.posicion = posicion;
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPosicion() {
        return posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    public TipoCasillero getTipo() {
        return tipo;
    }

    public void setTipo(TipoCasillero tipo) {
        this.tipo = tipo;
    }

}
