package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioRegistro;
import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorRegistro {

  private static final String VISTA_NUEVO_USUARIO = "nuevo-usuario";
  private static final String ERROR = "error";

  private ServicioRegistro servicioRegistro;

  @Autowired
  public ControladorRegistro(ServicioRegistro servicioRegistro) {
    this.servicioRegistro = servicioRegistro;
  }

  @RequestMapping(path = "/nuevo-usuario", method = RequestMethod.GET) // muestra el form
  public ModelAndView nuevoUsuario() {
    Map<String, Object> model = new ModelMap();
    model.put("registro", new RegistroDTO());
    return new ModelAndView("nuevo-usuario", model);
  }

  @RequestMapping(path = "/registrarme", method = RequestMethod.POST)
  @Valid
  public ModelAndView registrarme(
    @Valid @ModelAttribute("registro") RegistroDTO registro,
    BindingResult resultado
  ) {
    if (resultado.hasErrors()) {
      return new ModelAndView("nuevo-usuario");
    }
    return this.procesarRegistro(registro);
  }

  private ModelAndView procesarRegistro(RegistroDTO registro) {
    Map<String, Object> model = new ModelMap();

    try {
      servicioRegistro.registrar(registro);
    } catch (UsuarioExistenteException e) {
      model.put(ERROR, "El usuario ya existe");
      return new ModelAndView(VISTA_NUEVO_USUARIO, model);
    } catch (ContraseniaInvalidaException i) {
      model.put(ERROR, "La contraseña no es válida");
      return new ModelAndView(VISTA_NUEVO_USUARIO, model);
    } catch (EmailInvalidoException o) {
      model.put(ERROR, "El email no tiene un formato valido");
      return new ModelAndView(VISTA_NUEVO_USUARIO, model);
    } catch (CamposObligatoriosException u) {
      model.put(ERROR, "Los campos deben ser obligatorios");
      return new ModelAndView(VISTA_NUEVO_USUARIO, model);
    }
    return new ModelAndView("redirect:/login");
  }
}
