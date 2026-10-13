package com.tallerwebi.dominio.entidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.enums.EstadoPartida;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PartidaTest {

    @Test
    public void dadoQueSeCreaUnaPartidaCuandoSeInicializaDaComoResultadoLosValoresIniciales() {
        Partida partida = new Partida();

        assertNotNull(partida.getJugadores());
        assertTrue(partida.getJugadores().isEmpty());
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertNull(partida.getId());
        assertNull(partida.getCodigoUnico());
        assertNull(partida.getTiempoInicio());
        assertNull(partida.getCreador());
        assertNull(partida.getJugadorTurnoAleatorio());
    }

    @Test
    public void dadoQueExisteUnaPartidaCuandoSeAsignanSusDatosDaComoResultadoLosDatosGuardados() {
        Partida partida = new Partida();
        Usuario creador = new Usuario();
        Instant tiempoInicio = Instant.parse("2026-10-13T12:00:00Z");

        partida.setId(15L);
        partida.setCodigoUnico("MATANZA123");
        partida.setTiempoInicio(tiempoInicio);
        partida.setCreador(creador);
        partida.setEstado(EstadoPartida.EN_CURSO);

        assertEquals(15L, partida.getId());
        assertEquals("MATANZA123", partida.getCodigoUnico());
        assertEquals(tiempoInicio, partida.getTiempoInicio());
        assertSame(creador, partida.getCreador());
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
    }

    @Test
    public void dadoQueExisteUnaPartidaCuandoSeAsignanJugadoresYTurnosDaComoResultadoLosValoresActualizados() {
        Partida partida = new Partida();
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        List<Jugador> jugadores = List.of(jugadorUno, jugadorDos);

        partida.setJugadores(jugadores);
        partida.setJugadorTurnoAleatorio(jugadorDos);
        partida.setIndiceTurnoActual(1);
        partida.setRondaActual(3);
        partida.setTurnosFinalizadosEnRonda(1);

        assertSame(jugadores, partida.getJugadores());
        assertSame(jugadorDos, partida.getJugadorTurnoAleatorio());
        assertEquals(1, partida.getIndiceTurnoActual());
        assertEquals(3, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
    }
}
