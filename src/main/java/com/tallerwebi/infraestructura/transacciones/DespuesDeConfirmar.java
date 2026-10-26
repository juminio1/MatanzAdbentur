package com.tallerwebi.infraestructura.transacciones;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Difiere efectos externos hasta que la transacción haya confirmado sus cambios. */
public final class DespuesDeConfirmar {
    private DespuesDeConfirmar() { }

    public static void ejecutar(Runnable accion) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
