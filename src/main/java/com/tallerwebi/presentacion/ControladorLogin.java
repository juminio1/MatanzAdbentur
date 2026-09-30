package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import com.tallerwebi.dominio.servicios.ServicioLogin;
import com.tallerwebi.presentacion.DTO.LoginDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;


@Controller
public class ControladorLogin {

  private static final String VISTA_LOGIN = "login";
  private static final String ERROR_ATTR = "error";

  private final ServicioLogin servicioLogin;
  private HttpServletRequest request;

  public ControladorLogin(ServicioLogin servicioLogin) {
    this.servicioLogin = servicioLogin;
  }

  @Autowired
  public ControladorLogin(ServicioLogin servicioLogin, HttpServletRequest request) {
    this.servicioLogin = servicioLogin;
    this.request = request;
  }

  @RequestMapping("/login")
  public ModelAndView irALogin() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosLogin", new LoginDTO());
    return new ModelAndView(VISTA_LOGIN, modelo);
  }

  @RequestMapping(path = "validar-login", method = RequestMethod.POST)
  public ModelAndView validarLogin(@ModelAttribute("datosLogin") LoginDTO datosLogin, HttpServletRequest request) {
    try {
      Usuario usuarioAutenticado = servicioLogin.autenticar(
          datosLogin.getCredencial(),
          datosLogin.getPassword());
      HttpServletRequest actualRequest = request != null ? request : this.request;
      if (actualRequest != null && actualRequest.getSession() != null) {
        actualRequest.getSession().setAttribute("ROL", usuarioAutenticado.getRol());
        actualRequest.getSession().setAttribute("NOMBRE", usuarioAutenticado.getUsername());
      } // SESION STORAGE LOQUITA
      return new ModelAndView("redirect:/home");
    } catch (CredencialesInvalidasException e) {
      Map<String, Object> model = new ModelMap();
      model.put(ERROR_ATTR, e.getMessage());
      return new ModelAndView(VISTA_LOGIN, model);
    }
  }

 @GetMapping("/home")
  public ModelAndView irAHome() {
    HttpServletRequest actualRequest = this.request;
    String nombre = null;

    if (actualRequest != null && actualRequest.getSession(false) != null) {
      nombre = (String) actualRequest.getSession(false).getAttribute("NOMBRE");
    }

    if (nombre == null) {
      return new ModelAndView("redirect:/login");
    }

    Map<String, Object> modelo = new ModelMap();
    modelo.put("nombreJugador", nombre);
    return new ModelAndView("home", modelo);
  }

  @GetMapping("/")
  public ModelAndView inicio() {
    return new ModelAndView("redirect:/login");
  }

  @GetMapping("/cerrar-sesion")
  public ModelAndView cerrarSesion(HttpServletRequest request) {
    HttpServletRequest actualRequest = request != null ? request : this.request;
    if (actualRequest != null && actualRequest.getSession(false) != null) {
      actualRequest.getSession(false).invalidate();
    }
    return new ModelAndView("redirect:/login");
  }
}
