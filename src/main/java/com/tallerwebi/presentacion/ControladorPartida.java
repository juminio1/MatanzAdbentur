package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioDado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class ControladorPartida {

    private final ServicioDado servicioDado;

    public ControladorPartida(ServicioDado servicioDado) {
        this.servicioDado = servicioDado;
    }

    @GetMapping("/partida")
    public ModelAndView vistaTableroPartida() {
        return new ModelAndView("partida");
    }

    @PostMapping("/partida/tirar-dados")
    public ResultadoTirada tirarDados() {
        ResultadoTirada resultado = servicioDado.tirarDados();
        return resultado;
    }
}
