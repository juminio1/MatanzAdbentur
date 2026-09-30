package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioLogin;
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
    private final String LOGIN = "login";
  

    @Autowired
    public ControladorSalaEspera(ServicioSalaDeEspera servicioSalaDeEspera) {
        this.servicioSalaDeEspera = servicioSalaDeEspera;
        
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.GET)
    public ModelAndView irASalaDeEspera() {
        return new ModelAndView(SALA_DE_ESPERA);
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.POST)
    public ModelAndView crearSalaDeEspera(HttpServletRequest request){
      Map<String, Object> model = new ModelMap();
       
       Long idUsuario = (Long) request.getSession().getAttribute("id"); //obtiene la sesión del usuario que hizo la petición, busca dentro de esa sesión el atributo id 

        try {
          this.servicioSalaDeEspera.crearSalaDeEspera(idUsuario);
        } catch (UsuarioNoEncontradoException e) {
            model.put("error", "El usuario no existe");
            return new ModelAndView(LOGIN, model);
        }

        return new ModelAndView(SALA_DE_ESPERA);
    }
}
