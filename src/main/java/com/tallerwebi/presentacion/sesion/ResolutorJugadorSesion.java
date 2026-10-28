package com.tallerwebi.presentacion.sesion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Devuelve el jugador autenticado en sesión y exige que pertenezca a la partida.
     * Responde 401 si no hay sesión y 403 si el jugador es ajeno a la partida.
     */
    public static Long resolverParticipante(Partida partida, HttpServletRequest request,
                                            ServicioParticipacion participacion) {
        if (resolverUsuarioSesionId(request) == null && request.getSession().getAttribute("NOMBRE") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debés iniciar sesión.");
        }
        Long idJugador = resolverAutenticado(partida, request);
        if (!participacion.pertenece(partida, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenecés a esta partida.");
        }
        return idJugador;
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
