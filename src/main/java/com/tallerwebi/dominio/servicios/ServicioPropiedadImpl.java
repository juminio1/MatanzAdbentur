package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PropiedadNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPropiedad")
@Transactional
public class ServicioPropiedadImpl implements ServicioPropiedad {
    private final RepositorioPropiedad repositorioPropiedad;
    private final ServicioPartida servicioPartida;

    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, ServicioPartida servicioPartida) {
        this.repositorioPropiedad = repositorioPropiedad;
        this.servicioPartida = servicioPartida;
    }

    @Override
    public Propiedad obtenerPropiedadExistente(Long idPropiedad){
        Propiedad propiedad=repositorioPropiedad.buscarPropiedadPorId(idPropiedad);

        if(propiedad==null){
            throw new PropiedadNoEncontradaException("No se encontró la propiedad solicitada.");
        }
        return propiedad;
    }

    @Override
    public boolean estaDisponible(Propiedad propiedad) {
        return propiedad != null && propiedad.getPropietario() == null;
    }

    @Override
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        if(jugador==null){
            throw new IllegalArgumentException("El jugador no puede ser null");
        }

        Propiedad propiedadEncontrada = obtenerPropiedadExistente(idPropiedad);
        if (!estaDisponible(propiedadEncontrada)) {
            throw new PropiedadNoDisponibleException("La propiedad no está disponible.");
        }

        if (jugador.getDinero() < propiedadEncontrada.getPrecioCompra()) {
            throw new DineroInsuficienteException("El jugador no tiene dinero suficiente para realizar la operación.");
        }

        servicioPartida.restarDinero(jugador, propiedadEncontrada.getPrecioCompra());
        propiedadEncontrada.setPropietario(jugador);
        repositorioPropiedad.guardarPropiedad(propiedadEncontrada);

    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador) {
        if (propiedad == null || jugador == null) {
            return;
        }
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || propietario.getId() == jugador.getId()) {
            return;
        }
        Integer alquiler = propiedad.getPrecioAlquiler();
        if (alquiler == null || jugador.getDinero() < alquiler) {
            return;
        }
        servicioPartida.restarDinero(jugador, alquiler);
        servicioPartida.sumarDinero(propietario, alquiler);
    }
}