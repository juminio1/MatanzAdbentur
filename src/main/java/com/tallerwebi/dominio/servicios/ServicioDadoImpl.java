package com.tallerwebi.dominio.servicios;

import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service("servicioDado")
public class ServicioDadoImpl implements ServicioDado {

    private final Random random = new Random();

    @Override
    public ResultadoTiradaDTO tirarDados() {
        int d1 = random.nextInt(6) + 1; // Genera valor entre 1 y 6
        int d2 = random.nextInt(6) + 1; // Genera valor entre 1 y 6
        return new ResultadoTiradaDTO(d1, d2);
    }
}