package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.eventos.Evento;

@FunctionalInterface
public interface RepositorioEvento {
    Evento obtenerEventoAleatorio();
}
