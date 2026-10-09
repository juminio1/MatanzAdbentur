package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;

public interface ServicioPropiedad {
    void comprarPropiedad(Long idPropiedad, Jugador jugador);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador);

}
