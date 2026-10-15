package com.tallerwebi.dominio.servicios;

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

/**
 * Componente encargado de gestionar temporizadores asíncronos para los turnos de las partidas.
 *
 * ¿Por qué se creó esta clase separada?
 * 1. Mantiene la baja complejidad ciclomática en ServicioPartidaImpl (cumpliendo con PMD).
 * 2. Permite ejecutar tareas en hilos secundarios (background) de forma segura.
 * 3. Proporciona contexto transaccional a la ejecución diferida mediante TransactionTemplate.
 */
@Component
public class GestorTemporizadorPartida {

    /**
     * Executor Service que maneja un pool de 4 hilos en segundo plano para agendar tareas diferidas.
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Mapa concurrente clave-valor (codigoPartida -> tareaProgramada).
     * Permite rastrear y cancelar el temporizador activo de cada partida individual.
     */
    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();

    /**
     * Herramienta de Spring para abrir transacciones programmaticamente en hilos secundarios.
     */
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public GestorTemporizadorPartida(PlatformTransactionManager transactionManager) {
        // Inicializamos el template inyectando el administrador de transacciones de Spring
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Programa una acción automática que se ejecutará una vez transcurridos los segundos especificados.
     *
     * @param codigoUnico Identificador único de la partida.
     * @param duracionSegundos Tiempo de espera en segundos (30s).
     * @param accionExpiracion Expresión Lambda (Runnable) que contiene la acción a ejecutar (avanzarTurno).
     */
    public void programarTemporizador(
        String codigoUnico,
        int duracionSegundos,
        Runnable accionExpiracion
    ) {
        // 1. Si ya había una cuenta regresiva previa para esta partida, la cancelamos primero
        cancelarTemporizador(codigoUnico);

        // 2. Agendamos la tarea en un hilo secundario del scheduler para dentro de 'duracionSegundos'
        ScheduledFuture<?> tarea = scheduler.schedule(
            () -> {
                ejecutarAccionConTransaccion(accionExpiracion);
            },
            duracionSegundos,
            TimeUnit.SECONDS
        );

        // 3. Guardamos la referencia de la tarea en el mapa concurrente
        temporizadores.put(codigoUnico, tarea);
    }

    /** Inicia el reloj solo si la partida todavía no tiene un turno temporizado. */
    public synchronized boolean programarTemporizadorSiNoExiste(
        String codigoUnico,
        int duracionSegundos,
        Runnable accionExpiracion
    ) {
        ScheduledFuture<?> tareaExistente = temporizadores.get(codigoUnico);
        if (tareaExistente != null && !tareaExistente.isDone()) {
            return false;
        }
        programarTemporizador(codigoUnico, duracionSegundos, accionExpiracion);
        return true;
    }

    /**
     * Ejecuta la acción diferida dentro de un contexto de transacción activo de Spring/Hibernate.
     *
     * NOTA: Al ejecutarse en un hilo secundario del ScheduledExecutorService, el hilo NO hereda
     * la sesión/transacción del hilo principal HTTP. TransactionTemplate abre una transacción
     * nueva y limpia para permitir consultas y guardados en la base de datos sin fallos.
     */
    private void ejecutarAccionConTransaccion(Runnable accionExpiracion) {
        try {
            transactionTemplate.executeWithoutResult(status -> accionExpiracion.run());
        } catch (Exception e) {
            // Se relanza como IllegalStateException para cumplir con reglas de PMD (evita bloques catch vacíos y System.out/err)
            throw new IllegalStateException(
                "Error al ejecutar accion diferida del temporizador",
                e
            );
        }
    }

    /**
     * Cancela la tarea agendada en background para una partida si el jugador realizó una acción a tiempo.
     *
     * @param codigoUnico Identificador único de la partida.
     */
    public void cancelarTemporizador(String codigoUnico) {
        // Removemos la tarea guardada en el mapa
        ScheduledFuture<?> tareaExistente = temporizadores.remove(codigoUnico);

        // Si la tarea existía y no había terminado todavía, la cancelamos
        if (tareaExistente != null && !tareaExistente.isDone()) {
            tareaExistente.cancel(false);
        }
    }

    /** Devuelve los segundos que quedan en el temporizador activo de una partida. */
    public Integer obtenerSegundosRestantes(String codigoUnico) {
        ScheduledFuture<?> tarea = temporizadores.get(codigoUnico);
        if (tarea == null) {
            return null;
        }
        if (tarea.isDone()) return 0;
        long milisegundos = tarea.getDelay(TimeUnit.MILLISECONDS);
        return (int) Math.max(0, (milisegundos + 999) / 1000);
    }
}
