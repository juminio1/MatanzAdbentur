package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioSalaDeEspera;
import com.tallerwebi.presentacion.DTO.SeleccionFichaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controlador de eventos en tiempo real para la sala de espera.
 */
@Controller
public class ControladorSalaEsperaWebSocket {

    private final ServicioSalaDeEspera servicioSalaDeEspera;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ControladorSalaEsperaWebSocket(
            ServicioSalaDeEspera servicioSalaDeEspera,
            @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.servicioSalaDeEspera = servicioSalaDeEspera;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Recibe la peticion de ficha de un jugador e invoca al servicio.
     * El servicio persiste y notifica a la sala automáticamente tras el commit.
     */
    @MessageMapping("/seleccionar-ficha")
    public void seleccionarFicha(SeleccionFichaDTO seleccion) {
        try {
            // 1. Invocar al servicio
            servicioSalaDeEspera.seleccionarFicha(
                    seleccion.getCodigoUnicoPartida(),
                    seleccion.getIdUsuario(),
                    seleccion.getFichaSeleccionada()
            );

            // 2. Notificar la selección a la partida (lo que espera el test)
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend(
                        "/topic/partida/" + seleccion.getCodigoUnicoPartida(),
                        seleccion
                );
            }

        } catch (FichaOcupadaException e) {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend(
                        "/topic/partida/" + seleccion.getCodigoUnicoPartida() + "/errores",
                        "La ficha ya se encuentra seleccionada"
                );
            }
        } catch (Exception e) {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend(
                        "/topic/partida/" + seleccion.getCodigoUnicoPartida() + "/errores",
                        e.getMessage()
                );
            }
        }
    }
}