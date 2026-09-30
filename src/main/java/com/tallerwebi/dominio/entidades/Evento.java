package com.tallerwebi.dominio.entidades;

import jakarta.persistence.*;

@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;
    private Integer monto;

    public Evento(Integer id, TipoEvento tipoEvento, Integer monto) {
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.monto = monto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }
}
