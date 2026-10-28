package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.eventos.Evento;

public interface RepositorioEvento {
    Evento obtenerEventoAleatorio();
    Evento obtenerEventoGlobalAleatorio();
    
}
