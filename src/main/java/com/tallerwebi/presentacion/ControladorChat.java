package com.tallerwebi.presentacion;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@ResponseBody
@RequestMapping("/partida/{codigo}/chat")
public class ControladorChat {
    private final ServicioPartida partidas;
    private final ServicioChat chat;
    private final SimpMessagingTemplate mensajes;
    public record Solicitud(String texto) { }
    public ControladorChat(ServicioPartida partidas, ServicioChat chat, SimpMessagingTemplate mensajes) {
        this.partidas = partidas; this.chat = chat; this.mensajes = mensajes;
    }
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable("codigo") String codigo, HttpServletRequest request) {
        Long actor = actor(codigo, request);
        if (actor == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(chat.listar(codigo, actor));
    }
    @PostMapping
    public ResponseEntity<?> enviar(@PathVariable("codigo") String codigo, @RequestBody Solicitud solicitud, HttpServletRequest request) {
        Long actor = actor(codigo, request);
        if (actor == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            ServicioChat.Mensaje mensaje = chat.enviar(codigo, actor, solicitud.texto());
            // El servicio ya confirmó la escritura. Una falla de conexión se recupera con la consulta.
            try { mensajes.convertAndSend("/topic/partida/" + codigo + "/chat", mensaje); }
            catch (RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(ControladorChat.class).warn("No se pudo notificar el mensaje de chat", ex); }
            return ResponseEntity.ok(mensaje);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    private Long actor(String codigo, HttpServletRequest request) {
        return ResolutorJugadorSesion.resolverAutenticado(partidas.obtenerPartida(codigo), request);
    }
}
