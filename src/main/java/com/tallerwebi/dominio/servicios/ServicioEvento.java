package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento);
    Evento obtenerEventoAleatorio();
    void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException;
    void verificarActivacionEventoGlobal(Partida partida);
    Evento obtenerEventosGlobalesAleatorio();
}
