package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.resultados.ResultadoEvento;
import com.tallerwebi.dominio.entidades.Jugador;
import java.util.Optional;

@FunctionalInterface
public interface ServicioEventoCasillero {
    Optional<ResultadoEvento> aplicarSiCorresponde(
            String codigoPartida,
            Jugador jugador);
}
