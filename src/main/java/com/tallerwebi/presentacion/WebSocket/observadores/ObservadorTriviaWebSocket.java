package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.mapeadores.MapeadorTrivia;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTriviaWebSocket implements Observador<EventoTriviaActualizada> {

    private final NotificadorPartida notificadorPartida;

    public ObservadorTriviaWebSocket(NotificadorPartida notificadorPartida) {
        this.notificadorPartida = notificadorPartida;
    }

    @Override
    public void actualizar(EventoTriviaActualizada evento) {
        if (evento.tipo() == TipoAvisoTrivia.FINALIZADA || evento.tipo() == TipoAvisoTrivia.PREMIO_ENTREGADO) {
            Map<String, Object> contenido = Map.of(
                    "tipo", evento.tipo(),
                    "estado", MapeadorTrivia.estado(evento.codigoPartida(), evento.trivia()));
            notificadorPartida.notificarTrivia(evento.codigoPartida(), contenido);
            return;
        }
        if (evento.tipo() == TipoAvisoTrivia.RONDA_CERRADA) {
            notificadorPartida.notificarTrivia(evento.codigoPartida(), MapeadorTrivia.cierre(evento));
            return;
        }
        if (evento.tipo() != TipoAvisoTrivia.RONDA_NUEVA) return;
        Map<String, Object> contenido = Map.of(
                "tipo", evento.tipo(),
                "estado", MapeadorTrivia.estado(evento.codigoPartida(), evento.trivia()),
                "pregunta", MapeadorTrivia.pregunta(evento.trivia()));
        notificadorPartida.notificarTrivia(evento.codigoPartida(), contenido);
    }
}
