package com.tallerwebi.dominio.servicios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Controla el vencimiento real de cada etapa, aunque falle el temporizador programado.
public final class RelojEventoGlobal {

    private final Map<String, Long> vencimientosPorPartida = new ConcurrentHashMap<>();

    public void iniciar(String codigoPartida, Integer duracionSegundos) {
        vencimientosPorPartida.put(codigoPartida, System.nanoTime() + TimeUnit.SECONDS.toNanos(duracionSegundos));
    }

    public boolean vencio(String codigoPartida) {
        Long vencimiento = vencimientosPorPartida.get(codigoPartida);
        return vencimiento != null && System.nanoTime() >= vencimiento;
    }

    public Integer obtenerSegundosRestantes(String codigoPartida) {
        Long vencimiento = vencimientosPorPartida.get(codigoPartida);
        Integer segundosRestantes = null;
        if (vencimiento != null) {
            long nanosegundosRestantes = Math.max(0L, vencimiento - System.nanoTime());
            segundosRestantes = (int) Math.ceil(nanosegundosRestantes / 1_000_000_000.0);
        }
        return segundosRestantes;
    }

    public void limpiar(String codigoPartida) {
        vencimientosPorPartida.remove(codigoPartida);
    }
}
