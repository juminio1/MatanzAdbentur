package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento);
    Evento obtenerEventoAleatorio();
    void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException;
    void verificarActivacionEventoGlobal(Partida partida) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException;
    Evento obtenerEventosGlobalesAleatorio();
   ResultadoTiradaDTO realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException;
    Boolean hayEventoGlobalActivo(String codigoUnico);
}
