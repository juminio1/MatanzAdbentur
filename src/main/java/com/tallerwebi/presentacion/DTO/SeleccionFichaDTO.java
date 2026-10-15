package com.tallerwebi.presentacion.DTO;

import com.tallerwebi.dominio.enums.ColorFicha;

public class SeleccionFichaDTO {

    private String codigoUnicoPartida;
    private Long idUsuario;
    private ColorFicha fichaSeleccionada;

    public SeleccionFichaDTO() {
    }

    public SeleccionFichaDTO(String codigoUnicoPartida, Long idUsuario, ColorFicha fichaSeleccionada) {
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

    public ColorFicha getFichaSeleccionada() {
        return fichaSeleccionada;
    }

    public void setFichaSeleccionada(ColorFicha fichaSeleccionada) {
        this.fichaSeleccionada = fichaSeleccionada;
    }
    
}
