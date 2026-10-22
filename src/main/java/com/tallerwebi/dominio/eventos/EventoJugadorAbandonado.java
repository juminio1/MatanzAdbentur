package com.tallerwebi.dominio.eventos;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

public record EventoJugadorAbandonado(Partida partida,Jugador jugador) {}