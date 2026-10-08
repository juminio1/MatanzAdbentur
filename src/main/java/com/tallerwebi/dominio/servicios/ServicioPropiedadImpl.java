package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPropiedad")
@Transactional
public class ServicioPropiedadImpl implements ServicioPropiedad {
    private RepositorioPropiedad repositorioPropiedad;

    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad) {
        this.repositorioPropiedad = repositorioPropiedad;
    }

    @Override
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        Propiedad propiedadEncontrada = repositorioPropiedad.buscarPropiedadPorId(idPropiedad);
        if(!propiedadEncontrada.estaDisponible()){
            return;
        }
        if(jugador.getDinero() < propiedadEncontrada.getPrecioCompra()){
            return;
        }

        jugador.restarDinero(propiedadEncontrada.getPrecioCompra());
        propiedadEncontrada.asignarPropietario(jugador);
        repositorioPropiedad.guardarPropiedad(propiedadEncontrada);
    }
}
