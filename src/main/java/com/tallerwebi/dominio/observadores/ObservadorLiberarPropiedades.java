package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioLiberacionPropiedades;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObservadorLiberarPropiedades implements Observador<EventoJugadorAbandonado> {

    private final ServicioLiberacionPropiedades servicioLiberacionPropiedades;

    public ObservadorLiberarPropiedades(
            ServicioLiberacionPropiedades servicioLiberacionPropiedades) {
        this.servicioLiberacionPropiedades = servicioLiberacionPropiedades;
    }

    @Override
    public void actualizar(EventoJugadorAbandonado evento) {
        servicioLiberacionPropiedades.liberarPropiedades(evento.jugador());
    }
}