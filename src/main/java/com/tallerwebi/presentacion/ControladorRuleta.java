
package com.tallerwebi.presentacion;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

@Controller
@RequestMapping("/ruleta")
public class ControladorRuleta {

    private final ServicioRuleta servicioRuleta;

    public ControladorRuleta(ServicioRuleta servicioRuleta) {
        this.servicioRuleta = servicioRuleta;
    }

    @PostMapping("/sortear") //Recibe solicitudes HTTP mediante el post con esa ruta
    @ResponseBody //Devuelve el resultado directo en la respuesta http, sin buscar una vista Thymleaf
    public String sortearDificultad() {
        return servicioRuleta.sortearDificultad();
    }
}
