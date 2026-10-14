package com.tallerwebi.presentacion.DTO;

public class PosicionFichaDTO {

    private final Long idJugador;
    private final String apodo;
    private final String color;
    private final Integer posicion;

    public PosicionFichaDTO(
        Long idJugador,
        String apodo,
        String color,
        Integer posicion
    ) {
        this.idJugador = idJugador;
        this.apodo = apodo;
        this.color = color;
        this.posicion = posicion;
    }

    public Long getIdJugador() {
        return idJugador;
    }

    public String getApodo() {
        return apodo;
    }

    public String getColor() {
        return color;
    }

    public Integer getPosicion() {
        return posicion;
    }
}