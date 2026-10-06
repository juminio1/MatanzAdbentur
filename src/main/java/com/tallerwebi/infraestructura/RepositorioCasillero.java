package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import java.util.List;

public interface RepositorioCasillero {
    List<Casillero> obtenerTodos();
}
