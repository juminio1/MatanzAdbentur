package com.tallerwebi.dominio.eventos;

import com.tallerwebi.dominio.resultados.ResultadoJugada;

public record EventoTiradaAutomatica(String codigoPartida, ResultadoJugada resultado) { }
