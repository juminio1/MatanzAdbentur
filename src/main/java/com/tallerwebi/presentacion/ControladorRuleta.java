
package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.GestorRuletaPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.DTO.EstadoRuletaDTO;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController //devuelve el resultado directo en la respuesta HTTP, sin buscar una vista Thymeleaf
@RequestMapping("/ruleta")
public class ControladorRuleta {

    private final GestorRuletaPartida gestorRuleta;
    private final ServicioPartida servicioPartida;
    private final ServicioParticipacion participacion;

    public ControladorRuleta(GestorRuletaPartida gestorRuleta, ServicioPartida servicioPartida, ServicioParticipacion participacion) {
        this.gestorRuleta = gestorRuleta;
        this.servicioPartida = servicioPartida;
        this.participacion = participacion;
    }
    
    @PostMapping("/{codigoPartida}/{activacionId}/sortear") //Recibe solicitudes HTTP mediante POST con esa ruta
    public ResponseEntity<EstadoRuletaDTO> sortearDificultad(@PathVariable("codigoPartida") String codigoPartida, @PathVariable("activacionId") String activacionId, HttpServletRequest request) {
        Long idJugador = validarParticipante(codigoPartida, request);

        try {
            // El gestor evita realizar sorteos duplicados.
            EstadoRuletaDTO resultado = gestorRuleta.sortearDificultad(codigoPartida, activacionId, idJugador);
            return ResponseEntity.ok(resultado);
        
            } catch (SecurityException excepcion) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, excepcion.getMessage(), excepcion);
            } catch (IllegalStateException excepcion) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, excepcion.getMessage(), excepcion);
            }

    }

    @GetMapping("/{codigoPartida}/estado")
    public ResponseEntity<EstadoRuletaDTO> obtenerEstado(@PathVariable("codigoPartida") String codigoPartida, HttpServletRequest request) {
        validarParticipante(codigoPartida, request);
        EstadoRuletaDTO estado = gestorRuleta.obtenerEstado(codigoPartida);

        if (estado == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(estado);
    }

    private Long validarParticipante(String codigoPartida, HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);

        if (partida == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La partida no existe.");
        }

        // Identifica al jugador autenticado y verifica que pertenezca a la partida.
        return ResolutorJugadorSesion.resolverParticipante(partida, request, participacion);
    }
}
