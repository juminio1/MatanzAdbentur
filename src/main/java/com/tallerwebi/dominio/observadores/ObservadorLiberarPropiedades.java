package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObservadorLiberarPropiedades implements Observador<EventoJugadorAbandonado> {

    private final RepositorioPropiedad repositorioPropiedad;

    public ObservadorLiberarPropiedades(RepositorioPropiedad repositorioPropiedad) {
        this.repositorioPropiedad = repositorioPropiedad;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        List<Propiedad> propiedadesDelJugador = repositorioPropiedad.buscarPropiedadesPorPropietario(evento.jugador());

        if (propiedadesDelJugador != null) {
            for (Propiedad propiedad : propiedadesDelJugador) {
                propiedad.setPropietario(null);
                propiedad.setNivelMejora(0);

                repositorioPropiedad.guardarPropiedad(propiedad);
            }
        }
    }
}