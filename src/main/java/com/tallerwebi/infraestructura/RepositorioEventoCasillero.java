package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import java.util.List;

@FunctionalInterface
public interface RepositorioEventoCasillero {
    List<EventoCasillero> obtenerTodos();
}
