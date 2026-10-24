package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificadorPartida {

    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private static final Integer UNICO_JUGADOR = 1;
    private final SimpMessagingTemplate template;

    @Autowired
    public NotificadorPartida(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notificarCambioDeTurno(String codigoPartida, TurnoDTO turno) {
        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/turno", turno);
    }

    public void notificarMovimiento(String codigoPartida, RespuestaMovimientoDTO movimiento) {
        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida, movimiento);
    }

    public static void notificarSaldos(
        SimpMessagingTemplate template,
        ServicioPartida servicioPartida,
        String codigoPartida
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || partida.getJugadores() == null) return;
        for (Jugador jugador : partida.getJugadores()) {
            Map<String, Object> saldo = new HashMap<>();
            saldo.put("idJugador", jugador.getId());
            saldo.put("dinero", jugador.getDinero());
            saldo.put("enBancarrota", jugador.getEnBancarrota());
            template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/dinero", saldo);
        }
    }

    public static void notificarAlquiler(
        SimpMessagingTemplate template,
        String codigoPartida,
        Jugador pagador,
        Jugador propietario,
        String nombrePropiedad,
        Integer monto
    ) {
        Map<String, Object> aviso = new HashMap<>();
        aviso.put("idJugadorPagador", pagador.getId());
        aviso.put("pagador", pagador.getApodo());
        aviso.put("propietario", propietario.getApodo());
        aviso.put("propiedad", nombrePropiedad);
        aviso.put("monto", monto);
        template.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/alquiler", aviso);
    }

    // Reutiliza el canal de salida sin presentar la quiebra como un abandono.
    public void notificarQuiebraPorWebSocket(Partida partida, Long idJugador) {
        Jugador quebrado = partida.getJugadores().stream()
                .filter(jugador -> idJugador.equals(jugador.getId())).findFirst().orElseThrow();
        Map<String, Object> datos = new HashMap<>();
        datos.put("motivo", "QUIEBRA");
        datos.put("idJugador", idJugador);
        datos.put("apodo", quebrado.getApodo());
        datos.put("partidaFinalizada", partida.getEstado() == EstadoPartida.FINALIZADA);
        if (partida.getEstado() == EstadoPartida.FINALIZADA) {
            // Observer ya decidió la victoria; aquí solo identificamos al ganador para mostrarlo.
            partida.getJugadores().stream()
                    .filter(jugador -> !Boolean.TRUE.equals(jugador.getAbandono()))
                    .filter(jugador -> !Boolean.TRUE.equals(jugador.getEnBancarrota()))
                    .filter(jugador -> !idJugador.equals(jugador.getId()))
                    .findFirst().ifPresent(ganador -> datos.put("idGanador", ganador.getId()));
        }
        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + partida.getCodigoUnico() + "/abandono", datos);
    }

    public void notificarAbandonoPorWebSocket(Partida partida, Jugador jugadorQueAbandona) {
        List<Jugador> jugadoresRestantes = partida.getJugadores().stream().filter(jugador -> !jugador.getId().equals(jugadorQueAbandona.getId())).filter(jugador -> !Boolean.TRUE.equals(jugador.getAbandono())).toList();
        //obtiene todos los jugadores que siguen en la partida

        Map<String, Object> datos = new HashMap<>();

        datos.put("idJugador", jugadorQueAbandona.getId());
        datos.put("apodo", jugadorQueAbandona.getApodo()); //envia el id y el apodo de quien abandona

        Boolean victoriaPorAbandono = jugadoresRestantes.size() == UNICO_JUGADOR;

        datos.put("victoriaPorAbandono", victoriaPorAbandono);

        if (victoriaPorAbandono) {
            datos.put("idGanador", jugadoresRestantes.get(0).getId());
        }

        this.template.convertAndSend(TOPIC_PARTIDA_PREFIX + partida.getCodigoUnico() + "/abandono", datos);
    }
}
