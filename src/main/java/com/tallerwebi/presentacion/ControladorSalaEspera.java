package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
            SalaDeEspera sala = this.servicioSalaDeEspera.unirseASalaDeEspera(
                idUsuario,
                codigoUnico
            );
            return new ModelAndView(
                "redirect:/sala-de-espera?" + CODIGO_UNICO + "=" + sala.getCodigoUnico()
            );
        } catch (UsuarioNoEncontradoException e) {
            return new ModelAndView(REDIRECT_LOGIN);
        } catch (SalaNoEncontradaException | SalaDeEsperaLlenaException e) {
            redirectAttributes.addFlashAttribute("errorUnirse", "Código incorrecto o sala llena.");
            return new ModelAndView("redirect:/home");
        }
    }

    @RequestMapping(path = "/iniciar-partida", method = RequestMethod.POST)
    public ResponseEntity<Void> iniciarPartida(
        @RequestParam(CODIGO_UNICO) String codigoUnico,
        HttpServletRequest request
    ) {
        Long idUsuario = (Long) request.getSession().getAttribute("IDUSUARIO");
        if (idUsuario == null) {
            idUsuario = (Long) request.getSession().getAttribute("id");
        }

        if (idUsuario == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 1. Guardar código de sala en la sesión para que ControladorPartida lo lea
            request.getSession().setAttribute("codigoSala", codigoUnico);

            SalaDeEspera sala = this.servicioSalaDeEspera.obtenerSalaPorCodigo(codigoUnico);

            // 2. Intentar preparar e iniciar la partida
            try {
                this.servicioSalaDeEspera.prepararEIniciarPartida(sala.getId());
            } catch (com.tallerwebi.dominio.excepcion.PartidaIniciadaException e) {
                // Si la partida ya fue iniciada previamente en la DB, ignoramos el error y dejamos continuar
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                        "La partida ya se encontraba iniciada para el codigo {}",
                        codigoUnico
                    );
                }
            }

            // 3. Notificar a todos los clientes por WebSocket
            this.messagingTemplate.convertAndSend(
                "/topic/sala/" + codigoUnico + "/iniciar",
                "INICIAR"
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                    "Error al iniciar la partida con codigo {}: {}",
                    codigoUnico,
                    e.getMessage(),
                    e
                );
            }
            return ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
            ).build();
        }
    }
}
