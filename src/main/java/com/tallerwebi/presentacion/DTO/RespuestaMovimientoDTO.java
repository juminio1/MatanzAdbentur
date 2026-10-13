package com.tallerwebi.presentacion.DTO;

public class RespuestaMovimientoDTO {

    private Integer dado1;
    private Integer dado2;
    private Integer suma;
    private Integer nuevaPosicion;

    public RespuestaMovimientoDTO() {
    }

    public Integer getDado1() {
        return dado1;
    }

    public void setDado1(Integer dado1) {
        this.dado1 = dado1;
    }

    public Integer getDado2() {
        return dado2;
    }

    public void setDado2(Integer dado2) {
        this.dado2 = dado2;
    }

    public Integer getSuma() {
        return suma;
    }

    public void setSuma(Integer suma) {
        this.suma = suma;
    }

    public Integer getNuevaPosicion() {
        return nuevaPosicion;
    }

    public void setNuevaPosicion(Integer nuevaPosicion) {
        this.nuevaPosicion = nuevaPosicion;
    }
}
