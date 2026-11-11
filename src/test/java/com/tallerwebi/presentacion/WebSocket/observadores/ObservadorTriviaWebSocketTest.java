package com.tallerwebi.presentacion.WebSocket.observadores;

import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservadorTriviaWebSocketTest {

    // Verifica que el observador reciba un evento de trivia y envíe su código de partida y contenido al notificador.
    @Test
    public void dadoUnEventoDeTriviaDelegaElCodigoYElContenidoAlNotificador() {
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        ObservadorTriviaWebSocket observador = new ObservadorTriviaWebSocket(notificador);
        Map<String, Object> contenido = Map.of("contenido", "notificacion de prueba");
        EventoTriviaActualizada evento = new EventoTriviaActualizada("ABC123", contenido);

        observador.actualizar(evento);

        verify(notificador, times(1)).notificarTrivia("ABC123", contenido);
    }
}
