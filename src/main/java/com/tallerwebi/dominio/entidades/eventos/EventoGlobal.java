package com.tallerwebi.dominio.entidades.eventos;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity 
public class EventoGlobal extends Evento {
   
    @Enumerated(EnumType.STRING)
    private TipoEventoGlobal tipoEventoGlobal;

    public TipoEventoGlobal getTipoEventoGlobal() {
        return tipoEventoGlobal;
    }

    public void setTipoEventoGlobal(
            TipoEventoGlobal tipoEventoGlobal) {
        this.tipoEventoGlobal = tipoEventoGlobal;
    }
}

    

