package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento;
import com.tallerwebi.dominio.entidades.Jugador;

public interface ServicioEvento {
    void aplicarEvento(Jugador jugador, Evento evento);
    Evento obtenerEventoAleatorio();
}
