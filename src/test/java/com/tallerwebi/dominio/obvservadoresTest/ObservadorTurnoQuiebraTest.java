
package com.tallerwebi.dominio.obvservadoresTest;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class ObservadorTurnoQuiebraTest {

    private ServicioPartida servicioPartida;
    private ObservadorTurnoQuiebra observador;

    @BeforeEach
    void preparar() {
        servicioPartida = mock(ServicioPartida.class);
        observador = new ObservadorTurnoQuiebra(servicioPartida);
    }

    @Test
    void queAlQuebrarElJugadorActualSeAvanceElTurnoUnaSolaVez() {
        Jugador ana = new Jugador();
        ana.setId(1L);
        ana.setOrden(0);
        ana.setEnBancarrota(true);

        Jugador lucas = new Jugador();
        lucas.setId(2L);
        lucas.setOrden(1);
        lucas.setEnBancarrota(false);

        Jugador maria = new Jugador();
        maria.setId(3L);
        maria.setOrden(2);
        maria.setEnBancarrota(false);

        Partida partida = new Partida();
        partida.setCodigoUnico("PARTIDA123");
        partida.setJugadores(List.of(ana, lucas, maria));
        partida.setIndiceTurnoActual(0);

        EventoJugadorQuebrado evento = new EventoJugadorQuebrado(partida, ana);

        observador.actualizar(evento);

        verify(servicioPartida, times(1)).avanzarTurno("PARTIDA123");
    }
}
