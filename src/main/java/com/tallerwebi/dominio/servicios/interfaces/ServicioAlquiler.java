package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;

public interface ServicioAlquiler {
    void pagarAlquiler(Propiedad propiedad, Jugador jugador);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler);
    Integer calcularAlquiler(Propiedad propiedad);
    Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad);
}
