package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;

@FunctionalInterface
public interface RepositorioEvento {
    EventoGlobal obtenerEventoGlobalAleatorio();
    
}
