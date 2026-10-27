package com.tallerwebi.dominio.servicios.interfaces;
import com.tallerwebi.dominio.resultados.ResultadoJugada;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
@FunctionalInterface
public interface ServicioJugada {
    ResultadoJugada tirarYMover(String codigo, Long idJugador) throws PartidaNoEncontradaException, JugadorNoEncontradoException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, EventoGlobalActivoException, EventoInexistenteException;
}
