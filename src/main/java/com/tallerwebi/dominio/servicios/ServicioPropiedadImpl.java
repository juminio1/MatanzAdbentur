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
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        Propiedad propiedadEncontrada = repositorioPropiedad.buscarPropiedadPorId(idPropiedad);
        if (propiedadEncontrada == null) {
            throw new PropiedadNoEncontradaException("No se encontró la propiedad solicitada.");
        }
        if (!propiedadEncontrada.estaDisponible()) {
            throw new PropiedadNoDisponibleException("La propiedad no está disponible para la compra.");
        }
        if (jugador.getDinero() < propiedadEncontrada.getPrecioCompra()) {
            throw new DineroInsuficienteException("El jugador no tiene dinero suficiente para realizar la operación.");
        }
        servicioPartida.restarDinero(jugador, propiedadEncontrada.getPrecioCompra());
        propiedadEncontrada.asignarPropietario(jugador);
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