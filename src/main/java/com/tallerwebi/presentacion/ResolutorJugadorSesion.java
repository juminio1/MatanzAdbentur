package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import jakarta.servlet.http.HttpServletRequest;

/** Resuelve la identidad de sesión y su pertenencia a una partida. */
final class ResolutorJugadorSesion {

    private ResolutorJugadorSesion() {
    }

    static String resolverCodigoPartida(String codigoUnico, HttpServletRequest request) {
        if (codigoUnico != null && !codigoUnico.isBlank()) return codigoUnico;
        String codigoSala = (String) request.getSession().getAttribute("codigoSala");
        return codigoSala != null && !codigoSala.isBlank() ? codigoSala : null;
    }

    static Long resolverUsuarioSesionId(HttpServletRequest request) {
        Long id = (Long) request.getSession().getAttribute("id");
        return id != null ? id : (Long) request.getSession().getAttribute("IDUSUARIO");
    }

    static Long resolver(Partida partida, HttpServletRequest request) {
        return resolver(partida, resolverUsuarioSesionId(request),
                (String) request.getSession().getAttribute("NOMBRE"));
    }

    static Boolean pertenece(Partida partida, Long idJugador) {
        return idJugador != null && partida.getJugadores() != null
                && partida.getJugadores().stream().anyMatch(jugador -> idJugador.equals(jugador.getId()));
    }

    static Long resolver(Partida partida, Long usuarioSesionId, String nombreSesion) {
        if (partida == null || partida.getJugadores() == null) return usuarioSesionId;
        for (Jugador jugador : partida.getJugadores()) {
            if (nombreSesion != null && nombreSesion.equalsIgnoreCase(jugador.getApodo())) {
                return jugador.getId();
            }
            if (usuarioSesionId != null && usuarioSesionId.equals(jugador.getId())) {
                return jugador.getId();
            }
        }
        return usuarioSesionId;
    }
}
