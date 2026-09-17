package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.ServicioPartida;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontrado;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPartida {

  private final ServicioPartida servicioPartida;

  @Autowired
  public ControladorPartida(ServicioPartida servicioPartida) {
    this.servicioPartida = servicioPartida;
  }

  @RequestMapping(path = "/crear-partida", method = RequestMethod.POST)
  public ModelAndView crearPartida() {
    Integer idUsuario = 1;
    Partida partidaCreada;
    Map<String, Object> modelo = new ModelMap();
    try {
      partidaCreada = this.servicioPartida.crearPartida(idUsuario);
      modelo.put("partidaCreada", partidaCreada);
    } catch (UsuarioNoEncontrado e) {
      modelo.put("error", "usuario no encontrado");
      return new ModelAndView("home", modelo);
    }
    return new ModelAndView("sala-de-espera", modelo);
  }
}
