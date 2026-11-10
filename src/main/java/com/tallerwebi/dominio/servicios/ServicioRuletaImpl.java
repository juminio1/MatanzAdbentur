
package com.tallerwebi.dominio.servicios;

import java.util.Random;

import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

public class ServicioRuletaImpl implements ServicioRuleta {

    private final Random generadorAleatorio;

    private static final String DIFICULTAD_FACIL = "FACIL";
    private static final String DIFICULTAD_MEDIO = "MEDIO";
    private static final String DIFICULTAD_DIFICIL = "DIFICIL";

    private static final int LIMITE_FACIL = 50;
    private static final int LIMITE_MEDIO = 79;
    private static final int TOTAL_PROBABILIDAD = 100; 

    public ServicioRuletaImpl(Random probabilidadRandom) {
        this.generadorAleatorio = probabilidadRandom;
    }

    @Override
    public String sortearDificultad() {

        int numeroAleatorio = generadorAleatorio.nextInt(TOTAL_PROBABILIDAD); //Uso int porque los limites son valores primitivos

        if (numeroAleatorio < LIMITE_FACIL) {
            return DIFICULTAD_FACIL;
        }

        if (numeroAleatorio <= LIMITE_MEDIO) {
            return DIFICULTAD_MEDIO;
        }

        return DIFICULTAD_DIFICIL;
    }
}
