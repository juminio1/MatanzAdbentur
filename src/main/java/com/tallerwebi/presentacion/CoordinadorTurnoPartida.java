package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.TurnoDTO;

/** Coordina las consultas y notificaciones del turno vigente de una partida. */
final class CoordinadorTurnoPartida {

    private final ServicioPartida servicioPartida;

    CoordinadorTurnoPartida(ServicioPartida servicioPartida) {
        this.servicioPartida = servicioPartida;
    }

    TurnoDTO obtenerTurnoActual(String codigoPartida) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (partida == null || jugadorActual == null) return null;
        return crearTurno(codigoPartida, partida, jugadorActual);
    }

    TurnoDTO avanzarYObtenerTurno(String codigoPartida) {
        servicioPartida.avanzarTurno(codigoPartida);
        return obtenerTurnoActual(codigoPartida);
    }

    private TurnoDTO crearTurno(String codigoPartida, Partida partida, Jugador jugadorActual) {
        TurnoDTO turno = new TurnoDTO(codigoPartida, jugadorActual.getId(), jugadorActual.getApodo(),
                jugadorActual.getOrden(), "Es el turno de " + jugadorActual.getApodo(),
                servicioPartida.obtenerSegundosRestantesTurno(codigoPartida));
        turno.setRondaActual(partida.getRondaActual());
        return turno;
    }
}
