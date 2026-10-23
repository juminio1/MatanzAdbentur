package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl.DeclaracionQuiebraInvalidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Atiende las consultas de deuda y la declaración voluntaria de quiebra. */
@Controller
public class ControladorGestionQuiebra {

    private static final String CLAVE_ERROR = "error";
    private static final String SESION_NOMBRE = "NOMBRE";
    private static final String CLAVE_ID_JUGADOR = "idJugador";
    private static final String ERROR_JUGADOR_AJENO = "No pertenecés a esta partida.";
    private static final String PARAM_CODIGO_PARTIDA = "codigoPartida";
    private final ServicioPartida servicioPartida;
    private final ServicioGestionQuiebra servicioGestionQuiebra;

    public ControladorGestionQuiebra(ServicioPartida servicioPartida, ServicioGestionQuiebra servicioGestionQuiebra) {
        this.servicioPartida = servicioPartida;
        this.servicioGestionQuiebra = servicioGestionQuiebra;
    }

    // Consulta la deuda del jugador de sesión para mostrar su estado en la partida.
    @GetMapping("/partida/{codigoPartida}/deuda-pendiente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDeudaPendiente(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida, HttpServletRequest request) {
        if (ResolutorJugadorSesion.resolverUsuarioSesionId(request) == null
                && request.getSession().getAttribute(SESION_NOMBRE) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                    .body(Map.of(CLAVE_ERROR, "Debés iniciar sesión."));
        }
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noStore())
                    .body(Map.of(CLAVE_ERROR, "La partida no existe o ya finalizó."));
        }
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!ResolutorJugadorSesion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).cacheControl(CacheControl.noStore())
                    .body(Map.of(CLAVE_ERROR, ERROR_JUGADOR_AJENO));
        }
        DeudaPendiente deuda = servicioGestionQuiebra.obtenerDeudaPendiente(codigoPartida, idJugador);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("tieneDeudaPendiente", deuda != null);
        respuesta.put("montoPendiente", deuda == null ? null : deuda.getMontoPendiente());
        respuesta.put("motivo", deuda == null ? null : deuda.getMotivo());
        Map<String, Object> acreedor = null;
        if (deuda != null && deuda.getJugadorAcreedor() != null) {
            Jugador jugadorAcreedor = deuda.getJugadorAcreedor();
            acreedor = new HashMap<>();
            acreedor.put(CLAVE_ID_JUGADOR, jugadorAcreedor.getId());
            acreedor.put("apodo", jugadorAcreedor.getApodo());
        }
        respuesta.put("acreedor", acreedor);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }

    // Declara en quiebra al jugador identificado desde la sesión.
    @PostMapping("/partida/{codigoPartida}/declarar-quiebra")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> declararQuiebra(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida, HttpServletRequest request) {
        if (ResolutorJugadorSesion.resolverUsuarioSesionId(request) == null
                && request.getSession().getAttribute(SESION_NOMBRE) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(CLAVE_ERROR, "Debés iniciar sesión."));
        }
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(CLAVE_ERROR, "La partida no existe o ya finalizó."));
        }
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!ResolutorJugadorSesion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, ERROR_JUGADOR_AJENO));
        }
        try {
            servicioGestionQuiebra.declararQuiebra(codigoPartida, idJugador);
            return ResponseEntity.ok(Map.of("mensaje", "Te declaraste en quiebra."));
        } catch (DeclaracionQuiebraInvalidaException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

}
