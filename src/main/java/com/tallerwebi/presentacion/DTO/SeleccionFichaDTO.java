package com.tallerwebi.presentacion.DTO;

import com.tallerwebi.dominio.enums.Ficha;

public class SeleccionFichaDTO {

    private String codigoUnicoPartida;
    private Long idUsuario;
    private Ficha fichaSeleccionada;

    public SeleccionFichaDTO() {
    }

    public SeleccionFichaDTO(String codigoUnicoPartida, Long idUsuario, Ficha fichaSeleccionada) {
        this.codigoUnicoPartida = codigoUnicoPartida;
        this.idUsuario = idUsuario;
        this.fichaSeleccionada = fichaSeleccionada;
    }

    public String getCodigoUnicoPartida() {
        return codigoUnicoPartida;
    }

    public void setCodigoUnicoPartida(String codigoUnicoPartida) {
        this.codigoUnicoPartida = codigoUnicoPartida;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Ficha getFichaSeleccionada() {
        return fichaSeleccionada;
    }

    public void setFichaSeleccionada(Ficha fichaSeleccionada) {
        this.fichaSeleccionada = fichaSeleccionada;
    }
    
}
