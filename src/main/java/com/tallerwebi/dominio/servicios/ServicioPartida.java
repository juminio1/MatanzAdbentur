package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import java.util.List;

public interface ServicioPartida {
    Partida obtenerPartida(String codigoUnico); //da la info del tablero y sus posiciones
    void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException;
    void restarDinero(Jugador jugador, Integer monto);
    void sumarDinero(Jugador jugador, Integer monto);
    void incrementarRondas(Partida partida);
    void inicializarTurnos(String codigoUnico);
    Jugador obtenerJugadorActual(String codigoUnico);
    void avanzarTurno(String codigoUnico);
    ResultadoTiradaDTO tirarDados();
    void moverJugador(Jugador jugador, Integer cantidadAvance, Integer totalCasilleros);
    List<PosicionFichaDTO> obtenerPosicionesFichas(String codigo);
}
