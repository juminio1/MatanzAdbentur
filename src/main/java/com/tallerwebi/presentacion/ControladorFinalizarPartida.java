package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ControladorFinalizarPartida {
    private final ServicioFinalizacionPartida servicio;

    public ControladorFinalizarPartida(ServicioFinalizacionPartida servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/partida/{codigoPartida}/resultados-finales")
    @ResponseBody
    public ResponseEntity<?> consultar(@PathVariable("codigoPartida") String codigoPartida,
            HttpServletRequest request) {
        String nombre = (String) request.getSession().getAttribute("NOMBRE");
        if (nombre == null && ResolutorJugadorSesion.resolverUsuarioSesionId(request) == null) {
            return error(HttpStatus.UNAUTHORIZED, "Debés iniciar sesión.");
        }
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(servicio.consultar(codigoPartida, nombre));
        } catch (PartidaNoEncontradaException excepcion) {
            return error(HttpStatus.NOT_FOUND, excepcion.getMessage());
        } catch (SecurityException excepcion) {
            return error(HttpStatus.FORBIDDEN, excepcion.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore())
                .body(Map.of("error", mensaje));
    }
}
