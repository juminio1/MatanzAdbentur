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
    private final RepositorioPropiedad repositorioPropiedad;

    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad) {
        this.repositorioPropiedad = repositorioPropiedad;
    }

    @Override
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        Propiedad propiedadEncontrada = repositorioPropiedad.buscarPropiedadPorId(idPropiedad);
        //verifica que la propiedad este disponible
        if(!propiedadEncontrada.estaDisponible()){
            return;
        }
        //verifica que el jugador tenga el dinero para comprarla.
        if(jugador.getDinero() < propiedadEncontrada.getPrecioCompra()){
            return;
        }
        //le descuenta el dinero al jugador
        jugador.restarDinero(propiedadEncontrada.getPrecioCompra());
        //le asigna un propietario del jugador que compra
        propiedadEncontrada.asignarPropietario(jugador);
        //guarda la propiedad con la actualizada.
        repositorioPropiedad.guardarPropiedad(propiedadEncontrada);
    }
}
