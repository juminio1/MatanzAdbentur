package com.tallerwebi.config;

import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registro los observadores de sala y turno. */
@Configuration
public class ObservadoresConfig {

    // Conecta el aviso de quiebra sin acoplar el observador a la presentación.
    @Bean
    public Consumer<EventoJugadorQuebrado> notificacionQuiebra(NotificadorPartida notificador) {
        return evento -> notificador.notificarQuiebraPorWebSocket(evento.partida(), evento.jugador().getId());
    }

    @Bean
    public PublicadorEventos<EventoSalaActualizada> publicadorSala(
        List<Observador<EventoSalaActualizada>> observadores
    ) {
        return new PublicadorEventos<>(observadores);
    }

    @Bean
    public PublicadorEventos<EventoTurnoCambiado> publicadorTurno(
        List<Observador<EventoTurnoCambiado>> observadores
    ) {
        return new PublicadorEventos<>(observadores);
    }

    @Bean
    public PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono(
        List<Observador<EventoJugadorAbandonado>> observadores
    ) {
        return new PublicadorEventos<>(observadores);
    }

    //indica a Spring cómo crear el publicador que nuestro servicio necesita.
    @Bean
    public PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra(
            List<Observador<EventoJugadorQuebrado>> observadores) {
        return new PublicadorEventos<>(observadores);
    }
}
