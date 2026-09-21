package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.ServicioPartida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class ControladorPartida {

    private final ServicioPartida servicioPartida;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida) {
        this.servicioPartida = servicioPartida;
    }

    @RequestMapping(path = "/crear-partida", method = RequestMethod.POST)
    public ModelAndView crearPartida(){
        Integer id = 1;
        Partida partidaCreada = this.servicioPartida.crearPartida(id);
        Map<String, Object> modelo = new ModelMap();
        modelo.put("partidaCreada", partidaCreada);
        return new ModelAndView("tablero", modelo);
    }

}
