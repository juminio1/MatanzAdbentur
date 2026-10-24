package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service("servicioDado")
public class ServicioDadoImpl implements ServicioDado {

    private final Random random = new Random();

     @Override
    public ResultadoTiradaDTO tirarDados() {
        return new ResultadoTiradaDTO(random.nextInt(6) + 1, random.nextInt(6) + 1);
    }  

       
}
