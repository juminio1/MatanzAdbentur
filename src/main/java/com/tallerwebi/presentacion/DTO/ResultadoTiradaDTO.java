package com.tallerwebi.presentacion.DTO;

public class ResultadoTiradaDTO {
    private final Integer dado1;
    private final Integer dado2;

    public ResultadoTiradaDTO(Integer dado1, Integer dado2) {
        this.dado1 = dado1;
        this.dado2 = dado2;
    }

    public Integer getDado1() {
        return dado1;
    }

    public Integer getDado2() {
        return dado2;
    }

    public Integer sumarDados() {
        return dado1 + dado2;
    }
}