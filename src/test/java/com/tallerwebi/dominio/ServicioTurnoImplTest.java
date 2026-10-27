package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioTurnoImplTest {

    private ServicioTurnoImpl servicioTurno;

    @BeforeEach
    void preparar() {
        servicioTurno = new ServicioTurnoImpl();
    }

    @Test
    void queUnJugadorEnQuiebraNoRecibaTurnos() {
        Jugador ana = new Jugador();
        ana.setId(1L);
        ana.setOrden(0);
        ana.setEnBancarrota(false);
        ana.setAbandono(false);

        Jugador lucas = new Jugador();
        lucas.setId(2L);
        lucas.setOrden(1);
        lucas.setEnBancarrota(true);
        lucas.setAbandono(false);

        Jugador maria = new Jugador();
        maria.setId(3L);
        maria.setOrden(2);
        maria.setEnBancarrota(false);
        maria.setAbandono(false);

        Partida partida = new Partida();
        partida.setJugadores(List.of(ana, lucas, maria));
        partida.setIndiceTurnoActual(0);

        Jugador siguiente = servicioTurno.resolverSiguienteJugador(
                partida, partida.getIndiceTurnoActual()
        );

        assertNotNull(siguiente);
        assertEquals(maria.getId(), siguiente.getId());
        assertFalse(siguiente.getEnBancarrota());
    }
}