package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;

@FunctionalInterface
public interface RepositorioEvento {
    EventoGlobal obtenerEventoGlobalAleatorio();
    
}
