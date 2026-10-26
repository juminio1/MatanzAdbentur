package com.tallerwebi.infraestructura.transacciones;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

//espera a la confirmacion del jugador en un evento, para aplicar dicho efecto (Suma/restar plata, etc)
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
