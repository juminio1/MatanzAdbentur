package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;

@FunctionalInterface
public interface ServicioPropiedad {
    void comprarPropiedad(Long idPropiedad, Jugador jugador);
}
