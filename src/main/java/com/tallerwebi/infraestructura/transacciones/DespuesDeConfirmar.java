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

    // Mantiene una operación reservada hasta el commit; libera la reserva también ante un rollback.
    public static void ejecutarConConfirmacion(Runnable operacion, Runnable alConfirmar, Runnable alFinalizar) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                operacion.run();
                alConfirmar.run();
            } finally {
                alFinalizar.run();
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                alConfirmar.run();
            }

            @Override
            public void afterCompletion(int status) {
                alFinalizar.run();
            }
        });
        operacion.run();
    }
}
