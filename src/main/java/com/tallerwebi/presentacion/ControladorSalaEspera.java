package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorSalaEspera {

    private final ServicioSalaDeEspera servicioSalaDeEspera;
    private final String SALA_DE_ESPERA = "sala-de-espera";
    private static final String REDIRECT_LOGIN = "redirect:/login";

    @Autowired
    public ControladorSalaEspera(ServicioSalaDeEspera servicioSalaDeEspera) {
        this.servicioSalaDeEspera = servicioSalaDeEspera;
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.GET)
    public ModelAndView verSalaDeEspera(@RequestParam("codigoUnico") String codigoUnico, HttpServletRequest request) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");
        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.obtenerSalaPorCodigo(codigoUnico);
            
            ModelAndView modelo = new ModelAndView(SALA_DE_ESPERA);
            modelo.addObject("codigoUnico", codigoUnico);
            modelo.addObject("usuarios", sala.getUsuarios());
            modelo.addObject("cantidadUsuarios", sala.getUsuarios().size());
            return modelo;

        } catch (SalaNoEncontradaException e) {
            return new ModelAndView("redirect:/home");
        }
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.POST)
    public ModelAndView crearSalaDeEspera(HttpServletRequest request) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");
        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.crearSalaDeEspera(idUsuario);
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + sala.getCodigoUnico());
        } catch (UsuarioNoEncontradoException e) {
            return new ModelAndView(REDIRECT_LOGIN);
        }
    }

    @RequestMapping(path = "/unirse-a-partida", method = RequestMethod.POST)
    public ModelAndView unirseASala(@RequestParam("codigoUnico") String codigoUnico, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");
        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.unirseASalaDeEspera(idUsuario, codigoUnico);
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + sala.getCodigoUnico());
            
        } catch (UsuarioNoEncontradoException e) {
            return new ModelAndView(REDIRECT_LOGIN);
            
        } catch (SalaNoEncontradaException | SalaDeEsperaLlenaException e) {
            redirectAttributes.addFlashAttribute("errorUnirse", "Código incorrecto o sala llena.");
            return new ModelAndView("redirect:/home");
        }
    }

    
}
