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
public class GestorTemporizadorPartida implements org.springframework.beans.factory.DisposableBean {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();
    private final Map<String, Long> revisionesEventos = new ConcurrentHashMap<>();

    private final Map<String, Integer> pausados = new ConcurrentHashMap<>();

    public synchronized void pausar(String codigo) {
        if (pausados.containsKey(codigo)) return;
        Integer restantes = obtenerSegundosRestantes(codigo);
        cancelarTemporizador(codigo);
        pausados.put(codigo, restantes == null ? 60 : restantes);
    }

    public synchronized Integer consumirPausa(String codigo) {
        return pausados.remove(codigo);
    }

    public boolean estaPausado(String codigo) { return pausados.containsKey(codigo); }

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

        ScheduledFuture<?> tarea = scheduler.schedule(
            () -> ejecutarAccionConTransaccion(accionExpiracion),
            duracionSegundos,
            TimeUnit.SECONDS
        );

        temporizadores.put(codigoUnico, tarea);
    }

    public synchronized Boolean programarTemporizadorSiNoExiste(
        String codigoUnico,
        Integer duracionSegundos,
        Runnable accionExpiracion
    ) {
        ScheduledFuture<?> tareaExistente = temporizadores.get(codigoUnico);
        if (pausados.containsKey(codigoUnico)) return false;
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

    public synchronized void programarTemporizadorEvento(String clave, long revision,
            int segundos, Runnable accion) {
        long anterior = revisionesEventos.getOrDefault(clave, -1L);
        if (revision < anterior) return;
        ScheduledFuture<?> vigente = temporizadores.get(clave);
        if (revision == anterior && vigente != null && !vigente.isDone()) return;
        revisionesEventos.put(clave, revision);
        programarTemporizador(clave, segundos, accion);
    }

    public synchronized void cancelarTemporizadorEvento(String clave, long revision) {
        if (revision < revisionesEventos.getOrDefault(clave, -1L)) return;
        revisionesEventos.put(clave, revision);
        cancelarTemporizador(clave);
    }

    public Integer obtenerSegundosRestantes(String codigoUnico) {
        if (pausados.containsKey(codigoUnico)) return pausados.get(codigoUnico);
        ScheduledFuture<?> tarea = temporizadores.get(codigoUnico);
        if (tarea == null) {
            return null;
        }
        if (tarea.isDone()) return 0;
        Long milisegundos = tarea.getDelay(TimeUnit.MILLISECONDS);
        Long segundos = Math.max(0, (milisegundos + 999) / 1000);
        return segundos.intValue();
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        temporizadores.clear();
        pausados.clear();
        revisionesEventos.clear();
    }
}
