package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.TipoEvento;
import jakarta.persistence.*;

@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;
    private Integer monto;

    public Evento(Long id, TipoEvento tipoEvento, Integer monto) {
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.monto = monto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
