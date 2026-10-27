package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicioMovimientoTest {
    private final ServicioMovimientoImpl movimiento = new ServicioMovimientoImpl(new ServicioEconomiaImpl());

    @Test
    void acreditaCadaVueltaIncluyendoLaCaidaExactaEnInicio() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(28);
        jugador.setDinero(100);
        assertEquals(400, movimiento.avanzar(jugador, 32));
        assertEquals(0, jugador.getPosicionActual());
        assertEquals(500, jugador.getDinero());
    }

    @Test
    void retrocederNoOtorgaElPremioDeInicio() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(1);
        jugador.setDinero(100);
        assertEquals(0, movimiento.avanzar(jugador, -3));
        assertEquals(28, jugador.getPosicionActual());
        assertEquals(100, jugador.getDinero());
    }

    @Test
    void avanzarAUnaPosicionPremiaSoloSiCruzaInicio() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(25);
        jugador.setDinero(100);
        assertEquals(200, movimiento.avanzarAPosicion(jugador, 3));
        assertEquals(0, movimiento.avanzarAPosicion(jugador, 3));
        assertEquals(300, jugador.getDinero());
        assertThrows(IllegalArgumentException.class, () -> movimiento.avanzarAPosicion(jugador, 30));
        assertEquals(3, jugador.getPosicionActual());
    }
    @Test
    public void queElJugadorActualiceSuPosicionAlMoverseUnEspacioValidoDentroDelTablero() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(5);
        int avanceExacto = 7;
        int totalCasilleros = 40;

        movimiento.moverJugador(jugador, avanceExacto, totalCasilleros);

        assertEquals(12, jugador.getPosicionActual());
    }

    @Test
    public void queSiLaFichaSuperaElUltimoCasilleroDelTableroContinueDesdeElComienzo() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(28);
        int avance = 5;
        int totalCasilleros = 30;

        movimiento.moverJugador(jugador, avance, totalCasilleros);

        assertEquals(3, jugador.getPosicionActual());
    }

    @Test
    public void queAlCaerExactamenteEnElUltimoCasilleroElJugadorVuelvaALaPosicionCero() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(25);
        int avance = 5;
        int totalCasilleros = 30;

        movimiento.moverJugador(jugador, avance, totalCasilleros);

        assertEquals(0, jugador.getPosicionActual());
    }

}
