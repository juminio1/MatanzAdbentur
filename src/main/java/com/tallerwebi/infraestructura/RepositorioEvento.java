package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Evento;

@FunctionalInterface
public interface RepositorioEvento {
    Evento obtenerEventoAleatorio();
}
