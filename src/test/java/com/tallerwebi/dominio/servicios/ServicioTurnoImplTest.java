package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioTurnoImplTest {

    private ServicioTurnoImpl servicioTurno;

    @BeforeEach
    void preparar() {
        servicioTurno = new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }

    @Test
    void unaRondaSeCompletaSoloConLosJugadoresActivos() {
        Jugador ana = new Jugador();
        ana.setOrden(0);
        Jugador ausente = new Jugador();
        ausente.setOrden(1);
        ausente.setAbandono(true);
        Jugador quebrado = new Jugador();
        quebrado.setOrden(2);
        quebrado.setEnBancarrota(true);
        Jugador maria = new Jugador();
        maria.setOrden(3);
        Partida partida = new Partida();
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(ana, ausente, quebrado, maria));
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(0);

        servicioTurno.incrementarRondas(partida);
        assertEquals(1, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
        assertSame(maria, servicioTurno.resolverSiguienteJugador(partida, 0));
        partida.setIndiceTurnoActual(maria.getOrden());
        servicioTurno.incrementarRondas(partida);
        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
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
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(ana, lucas, maria));
        partida.setIndiceTurnoActual(0);

        Jugador siguiente = servicioTurno.resolverSiguienteJugador(
                partida, partida.getIndiceTurnoActual()
        );

        assertNotNull(siguiente);
        assertEquals(maria.getId(), siguiente.getId());
        assertFalse(siguiente.getEnBancarrota());
    }
    @Test
    void incrementarRondasRechazaEN_ESPERASinCambiarContadoresNiPenalizaciones() {
        Partida partida = new Partida();
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_ESPERA);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(0);
        Jugador actual = new Jugador();
        actual.setOrden(0);
        actual.setUltimaRondaConOportunidad(3);
        Jugador penalizado = new Jugador();
        penalizado.setOrden(1);
        penalizado.setPierdeTurno(true);
        penalizado.setUltimaRondaConOportunidad(3);
        partida.setJugadores(List.of(actual, penalizado));

        assertThrows(IllegalStateException.class, () -> servicioTurno.incrementarRondas(partida));

        assertEquals(4, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(3, actual.getUltimaRondaConOportunidad());
        assertEquals(3, penalizado.getUltimaRondaConOportunidad());
        assertTrue(penalizado.getPierdeTurno());
        assertEquals(0, partida.getIndiceTurnoActual());
    }
    @Test
    void resolverSiguienteJugadorRechazaEN_ESPERASinCambiarContadoresNiPenalizaciones() {
        Partida partida = new Partida();
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_ESPERA);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(0);
        Jugador actual = new Jugador();
        actual.setOrden(0);
        actual.setUltimaRondaConOportunidad(3);
        Jugador penalizado = new Jugador();
        penalizado.setOrden(1);
        penalizado.setPierdeTurno(true);
        penalizado.setUltimaRondaConOportunidad(3);
        partida.setJugadores(List.of(actual, penalizado));

        assertThrows(IllegalStateException.class, () -> servicioTurno.resolverSiguienteJugador(partida, 0));

        assertEquals(4, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(3, actual.getUltimaRondaConOportunidad());
        assertEquals(3, penalizado.getUltimaRondaConOportunidad());
        assertTrue(penalizado.getPierdeTurno());
        assertEquals(0, partida.getIndiceTurnoActual());
    }
    @Test
    void incrementarRondasRechazaFINALIZADASinCambiarContadoresNiPenalizaciones() {
        Partida partida = new Partida();
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(0);
        Jugador actual = new Jugador();
        actual.setOrden(0);
        actual.setUltimaRondaConOportunidad(3);
        Jugador penalizado = new Jugador();
        penalizado.setOrden(1);
        penalizado.setPierdeTurno(true);
        penalizado.setUltimaRondaConOportunidad(3);
        partida.setJugadores(List.of(actual, penalizado));

        assertThrows(IllegalStateException.class, () -> servicioTurno.incrementarRondas(partida));

        assertEquals(4, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(3, actual.getUltimaRondaConOportunidad());
        assertEquals(3, penalizado.getUltimaRondaConOportunidad());
        assertTrue(penalizado.getPierdeTurno());
        assertEquals(0, partida.getIndiceTurnoActual());
    }
    @Test
    void resolverSiguienteJugadorRechazaFINALIZADASinCambiarContadoresNiPenalizaciones() {
        Partida partida = new Partida();
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(0);
        Jugador actual = new Jugador();
        actual.setOrden(0);
        actual.setUltimaRondaConOportunidad(3);
        Jugador penalizado = new Jugador();
        penalizado.setOrden(1);
        penalizado.setPierdeTurno(true);
        penalizado.setUltimaRondaConOportunidad(3);
        partida.setJugadores(List.of(actual, penalizado));

        assertThrows(IllegalStateException.class, () -> servicioTurno.resolverSiguienteJugador(partida, 0));

        assertEquals(4, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(3, actual.getUltimaRondaConOportunidad());
        assertEquals(3, penalizado.getUltimaRondaConOportunidad());
        assertTrue(penalizado.getPierdeTurno());
        assertEquals(0, partida.getIndiceTurnoActual());
    }
}
