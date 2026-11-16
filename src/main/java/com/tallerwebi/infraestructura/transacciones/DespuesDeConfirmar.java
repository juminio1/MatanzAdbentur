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

    // Deshace un cambio en memoria hecho dentro de una transacción que terminó revertida.
    // Sin transacción no hay nada que revertir y la acción no se ejecuta.
    public static void siSeRevierte(Runnable accion) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) accion.run();
            }
        });
    }
}
