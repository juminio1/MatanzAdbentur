package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service("servicioDado")
public class ServicioDadoImpl implements ServicioDado {

    private final Random random = new Random();

    @Override
    public ResultadoTirada tirarDados() {
        return new ResultadoTirada(random.nextInt(6) + 1, random.nextInt(6) + 1);
    }
}
