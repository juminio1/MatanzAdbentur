package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.*;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorSalaEspera {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorSalaEspera.class);

    private final ServicioSalaDeEspera servicioSalaDeEspera;
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate messagingTemplate;

    private final String SALA_DE_ESPERA = "sala-de-espera";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String CODIGO_UNICO = "codigoUnico";
    private static final String REDIRECT_HOME = "redirect:/home";
    private static final String REDIRECT_PARTIDA = "redirect:/partida";
    private static final String REDIRECT_SALA = "redirect:/sala-de-espera";

    @Autowired
    public ControladorSalaEspera(
        ServicioSalaDeEspera servicioSalaDeEspera,
        ServicioPartida servicioPartida,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.servicioSalaDeEspera = servicioSalaDeEspera;
        this.servicioPartida = servicioPartida;
        this.messagingTemplate = messagingTemplate;
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.GET)
    public ModelAndView verSalaDeEspera(
        @RequestParam(CODIGO_UNICO) String codigoUnico,
        HttpServletRequest request
    ) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");
        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.obtenerSalaPorCodigo(codigoUnico);

            ModelAndView modelo = new ModelAndView(SALA_DE_ESPERA);
            modelo.addObject(CODIGO_UNICO, codigoUnico);
            modelo.addObject("usuarios", sala.getUsuarios());
            modelo.addObject("cantidadUsuarios", sala.getUsuarios().size());
            modelo.addObject("idUsuario", idUsuario);
            return modelo;
        } catch (SalaNoEncontradaException e) {
            return new ModelAndView(REDIRECT_HOME);
        }
    }

    @RequestMapping(path = "/sala-de-espera", method = RequestMethod.POST)
    public ModelAndView crearSalaDeEspera(HttpServletRequest request) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");

        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.crearSalaDeEspera(idUsuario);

            return new ModelAndView(
                "redirect:/sala-de-espera?" + CODIGO_UNICO + "=" + sala.getCodigoUnico()
            );
        } catch (UsuarioNoEncontradoException e) {
            return new ModelAndView(REDIRECT_LOGIN);
        }
    }

    @RequestMapping(path = "/unirse-a-partida", method = RequestMethod.POST)
    public ModelAndView unirseASala(
        @RequestParam(CODIGO_UNICO) String codigoUnico,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes
    ) {
        Long idUsuario = (Long) request.getSession().getAttribute("id");

        if (idUsuario == null) return new ModelAndView(REDIRECT_LOGIN);

        try {
            SalaDeEspera sala = this.servicioSalaDeEspera.unirseASalaDeEspera(idUsuario, codigoUnico);

            return new ModelAndView(
                "redirect:/sala-de-espera?" + CODIGO_UNICO + "=" + sala.getCodigoUnico()
            );
        } catch (UsuarioNoEncontradoException e) {
            return new ModelAndView(REDIRECT_LOGIN);
        } catch (SalaNoEncontradaException | SalaDeEsperaLlenaException e) {
            redirectAttributes.addFlashAttribute("errorUnirse", "Código incorrecto o sala llena.");

            return new ModelAndView(REDIRECT_HOME);
        }
    }

   @RequestMapping(path = "/iniciar-partida", method = RequestMethod.POST)
public ModelAndView iniciarPartida(
    @RequestParam(value = CODIGO_UNICO, required = false) String codigoUnico,
    HttpServletRequest request,
    RedirectAttributes redirectAttributes
) {
    Long idUsuario = obtenerIdUsuario(request);

    if (idUsuario == null) {
        return new ModelAndView(REDIRECT_LOGIN);
    }

    if (codigoUnico == null || codigoUnico.isBlank()) {
        return redireccionarConError(
            REDIRECT_HOME,
            "El código de la sala es inválido",
            redirectAttributes
        );
    }

    return procesarInicioPartida(codigoUnico, idUsuario, request, redirectAttributes);
}

private ModelAndView procesarInicioPartida(String codigoUnico, Long idUsuario, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    try {
        servicioPartida.iniciarPartida(codigoUnico, idUsuario); 

        return iniciarPartidaExitosamente(
            codigoUnico,
            request,
            redirectAttributes
        );

    } catch (IllegalArgumentException exception) {
        return redireccionarASalaConError(
            codigoUnico,
            "Solo el creador de la sala puede iniciar la partida",
            redirectAttributes
        );

    } catch (SalaNoEncontradaException exception) {
        return redireccionarConError(
            REDIRECT_HOME,
            "La sala de espera no existe",
            redirectAttributes
        );

    } catch (PartidaNoEncontradaException exception) {
        return redireccionarASalaConError(
            codigoUnico,
            "No se encontró una partida para esta sala",
            redirectAttributes
        );

    } catch (CantidadJugadoresInsuficienteException exception) {
        return redireccionarASalaConError(
            codigoUnico,
            "No hay suficientes jugadores para iniciar la partida",
            redirectAttributes
        );

    } catch (CantidadMaximaJugadoresSuperadaException exception) {
        return redireccionarASalaConError(
            codigoUnico,
            "Se superó la cantidad máxima de jugadores",
            redirectAttributes
        );

    } catch (PartidaIniciadaException exception) {
        request.getSession().setAttribute("codigoSala", codigoUnico);
        redirectAttributes.addAttribute("codigo", codigoUnico);

        return new ModelAndView(REDIRECT_PARTIDA);

    } catch (FichaNoDisponibleException exception) {
        return redireccionarASalaConError(
                codigoUnico,
                exception.getMessage(),
                redirectAttributes
        );

    } catch (Exception exception) {
        LOGGER.error("Error al iniciar la partida {}", codigoUnico, exception);

        return redireccionarASalaConError(
            codigoUnico,
            "Ocurrió un error al iniciar la partida",
            redirectAttributes
        );
    }
}

    private ModelAndView iniciarPartidaExitosamente(
    String codigoUnico,
    HttpServletRequest request,
    RedirectAttributes redirectAttributes
) {
    request.getSession().setAttribute("codigoSala", codigoUnico);

    messagingTemplate.convertAndSend(
        "/topic/sala/" + codigoUnico + "/iniciar",
        "INICIAR"
    );

    redirectAttributes.addAttribute("codigo", codigoUnico);

    return new ModelAndView(REDIRECT_PARTIDA);
}

private ModelAndView redireccionarASalaConError(String codigoUnico, String mensaje, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("errorIniciarPartida", mensaje);
    redirectAttributes.addAttribute(CODIGO_UNICO, codigoUnico);

    return new ModelAndView(REDIRECT_SALA);
}

private ModelAndView redireccionarConError(String destino, String mensaje, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("errorIniciarPartida", mensaje);

    return new ModelAndView(destino);
}

private Long obtenerIdUsuario(HttpServletRequest request) {
    Long idUsuario = (Long) request.getSession().getAttribute("id");

    if (idUsuario == null) {
        idUsuario = (Long) request.getSession().getAttribute("IDUSUARIO");
    }

    return idUsuario;
}
}
