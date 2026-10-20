package com.tallerwebi.config;

import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registro los observadores de sala y turno. */
@Configuration
public class ObservadoresConfig {

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
}
