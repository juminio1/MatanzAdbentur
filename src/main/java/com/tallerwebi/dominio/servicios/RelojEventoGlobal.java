package com.tallerwebi.dominio.servicios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Controla el vencimiento real de cada etapa, aunque falle el temporizador programado.
public final class RelojEventoGlobal {

    private final Map<String, Long> vencimientosPorPartida = new ConcurrentHashMap<>();

    public Long iniciar(String codigoPartida, Integer duracionSegundos) {
        Long vencimiento = System.nanoTime() + TimeUnit.SECONDS.toNanos(duracionSegundos);
        vencimientosPorPartida.put(codigoPartida, vencimiento);
        return vencimiento;
    }

    public Long obtenerVencimiento(String codigoPartida) {
        return vencimientosPorPartida.get(codigoPartida);
    }

    public Boolean correspondeAlVencimiento(String codigoPartida, Long vencimiento) {
        return vencimiento != null && vencimiento.equals(obtenerVencimiento(codigoPartida));
    }

    public Boolean vencio(String codigoPartida) {
        Long vencimiento = vencimientosPorPartida.get(codigoPartida);
        return vencimiento != null && System.nanoTime() >= vencimiento;
    }

    public Boolean vencioOProgramacionAgotada(String codigoPartida, Integer segundosProgramados) {
        return vencio(codigoPartida) || Integer.valueOf(0).equals(segundosProgramados);
    }

    public Integer obtenerSegundosConRespaldo(String codigoPartida, Boolean enPausaEmpate,
            Integer segundosProgramados, Integer duracionPorDefecto) {
        Integer segundos = segundosProgramados;
        if (segundos == null && !enPausaEmpate) {
            segundos = obtenerSegundosRestantes(codigoPartida);
        }
        return segundos == null ? duracionPorDefecto : segundos;
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
