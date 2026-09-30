package com.tallerwebi.presentacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.tallerwebi.dominio.ServicioSalaDeEspera;

@Controller 
public class ControladorSalaEspera {

    private final ServicioSalaDeEspera servicioSalaDeEspera;
    private final String SALA_DE_ESPERA = "sala-de-espera";

    @Autowired 
    public ControladorSalaEspera(ServicioSalaDeEspera servicioSalaDeEspera){
        this.servicioSalaDeEspera = servicioSalaDeEspera;

    }

  @RequestMapping(path = "/sala-de-espera", method = RequestMethod.GET)
  public ModelAndView irASalaDeEspera() {
    return new ModelAndView(SALA_DE_ESPERA);
  }


  
    
}
