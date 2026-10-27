
package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioLiberacionPropiedades;
import org.springframework.stereotype.Component;

@Component
public class ObservadorLiberarPropiedadesQuiebra
        //indica que escucha eventos de quiebra.
        implements Observador<EventoJugadorQuebrado> {

    private final ServicioLiberacionPropiedades servicioLiberacionPropiedades;

    public ObservadorLiberarPropiedadesQuiebra(
            ServicioLiberacionPropiedades servicioLiberacionPropiedades) {
        this.servicioLiberacionPropiedades = servicioLiberacionPropiedades;
    }

    //se ejecuta cuando el publicador le notifica que un jugador quebro.
    @Override
    public void actualizar(EventoJugadorQuebrado evento) {
        //busca liberar todas las propiedades del jugador quebrado y quitar sus mejora. 
        servicioLiberacionPropiedades.liberarPropiedades(evento.jugador());
    }
}
