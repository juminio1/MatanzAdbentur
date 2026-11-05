package com.tallerwebi.infraestructura;

import java.util.List;

import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;

@FunctionalInterface
public interface RepositorioEventoCasillero {
    List<EventoCasillero> obtenerTodos();
}
