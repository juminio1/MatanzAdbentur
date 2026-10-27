package com.tallerwebi.infraestructura.temporizadores;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Programo el vencimiento de los turnos. */
@Component
public class GestorTemporizadorPartida {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public GestorTemporizadorPartida(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void programarTemporizador(
        String codigoUnico,
        Integer duracionSegundos,
        Runnable accionExpiracion
    ) {
        cancelarTemporizador(codigoUnico);

        ScheduledFuture<?> tarea = scheduler.schedule(() -> ejecutarAccionConTransaccion(accionExpiracion),duracionSegundos,TimeUnit.SECONDS);

        temporizadores.put(codigoUnico, tarea);
    }

    public synchronized Boolean programarTemporizadorSiNoExiste(
        String codigoUnico,
        Integer duracionSegundos,
        Runnable accionExpiracion
    ) {
        ScheduledFuture<?> tareaExistente = temporizadores.get(codigoUnico);
        if (tareaExistente != null && !tareaExistente.isDone()) {
            return false;
        }
        programarTemporizador(codigoUnico, duracionSegundos, accionExpiracion);
        return true;
    }

    private void ejecutarAccionConTransaccion(Runnable accionExpiracion) {
        // Abro una transacción para la tarea en segundo plano.
        try {
            transactionTemplate.executeWithoutResult(status -> accionExpiracion.run());
        } catch (Exception e) {
            throw new IllegalStateException(
                "Error al ejecutar accion diferida del temporizador",
                e
            );
        }
    }

    public void cancelarTemporizador(String codigoUnico) {
        ScheduledFuture<?> tareaExistente = temporizadores.remove(codigoUnico);

        if (tareaExistente != null && !tareaExistente.isDone()) {
            tareaExistente.cancel(false);
        }
    }

    public Integer obtenerSegundosRestantes(String codigoUnico) {
        ScheduledFuture<?> tarea = temporizadores.get(codigoUnico);
        if (tarea == null) {
            return null;
        }
        if (tarea.isDone()) return 0;
        Long milisegundos = tarea.getDelay(TimeUnit.MILLISECONDS);
        Long segundos = Math.max(0, (milisegundos + 999) / 1000);
        return segundos.intValue();
    }
}
