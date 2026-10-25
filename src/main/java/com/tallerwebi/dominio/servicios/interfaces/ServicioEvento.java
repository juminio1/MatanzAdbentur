package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.excepcion.TiradasFaltantesException;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento) throws EventoInexistenteException;
    Evento obtenerEventoAleatorio();
    void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    void verificarActivacionEventoGlobal(Partida partida) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, ActivacionEventoInvalidoException,EventoInexistenteException;
    Evento obtenerEventosGlobalesAleatorio();
   ResultadoTiradaDTO realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException,TiradasFaltantesException, EventoInexistenteException;
    Boolean hayEventoGlobalActivo(String codigoUnico);
}
