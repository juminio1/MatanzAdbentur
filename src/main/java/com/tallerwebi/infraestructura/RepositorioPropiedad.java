package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Propiedad;

public interface RepositorioPropiedad {
    void guardarPropiedad(Propiedad propiedad);
    Propiedad buscarPropiedadPorId(Long id);
}
