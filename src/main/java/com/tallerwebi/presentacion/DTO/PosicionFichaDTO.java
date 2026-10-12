package com.tallerwebi.presentacion.DTO;

public class PosicionFichaDTO {

    private final int idJugador;
    private final String apodo;
    private final String color;
    private final Integer posicion;

    public PosicionFichaDTO(
        int idJugador,
        String apodo,
        String color,
        Integer posicion
    ) {
        this.idJugador = idJugador;
        this.apodo = apodo;
        this.color = color;
        this.posicion = posicion;
    }

    public int getIdJugador() {
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