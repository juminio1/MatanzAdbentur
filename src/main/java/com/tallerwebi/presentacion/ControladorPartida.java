package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioDado;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorPartida {

    private final ServicioDado servicioDado;

    public ControladorPartida(ServicioDado servicioDado) {
        this.servicioDado = servicioDado;
    }

    @PostMapping("/partida/tirar-dados")
    public ResultadoTirada tirarDados() {
        return servicioDado.tirarDados();
    }
}
