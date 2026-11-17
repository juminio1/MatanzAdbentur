package com.tallerwebi.dominio.servicios.interfaces;
import com.tallerwebi.dominio.resultados.ResultadoJugada;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;

public interface ServicioJugada {
    ResultadoJugada tirarYMover(String codigo, Long idJugador) throws PartidaNoEncontradaException, JugadorNoEncontradoException;
    // La usa el temporizador cuando se acaba el tiempo y el jugador todavía no tiró.
    ResultadoJugada tirarPorVencimiento(String codigo, Long idJugador) throws PartidaNoEncontradaException, JugadorNoEncontradoException;
    void pagarFianzaVoluntaria(String codigo, Long idJugador) throws PartidaNoEncontradaException, JugadorNoEncontradoException;
}
