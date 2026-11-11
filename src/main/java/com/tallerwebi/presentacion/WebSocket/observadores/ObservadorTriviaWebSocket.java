package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTriviaWebSocket implements Observador<EventoTriviaActualizada> {

    private final NotificadorPartida notificadorPartida;

    public ObservadorTriviaWebSocket(NotificadorPartida notificadorPartida) {
        this.notificadorPartida = notificadorPartida;
    }

    //recibe el evento de la trivia, obtiene el codigo de la partida y obtiene el msj.
    // Llama al notificadorTrivia() para publicarlo por websockets
    @Override
    public void actualizar(EventoTriviaActualizada evento) {
        notificadorPartida.notificarTrivia(evento.codigoPartida(), evento.contenido());
    }
}
