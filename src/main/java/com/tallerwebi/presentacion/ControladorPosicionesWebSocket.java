package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.resultados.PosicionFicha;
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "Solicitud de posiciones recibida via WebSocket para la partida: {}",
                codigo
            );
        }

        try {
            List<PosicionFicha> posiciones = servicioPartida.obtenerPosicionesFichas(codigo);

            if (posiciones.isEmpty() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("No se encontraron jugadores ni ficha para la partida: {}", codigo);
            }

            String destino = "/topic/partida/" + codigo + "/posiciones";
            messagingTemplate.convertAndSend(destino, posiciones);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                    "Posiciones enviadas exitosamente a {}. Total jugadores: {}",
                    destino,
                    posiciones.size()
                );
            }
        } catch (Exception exception) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                    "Error inesperado al responder la solicitud de posiciones para la partida: {}",
                    codigo,
                    exception
                );
            }
        }
    }
}
