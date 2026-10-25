package com.tallerwebi.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorSalaWebSocket;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ObservadoresConfigTest {

    @Test
    void springConectaCadaPublicadorConSuObservadorYConservaLosMensajes() {
        NotificadorSala sala = mock(NotificadorSala.class);
        NotificadorPartida partida = mock(NotificadorPartida.class);
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(NotificadorSala.class, () -> sala);
            contexto.registerBean(NotificadorPartida.class, () -> partida);
            contexto.register(ObservadoresConfig.class, ObservadorSalaWebSocket.class,
                    ObservadorTurnoWebSocket.class, PublicadoresInyectados.class);
            contexto.refresh();
            PublicadoresInyectados publicadores = contexto.getBean(PublicadoresInyectados.class);

            publicadores.sala.notificar(new EventoSalaActualizada("ABC123", List.of("Ana"),
                    List.of("ROJO"), Map.of("Ana", "ROJO"), "Ana"));

            verify(sala).notificarSalaActualizada(argThat(estado ->
                    "ABC123".equals(estado.getCodigo()) && "Ana".equals(estado.getCreador())
                    && estado.getUsernames().equals(List.of("Ana"))
                    && estado.getFichasOcupadas().equals(List.of("ROJO"))
                    && estado.getFichasAsignadas().equals(Map.of("Ana", "ROJO"))));
            verifyNoInteractions(partida);

            publicadores.turno.notificar(new EventoTurnoCambiado("ABC123", 1L, "Ana", 0, 30, 2, 1));

            verify(partida).notificarCambioDeTurno(eq("ABC123"), argThat(turno ->
                    "ABC123".equals(turno.getCodigoPartida()) && Long.valueOf(1L).equals(turno.getIdJugadorActual())
                    && "Ana".equals(turno.getApodoJugadorActual()) && Integer.valueOf(0).equals(turno.getOrden())
                    && "Es el turno de Ana".equals(turno.getMensaje())
                    && Integer.valueOf(30).equals(turno.getDuracionSegundos())
                    && Integer.valueOf(2).equals(turno.getRondaActual())
                    && Integer.valueOf(1).equals(turno.getTurnosFinalizadosEnRonda())));
        }
    }

    static class PublicadoresInyectados {
        @Autowired
        private PublicadorEventos<EventoSalaActualizada> sala;

        @Autowired
        private PublicadorEventos<EventoTurnoCambiado> turno;
    }
}
