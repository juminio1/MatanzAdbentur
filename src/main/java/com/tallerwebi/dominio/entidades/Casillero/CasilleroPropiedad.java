package com.tallerwebi.dominio.entidades.Casillero;

import com.tallerwebi.dominio.entidades.Propiedad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class CasilleroPropiedad extends Casillero {

    @OneToOne(cascade = CascadeType.PERSIST)
    private Propiedad propiedad;

    @Column(name = "partida_id")
    private Long partidaId;

    public CasilleroPropiedad() {
    }

    public CasilleroPropiedad(Long id, Integer posicion, String nombre, String descripcion, Propiedad propiedad) {
        super(id, posicion, nombre, descripcion);
        this.propiedad = propiedad;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    public void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    public Long getPartidaId() {
        return partidaId;
    }

    public void setPartidaId(Long partidaId) {
        this.partidaId = partidaId;
    }


}
