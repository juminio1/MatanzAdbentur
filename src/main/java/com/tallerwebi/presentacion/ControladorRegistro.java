package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioRegistro;
import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailExistenteException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
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

    private final ServicioRegistro servicioRegistro;

    @Autowired
    public ControladorRegistro(ServicioRegistro servicioRegistro) {
        this.servicioRegistro = servicioRegistro;
    }

    @RequestMapping(path = "/nuevo-usuario", method = RequestMethod.GET)
    public ModelAndView nuevoUsuario() {
        Map<String, Object> model = new ModelMap();
        model.put("registro", new RegistroDTO());
        return new ModelAndView(VISTA_NUEVO_USUARIO, model);
    }

    @RequestMapping(path = "/registrarme", method = RequestMethod.POST)
    public ModelAndView registrarme(@Valid @ModelAttribute("registro") RegistroDTO registro, BindingResult resultado) {
        if (resultado.hasErrors()) {
            return new ModelAndView(VISTA_NUEVO_USUARIO);
        }

        return this.procesarRegistro(registro, resultado);
    }

    private ModelAndView procesarRegistro(RegistroDTO registro, BindingResult resultado) {
        try {
            servicioRegistro.registrar(registro);

        } catch (EmailExistenteException e) {
            resultado.rejectValue("email", "email.existente", "Este email ya se encuentra registrado");
            return new ModelAndView(VISTA_NUEVO_USUARIO);

        } catch (UsernameExistenteException e) {
            resultado.rejectValue("username", "username.existente", "Este apodo ya se encuentra registrado");
            return new ModelAndView(VISTA_NUEVO_USUARIO);

        } catch (ContraseniaInvalidaException e) {
            resultado.rejectValue("passwordRepetido", "password.no.coincide", "Las contraseñas no coinciden");
            return new ModelAndView(VISTA_NUEVO_USUARIO);

        } catch (EmailInvalidoException e) {
            resultado.rejectValue("email", "email.invalido", "El email no tiene un formato válido");
            return new ModelAndView(VISTA_NUEVO_USUARIO);

        } catch (CamposObligatoriosException e) {
            resultado.reject("campos.obligatorios", "Todos los campos son obligatorios");
            return new ModelAndView(VISTA_NUEVO_USUARIO);

        } catch (UsuarioExistenteException e) {
            resultado.reject("usuario.existente", "El usuario ya se encuentra registrado");
            return new ModelAndView(VISTA_NUEVO_USUARIO);
        }

        return new ModelAndView("redirect:/login");
    }
}