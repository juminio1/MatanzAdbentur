package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.excepcion.TiradasFaltantesException;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento) throws EventoInexistenteException;
    Evento obtenerEventoAleatorio();
    void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    void verificarActivacionEventoGlobal(Partida partida)throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    Evento obtenerEventosGlobalesAleatorio();
   ResultadoTirada realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException,TiradasFaltantesException, EventoInexistenteException;
    EstadoEventoGlobal consultarEstadoEventoGlobal(String codigoPartida);
    ResultadoTirada tirarDadosEventoGlobal(String codigoPartida, Long idJugador) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException;
    Boolean verificarSiHayEventoGlobalActivo(String codigoUnico);
    void reanudarPartidaTrasEvento(String codigoPartida);
    void confirmarContinuacion(String codigoPartida, Long idJugador);
    void realizarTiradasAutomaticasPendientes(Partida partida) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException;
}
