package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.InicioPartidaNoAutorizadoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.PartidaSinJugadoresException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import java.util.List;

public interface ServicioPartida {
    Partida obtenerPartida(String codigoUnico); //da la info del tablero y sus posiciones
    void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException;
    void restarDinero(Jugador jugador, Integer monto);
    void sumarDinero(Jugador jugador, Integer monto);
    void actualizarJugador(Jugador jugador);
    void incrementarRondas(Partida partida);
    void inicializarTurnos(String codigoUnico) throws PartidaIniciadaException;
    Jugador obtenerJugadorActual(String codigoUnico) throws PartidaIniciadaException;
    Integer obtenerSegundosRestantesTurno(String codigoUnico);
    void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico) throws PartidaNoEncontradaException, PartidaIniciadaException;
    Jugador obtenerJugadorPorId(Long idJugador);
    void avanzarTurno(String codigoUnico) throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException;
    ResultadoTiradaDTO tirarDados();
    void moverJugador(Jugador jugador, Integer cantidadAvance, Integer totalCasilleros);
    List<PosicionFichaDTO> obtenerPosicionesFichas(String codigo) throws PartidaIniciadaException, PartidaNoEncontradaException, EstadoPartidaInvalidoException, PartidaSinJugadoresException;
}
