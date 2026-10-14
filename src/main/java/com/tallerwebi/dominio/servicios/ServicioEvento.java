package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.eventos.Evento;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento);
    Evento obtenerEventoAleatorio();
}
