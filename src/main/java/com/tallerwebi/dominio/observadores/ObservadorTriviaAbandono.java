package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.servicios.GestorTriviaPartida;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ObservadorTriviaAbandono implements Observador<EventoJugadorAbandonado> {
    private final GestorTriviaPartida gestor;

    // Lazy deja la creación del gestor para cuando se use, evitando un ciclo al conectar los avisos de abandono.
    public ObservadorTriviaAbandono(@Lazy GestorTriviaPartida gestor) {
        this.gestor = gestor;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        String codigo = evento.partida().getCodigoUnico();
        DespuesDeConfirmar.ejecutar(() -> gestor.participantesCambiaron(codigo));
    }
}
