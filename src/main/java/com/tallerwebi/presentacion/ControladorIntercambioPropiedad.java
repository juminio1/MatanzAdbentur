package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioIntercambioPropiedad.Propuesta;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/partida/{codigo}/intercambios")
@ResponseBody
public class ControladorIntercambioPropiedad {
    private final ServicioPartida partidas;
    private final ServicioIntercambioPropiedad intercambios;
    private final ServicioParticipacion participacion;
    private final SimpMessagingTemplate mensajes;
    private final MapeadorPropiedad mapeador;

    public record Solicitud(Long idDestinatario, Integer posicionOfrecida, Integer posicionSolicitada) { }
    public record Respuesta(Boolean aceptar, Integer posicionElegida) { }

    public ControladorIntercambioPropiedad(ServicioPartida partidas, ServicioIntercambioPropiedad intercambios,
            ServicioParticipacion participacion, SimpMessagingTemplate mensajes,
            ServicioCasillero casilleros, ServicioPropiedad propiedades) {
        this.partidas = partidas;
        this.intercambios = intercambios;
        this.participacion = participacion;
        this.mensajes = mensajes;
        this.mapeador = new MapeadorPropiedad(casilleros, propiedades);
    }

    @PostMapping
    public ResponseEntity<?> proponer(@PathVariable("codigo") String codigo, @RequestBody Solicitud datos, HttpServletRequest request) {
        Long actor = actor(codigo, request);
        if (actor == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            Propuesta propuesta = intercambios.proponer(codigo, actor, datos.idDestinatario(), datos.posicionOfrecida(), datos.posicionSolicitada());
            Map<String, Object> dto = dto(propuesta);
            publicar(codigo, dto);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return error(ex);
        }
    }

    @GetMapping
    public ResponseEntity<?> pendientes(@PathVariable("codigo") String codigo, HttpServletRequest request) {
        Long actor = actor(codigo, request);
        if (actor == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(Map.of(
                "recibidas", intercambios.pendientes(codigo, actor).stream().map(this::dto).toList(),
                "enviadas", intercambios.enviadas(codigo, actor).stream().map(this::dto).toList()));
        } catch (RuntimeException ex) {
            return error(ex);
        }
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responder(@PathVariable("codigo") String codigo, @PathVariable("id") String id,
            @RequestBody Respuesta datos, HttpServletRequest request) {
        Long actor = actor(codigo, request);
        if (actor == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (datos.aceptar() == null) return ResponseEntity.badRequest().body(Map.of("error", "Indicá si aceptás el intercambio."));
        try {
            if (!datos.aceptar()) {
                Propuesta propuesta = intercambios.rechazar(codigo, id, actor);
                Map<String, Object> dto = new HashMap<>(dto(propuesta));
                dto.put("estado", "rechazado");
                publicar(codigo, dto);
                return ResponseEntity.ok(dto);
            }
            if (datos.posicionElegida() != null) {
                Map<String, Object> dto = dto(intercambios.elegirContrapropiedad(codigo, id, actor, datos.posicionElegida()));
                publicar(codigo, dto);
                return ResponseEntity.ok(dto);
            }
            return notificarAceptacion(codigo, intercambios.aceptar(codigo, id, actor));
        } catch (RuntimeException ex) {
            return error(ex);
        }
    }

    private ResponseEntity<?> notificarAceptacion(String codigo, ServicioIntercambioPropiedad.Resultado resultado) {
        List<Map<String, Object>> propiedades = List.of(resultado.ofrecida(), resultado.solicitada()).stream().map(c -> {
            Map<String, Object> dto = mapeador.desdePropiedad(c, c.getPropiedad());
            dto.put("intercambio", true);
            publicarEn("/topic/partida/" + codigo + "/propiedades", dto);
            return dto;
        }).toList();
        Map<String, Object> respuesta = new HashMap<>(dto(resultado.propuesta()));
        respuesta.put("estado", "aceptado");
        respuesta.put("propiedades", propiedades);
        publicar(codigo, respuesta);
        return ResponseEntity.ok(respuesta);
    }

    private Long actor(String codigo, HttpServletRequest request) {
        Partida partida = partidas.obtenerPartida(codigo);
        Long id = partida == null ? null : ResolutorJugadorSesion.resolverAutenticado(partida, request);
        return participacion.pertenece(partida, id) ? id : null;
    }

    private Map<String, Object> dto(Propuesta propuesta) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", propuesta.id());
        datos.put("idProponente", propuesta.idProponente());
        datos.put("idDestinatario", propuesta.idDestinatario());
        datos.put("posicionOfrecida", propuesta.posicionOfrecida());
        datos.put("posicionSolicitada", propuesta.posicionSolicitada());
        datos.put("nombreOfrecida", propuesta.nombreOfrecida());
        datos.put("nombreSolicitada", propuesta.nombreSolicitada());
        datos.put("apodoProponente", propuesta.apodoProponente());
        datos.put("vence", propuesta.vence().toEpochMilli());
        datos.put("estado", "pendiente");
        datos.put("requiereConfirmacion", propuesta.requiereConfirmacion());
        datos.put("confirmacionFinal", propuesta.esperaConfirmacionFinal());
        return datos;
    }

    private void publicar(String codigo, Map<String, Object> datos) {
        publicarEn("/topic/partida/" + codigo + "/intercambios", datos);
    }

    private void publicarEn(String destino, Map<String, Object> datos) {
        try {
            mensajes.convertAndSend(destino, datos);
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(ControladorIntercambioPropiedad.class)
                .warn("El intercambio se confirmó pero no se pudo notificar por WebSocket", ex);
        }
    }

    private ResponseEntity<?> error(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
            ex.getMessage() == null ? "No se pudo completar el intercambio." : ex.getMessage()));
    }
}
