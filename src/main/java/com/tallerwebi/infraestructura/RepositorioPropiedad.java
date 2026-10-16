package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;

import java.util.List;

public interface RepositorioPropiedad {
    void guardarPropiedad(Propiedad propiedad);
    Propiedad buscarPropiedadPorId(Long id);
    List<Propiedad> buscarPropiedadesPorGrupo(GrupoPropiedades grupo);
}
