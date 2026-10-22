package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import java.util.Optional;

@FunctionalInterface
public interface ServicioEventoCasillero {
    Optional<EventoCasillero> aplicarSiCorresponde(Jugador jugador);
}
