package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioLiberacionPropiedades;
import org.springframework.stereotype.Component;


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
