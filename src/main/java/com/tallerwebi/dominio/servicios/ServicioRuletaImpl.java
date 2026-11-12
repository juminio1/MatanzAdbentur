
package com.tallerwebi.dominio.servicios;

import java.util.Random;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

public class ServicioRuletaImpl implements ServicioRuleta {

    private final Random generadorAleatorio;

    private static final int LIMITE_FACIL = 50;
    private static final int LIMITE_MEDIO = 79;
    private static final int TOTAL_PROBABILIDAD = 100;

    public ServicioRuletaImpl(Random probabilidadRandom) {
        this.generadorAleatorio = probabilidadRandom;
    }

    @Override
    public DificultadRuleta sortearDificultad() {

        int numeroAleatorio = this.generadorAleatorio.nextInt(TOTAL_PROBABILIDAD); //Uso int porque los limites son valores primitivos

        if (numeroAleatorio < LIMITE_FACIL) {
            return DificultadRuleta.FACIL;
        }

        if (numeroAleatorio <= LIMITE_MEDIO) {
            return DificultadRuleta.MEDIO;
        }

        return DificultadRuleta.DIFICIL;
    }
}
