
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
//cuando recibe EventoJugadorQuebrado, le pide a ObservadorVictoria que verifique si la partida debe finalizar..
public class ObservadorVictoriaQuiebra
        implements Observador<EventoJugadorQuebrado> {

    private final ObservadorVictoria observadorVictoria;
    private final Consumer<EventoJugadorQuebrado> notificarQuiebra;

    public ObservadorVictoriaQuiebra(
            ObservadorVictoria observadorVictoria,
            Consumer<EventoJugadorQuebrado> notificarQuiebra) {
        this.observadorVictoria = observadorVictoria;
        this.notificarQuiebra = notificarQuiebra;
    }

    @Override
    public void actualizar(EventoJugadorQuebrado evento) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("La notificación de quiebra requiere una transacción activa");
        }
        observadorVictoria.verificarVictoria(evento.partida());
        // Avisamos solo después de confirmar también los cambios de los demás observadores.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificarQuiebra.accept(evento);
            }
        });
    }
}
