package com.tallerwebi.dominio.entidades.Evento;

import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "EventoGlobal")
public class EventoGlobal extends Evento {

    @Enumerated(EnumType.STRING)
    private TipoEfecto tipoEfecto;

    private Integer valor;

    @Enumerated(EnumType.STRING)
    private TipoEventoGlobal tipoEventoGlobal;

    public TipoEfecto getTipoEfecto() {
        return tipoEfecto;
    }

    public void setTipoEfecto(TipoEfecto tipoEfecto) {
        this.tipoEfecto = tipoEfecto;
    }

    public Integer getValor() {
        return valor;
    }

    public void setValor(Integer valor) {
        this.valor = valor;
    }

    public TipoEventoGlobal getTipoEventoGlobal() {
        return tipoEventoGlobal;
    }

    public void setTipoEventoGlobal(
            TipoEventoGlobal tipoEventoGlobal) {
        this.tipoEventoGlobal = tipoEventoGlobal;
    }
}

    

