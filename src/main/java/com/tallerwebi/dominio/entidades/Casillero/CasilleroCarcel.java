package com.tallerwebi.dominio.entidades.Casillero;

import jakarta.persistence.Entity;

@Entity
public class CasilleroCarcel extends Casillero {

public CasilleroCarcel() {
        super();
    }

public CasilleroCarcel(Long id, Integer posicion, String nombre, String descripcion) {
        super(id, posicion, nombre, descripcion);
    }

}
