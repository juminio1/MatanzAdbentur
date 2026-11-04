package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// Conserva el resultado ya aplicado para representarlo sin volver a ejecutarlo.
@Embeddable
public class ConsecuenciaEventoGlobal {

    @Column(name = "consecuenciaEvento", length = 2000)
    private String detalle;

    @ManyToOne
    @JoinColumn(name = "tarjeta_evento_global_id")
    private EventoCasillero tarjeta;

    private Integer posicionCompraEvento;

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public EventoCasillero getTarjeta() {
        return tarjeta;
    }

    public void setTarjeta(EventoCasillero tarjeta) {
        this.tarjeta = tarjeta;
    }

    public Integer getPosicionCompraEvento() {
        return posicionCompraEvento;
    }

    public void setPosicionCompraEvento(Integer posicion) {
        posicionCompraEvento = posicion;
    }
}
