package com.tallerwebi.dominio.resultados;
import java.util.List;
/** Resultado de dominio independiente del transporte. */
public record ResultadoJugada(Long idJugador, Integer posicionAnterior, Integer nuevaPosicion,
        Integer posicionCaidaEvento, ResultadoTirada tirada, EventoAplicado evento,
        Integer dineroPorInicio, List<SaldoJugador> saldos, AlquilerJugada alquiler) {
    public record EventoAplicado(String nombre, String descripcion, String tipoEfecto) { }
}
