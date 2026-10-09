package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorSalaEspera {

    private final ServicioSalaDeEspera servicioSalaDeEspera;
    private final String SALA_DE_ESPERA = "sala-de-espera";

    @Autowired
    public ControladorSalaEspera(ServicioSalaDeEspera servicioSalaDeEspera) {
        this.servicioSalaDeEspera = servicioSalaDeEspera;
  
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.POST)
    public ModelAndView crearSalaDeEspera(HttpServletRequest request) {
        Map<String, Object> model = new ModelMap();

        Long idUsuario = (Long) request.getSession().getAttribute("id"); //obtiene la sesión del usuario que hizo la petición, busca dentro de esa sesión el atributo id

        ModelAndView modelo = new ModelAndView(SALA_DE_ESPERA);

        try {
            Partida partida = this.servicioSalaDeEspera.crearSalaDeEspera(idUsuario);
            modelo.addObject("codigoUnico", partida.getCodigoUnico());
        } catch (UsuarioNoEncontradoException e) {
            model.put("error", "El usuario no existe");
            return new ModelAndView("redirect:/login");
        }

        return modelo;
    }
}
