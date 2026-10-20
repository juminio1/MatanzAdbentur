package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificadorPartida {

    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private final SimpMessagingTemplate template;

    @Autowired
    public NotificadorPartida(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notificarCambioDeTurno(String codigoPartida, TurnoDTO turno) {
        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/turno", turno);
    }

    public void notificarMovimiento(String codigoPartida, RespuestaMovimientoDTO movimiento){
        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida, movimiento);

    }

    public static void notificarSaldos(SimpMessagingTemplate template, ServicioPartida servicioPartida,
                                       String codigoPartida) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || partida.getJugadores() == null) return;
        for (Jugador jugador : partida.getJugadores()) {
            Map<String, Object> saldo = new HashMap<>();
            saldo.put("idJugador", jugador.getId());
            saldo.put("dinero", jugador.getDinero());
            saldo.put("enBancarrota", jugador.getEnBancarrota());
            template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/dinero", saldo);
        }
    }

    public static void notificarAlquiler(SimpMessagingTemplate template, String codigoPartida, Jugador pagador,
                                         Jugador propietario, String nombrePropiedad, Integer monto) {
        Map<String, Object> aviso = new HashMap<>();
        aviso.put("idJugadorPagador", pagador.getId());
        aviso.put("pagador", pagador.getApodo());
        aviso.put("propietario", propietario.getApodo());
        aviso.put("propiedad", nombrePropiedad);
        aviso.put("monto", monto);
        template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/alquiler", aviso);
    }
}
