package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Jugador;

public interface ServicioFinalizacionPartida {
    Integer calcularValorPatrimonial(Propiedad propiedad);
    Integer calcularPatrimonioTotal(String codigoPartida, Jugador jugador);
}
