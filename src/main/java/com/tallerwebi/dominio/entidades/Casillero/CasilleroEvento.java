package com.tallerwebi.dominio.entidades.Casillero;

import com.tallerwebi.dominio.enums.TipoEvento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class CasilleroEvento extends Casillero{

    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    private Integer monto;

    public CasilleroEvento(Long id, Integer posicion, String nombre, String descripcion, TipoEvento evento, Integer monto) {
        super(id, posicion, nombre, descripcion);
        this.tipoEvento = evento;
        this.monto = monto;
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
