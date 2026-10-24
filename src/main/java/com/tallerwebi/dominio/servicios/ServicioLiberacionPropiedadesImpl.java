
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioLiberacionPropiedades;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//Devuelve las propiedades al banco y elimina sus mejoras.
@Service
@Transactional
public class ServicioLiberacionPropiedadesImpl
        implements ServicioLiberacionPropiedades {

    private final RepositorioPropiedad repositorioPropiedad;

    public ServicioLiberacionPropiedadesImpl(
            RepositorioPropiedad repositorioPropiedad) {
        this.repositorioPropiedad = repositorioPropiedad;
    }

    @Override
    public void liberarPropiedades(Jugador jugador) {

        List<Propiedad> propiedades =
                repositorioPropiedad.buscarPropiedadesPorPropietario(jugador);

        if (propiedades == null) {
            return;
        }

        for (Propiedad propiedad : propiedades) {
            propiedad.setPropietario(null);
            propiedad.setNivelMejora(0);
            repositorioPropiedad.guardarPropiedad(propiedad);
        }
    }
}
