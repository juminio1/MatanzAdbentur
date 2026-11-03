package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.EventoGlobal;
import java.util.List;

/** Consulta el catálogo global sin mezclar eventos de casillero. */
@FunctionalInterface
public interface RepositorioEventoGlobal {
    List<EventoGlobal> obtenerTodos();
}
