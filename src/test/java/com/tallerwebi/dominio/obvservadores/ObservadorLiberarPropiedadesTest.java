
package com.tallerwebi.dominio.obvservadores;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorLiberarPropiedades;
import com.tallerwebi.dominio.servicios.interfaces.ServicioLiberacionPropiedades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ObservadorLiberarPropiedadesTest {

    private ServicioLiberacionPropiedades servicioLiberacionPropiedadesMock;
    private ObservadorLiberarPropiedades observador;

    @BeforeEach
    public void init() {
        servicioLiberacionPropiedadesMock =
                mock(ServicioLiberacionPropiedades.class);

        observador = new ObservadorLiberarPropiedades(
                servicioLiberacionPropiedadesMock
        );
    }

    @Test
    public void dadoQueUnJugadorAbandonaCuandoElObservadorRecibeElEventoDelegaLaLiberacionDePropiedades() {

        // creamos el jugador, la partida y el evento.
        Jugador jugadorQueAbandona = new Jugador();
        jugadorQueAbandona.setId(1L);

        Partida partida = new Partida();

        EventoJugadorAbandonado evento =
                new EventoJugadorAbandonado(partida, jugadorQueAbandona);

        // ejecutamos el observador.
        observador.actualizar(evento);

        // Assert: verificamos que llamó al servicio.
        verify(servicioLiberacionPropiedadesMock)
                .liberarPropiedades(jugadorQueAbandona);
    }
}
