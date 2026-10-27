package com.tallerwebi.presentacion.sesion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import jakarta.servlet.http.HttpServletRequest;

/** Identifico al jugador desde la sesión. */
public final class ResolutorJugadorSesion {

    private ResolutorJugadorSesion() {
    }

    public static String resolverCodigoPartida(String codigoUnico, HttpServletRequest request) {
        if (codigoUnico != null && !codigoUnico.isBlank()) return codigoUnico;
        String codigoSala = (String) request.getSession().getAttribute("codigoSala");
        return codigoSala != null && !codigoSala.isBlank() ? codigoSala : null;
    }

    public static Long resolverUsuarioSesionId(HttpServletRequest request) {
        Long id = (Long) request.getSession().getAttribute("id");
        return id != null ? id : (Long) request.getSession().getAttribute("IDUSUARIO");
    }

    /** Usa el nombre autenticado en sesión; los identificadores de usuario y jugador no son equivalentes. */
    public static Long resolverAutenticado(Partida partida, HttpServletRequest request) {
        String nombre = (String) request.getSession().getAttribute("NOMBRE");
        if (nombre == null || partida == null || partida.getJugadores() == null) return null;
        return partida.getJugadores().stream()
                .filter(jugador -> nombre.equalsIgnoreCase(jugador.getApodo()))
                .map(Jugador::getId).findFirst().orElse(null);
    }

    public static Long resolver(Partida partida, HttpServletRequest request) {
        return resolver(partida, resolverUsuarioSesionId(request),
                (String) request.getSession().getAttribute("NOMBRE"));
    }

    public static Long resolver(Partida partida, Long usuarioSesionId, String nombreSesion) {
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
