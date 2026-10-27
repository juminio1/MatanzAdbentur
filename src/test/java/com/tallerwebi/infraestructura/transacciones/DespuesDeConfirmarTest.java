package com.tallerwebi.infraestructura.transacciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
class DespuesDeConfirmarTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void ejecutaLaAccionInmediatamenteCuandoNoHayTransaccionActiva() {
        AtomicInteger ejecuciones = new AtomicInteger();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());

        DespuesDeConfirmar.ejecutar(ejecuciones::incrementAndGet);

        assertEquals(1, ejecuciones.get());
    }

    @Test
    void ejecutaLaAccionUnaSolaVezDespuesDelCommit() {
        List<String> pasos = new ArrayList<>();

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    assertTrue(pasos.isEmpty());
                    pasos.add("antes del commit");
                }

                @Override
                public void afterCompletion(int status) {
                    assertEquals(STATUS_COMMITTED, status);
                    assertEquals(List.of("antes del commit", "accion"), pasos);
                }
            });

            DespuesDeConfirmar.ejecutar(() -> pasos.add("accion"));

            assertTrue(pasos.isEmpty());
        });

        assertEquals(List.of("antes del commit", "accion"), pasos);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    @Test
    void noEjecutaLaAccionCuandoLaTransaccionHaceRollback() {
        AtomicInteger ejecuciones = new AtomicInteger();
        AtomicInteger resultado = new AtomicInteger(-1);

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    resultado.set(status);
                }
            });

            DespuesDeConfirmar.ejecutar(ejecuciones::incrementAndGet);
            assertEquals(0, ejecuciones.get());
            estado.setRollbackOnly();
        });

        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, resultado.get());
        assertEquals(0, ejecuciones.get());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }
}
