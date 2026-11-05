package com.tallerwebi.infraestructura;

import java.util.List;

import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;

/** Consulta el catálogo global sin mezclar eventos de casillero. */
@FunctionalInterface
public interface RepositorioEventoGlobal {
    List<EventoGlobal> obtenerTodos();
}
