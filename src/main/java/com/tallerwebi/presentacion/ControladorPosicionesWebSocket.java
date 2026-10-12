package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ControladorPosicionesWebSocket {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ControladorPosicionesWebSocket.class
    );

    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ControladorPosicionesWebSocket(
        ServicioPartida servicioPartida,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.servicioPartida = servicioPartida;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/partida/{codigo}/solicitar-posiciones")
    public void solicitarPosiciones(@DestinationVariable String codigo) {

        LOGGER.warn("Solicitud de posiciones recibida. Sala: {}", codigo);

        try {
            Partida partida = servicioPartida.obtenerPartida(codigo);

            if (partida == null) {
                LOGGER.warn("No se encontró la partida: {}", codigo);
                return;
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Partida encontrada con {} jugadores", partida.getJugadores().size());
            }
            List<PosicionFichaDTO> posiciones = partida
                .getJugadores()
                .stream()
                .map(jugador ->
                    new PosicionFichaDTO(
                        jugador.getId(),
                        jugador.getApodo(),
                        jugador.getFicha().name(),
                        jugador.getPosicionActual()
                    )
                )
                .toList();

            messagingTemplate.convertAndSend(
                "/topic/partida/" + codigo + "/posiciones",
                posiciones
            );

            LOGGER.info("Posiciones enviadas a la partida: {}", codigo);
        } catch (Exception exception) {
            LOGGER.error("Error al obtener las posiciones de la partida: {}", codigo, exception);
        }
    }
}
