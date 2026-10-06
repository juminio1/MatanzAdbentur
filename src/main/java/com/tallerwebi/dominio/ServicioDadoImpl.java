package com.tallerwebi.dominio;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service("servicioDado")
public class ServicioDadoImpl implements ServicioDado {

    private final Random random = new Random();

    @Override
    public ResultadoTirada tirarDados() {
        int d1 = random.nextInt(6) + 1; // Genera valor entre 1 y 6
        int d2 = random.nextInt(6) + 1; // Genera valor entre 1 y 6
        return new ResultadoTirada(d1, d2);
    }
}