package com.tallerwebi.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.dominio.observadores.ObservadorVictoriaQuiebra;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorSalaWebSocket;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ObservadoresConfigTest {

    @Test
    void springConectaElAvisoDeQuiebraAlNotificadorExistenteDespuesDelCommit() throws BeansException, ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        ObservadorVictoria victoria = mock(ObservadorVictoria.class);
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(NotificadorPartida.class, () -> notificador);
            contexto.registerBean(ObservadorVictoria.class, () -> victoria);
            contexto.register(ObservadoresConfig.class, ObservadorVictoriaQuiebra.class, PublicadoresInyectados.class);
            contexto.refresh();
            Partida partida = new Partida();
            Jugador jugador = new Jugador();
            jugador.setId(1L);
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                contexto.getBean(PublicadoresInyectados.class).quiebra
                        .notificar(new EventoJugadorQuebrado(partida, jugador));
                verify(victoria).verificarVictoria(partida);
                verifyNoInteractions(notificador);
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit);
                verify(notificador).notificarQuiebraPorWebSocket(partida, 1L);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }

    @Test
    void springConectaCadaPublicadorConSuObservadorYConservaLosMensajes() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
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
        private PublicadorEventos<EventoJugadorQuebrado> quiebra;
        @Autowired
        private PublicadorEventos<EventoSalaActualizada> sala;

        @Autowired
        private PublicadorEventos<EventoTurnoCambiado> turno;
    }
}
