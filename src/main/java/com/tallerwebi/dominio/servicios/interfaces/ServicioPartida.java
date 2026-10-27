package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.resultados.PosicionFicha;
import java.util.List;

public interface ServicioPartida {
    Partida obtenerPartida(String codigoUnico); //da la info del tablero y sus posiciones
    void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException;
    void actualizarJugador(Jugador jugador);
    void incrementarRondas(Partida partida);
    void inicializarTurnos(String codigoUnico);
    void abandonarPartida(String codigoUnico, Long idJugador) throws PartidaNoEncontradaException,  ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    Jugador obtenerJugadorActual(String codigoUnico);
    Integer obtenerSegundosRestantesTurno(String codigoUnico);
    void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico);
    void prepararTiempoTurno(String codigoUnico, Integer segundosPresentacion);
    Jugador obtenerJugadorPorId(Long idJugador);
    void avanzarTurno(String codigoUnico) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    List<PosicionFicha> obtenerPosicionesFichas(String codigo);
    Boolean verificarSiHayEventoGlobalActivo(String codigoUnico);
}
