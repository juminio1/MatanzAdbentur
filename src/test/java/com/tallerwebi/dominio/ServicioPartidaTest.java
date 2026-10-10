package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ServicioPartida servicioPartida;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        servicioPartida = new ServicioPartidaImpl(repositorioPartidaMock);
    }

    @Test
    public void queSeObtenganLasPosicionesYSeMuestrenTodosLosJugadoresPorCodigo() {
        // partida falsa
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setPosicionActual(0); // Casillero inicial

        Jugador jugador2 = new Jugador();
        jugador2.setPosicionActual(7); // Casillero avanzado

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        // devuelve la partida falsa
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        Partida partidaObtenida = servicioPartida.obtenerPartida(codigoPrueba);

        // verifica que trae a los jugadores y sus posiciones exactas
        assertNotNull(partidaObtenida);
        assertEquals(2, partidaObtenida.getJugadores().size());
        assertEquals(0, partidaObtenida.getJugadores().get(0).getPosicionActual());
        assertEquals(7, partidaObtenida.getJugadores().get(1).getPosicionActual());
    }

    @Test
    public void alInicializarTurnosDeberiaAsignarOrdenACadaJugadorYComenzarEnCero() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        Jugador jugador2 = new Jugador();
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.inicializarTurnos(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual());
        assertEquals(0, jugador1.getOrden());
        assertEquals(1, jugador2.getOrden());
        assertFalse(jugador1.getPierdeTurno());
    }

    @Test
    public void dadoQueEsTurnoUnoAlObtenerJugadorActualDeberiaRetornarJugadorDos() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(1); // Es el turno del orden 1

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPrueba);

        assertNotNull(jugadorActual);
        assertEquals(1, jugadorActual.getOrden());
    }

    @Test
    public void alAvanzarTurnoDeberiaSumarUnoAlIndice() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(0); // Arranca en 0

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(1, partidaSimulada.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueEsElUltimoJugadorAlAvanzarTurnoDeberiaReiniciarAlPrimerJugador() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);
        
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(2); // Está en el último jugador

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual()); // Vuelve a 0
    }

    @Test
    public void dadoQueElSiguientePierdeTurnoAlAvanzarTurnoDeberiaSaltarloYQuitarleLaPenalizacion() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        jugador2.setPierdeTurno(true); 
        
        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);
        
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(0); 

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        // Debería saltar del 0 al 2 directamente porque el 1 pierde el turno
        assertEquals(2, partidaSimulada.getIndiceTurnoActual());
        assertFalse(jugador2.getPierdeTurno()); // Se le retira el castigo
    }
}
