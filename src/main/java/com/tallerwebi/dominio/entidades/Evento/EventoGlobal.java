package com.tallerwebi.dominio.entidades.Evento;

import com.tallerwebi.dominio.enums.TipoEfectoEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Especialización JOINED: su identificador es la clave foránea de Evento. */
@Entity
public class EventoGlobal extends Evento {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEfectoEventoGlobal tipoEfecto;

    @Column(nullable = false)
    private Integer valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoEventoGlobal tipoEventoGlobal;

    public TipoEfectoEventoGlobal getTipoEfecto() {
        return tipoEfecto;
    }

    public void setTipoEfecto(TipoEfectoEventoGlobal tipoEfecto) {
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

    public void setTipoEventoGlobal(TipoEventoGlobal tipoEventoGlobal) {
        this.tipoEventoGlobal = tipoEventoGlobal;
    }
}
