package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/** Coordina las consultas y notificaciones del turno vigente de una partida. */
final class CoordinadorTurnoPartida {

    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;

    CoordinadorTurnoPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket) {
        this.servicioPartida = servicioPartida;
        this.notificadorWebSocket = notificadorWebSocket;
    }

    TurnoDTO obtenerTurnoActual(String codigoPartida) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (partida == null || jugadorActual == null) return null;
        return crearTurno(codigoPartida, partida, jugadorActual);
    }

    TurnoDTO avanzarYNotificarTurno(String codigoPartida) {
        servicioPartida.avanzarTurno(codigoPartida);
        Partida partidaActualizada = servicioPartida.obtenerPartida(codigoPartida);
        Jugador siguienteJugador = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (partidaActualizada == null || siguienteJugador == null) return null;
        TurnoDTO turno = crearTurno(codigoPartida, partidaActualizada, siguienteJugador);
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/turno", turno);
        return turno;
    }

    private TurnoDTO crearTurno(String codigoPartida, Partida partida, Jugador jugadorActual) {
        TurnoDTO turno = new TurnoDTO(codigoPartida, jugadorActual.getId(), jugadorActual.getApodo(),
                jugadorActual.getOrden(), "Es el turno de " + jugadorActual.getApodo());
        turno.setRondaActual(partida.getRondaActual());
        return turno;
    }
}
