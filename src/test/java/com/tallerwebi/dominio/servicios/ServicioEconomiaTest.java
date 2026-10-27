package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicioEconomiaTest {
    private final ServicioEconomiaImpl economia = new ServicioEconomiaImpl();

    @Test
    void rechazaMontosInvalidosSinCambiarElSaldo() {
        Jugador jugador = new Jugador();
        jugador.setDinero(100);
        assertThrows(IllegalArgumentException.class, () -> economia.restarDinero(jugador, -10));
        assertThrows(IllegalArgumentException.class, () -> economia.sumarDinero(jugador, null));
        assertEquals(100, jugador.getDinero());
    }

    @Test
    void laQuiebraSeResuelveEnElServicioDeDeudas() {
        Jugador jugador = new Jugador();
        jugador.setDinero(100);
        economia.restarDinero(jugador, 2100);
        assertEquals(-2000, jugador.getDinero());
        assertFalse(jugador.getEnBancarrota());
    }

    @Test
    void reconoceSaldoExactoYSaldoNoInicializado() {
        Jugador jugador = new Jugador();
        jugador.setDinero(100);
        assertTrue(economia.tieneDineroSuficiente(jugador, 100));
        assertFalse(economia.tieneDineroSuficiente(jugador, 101));
        jugador.setDinero(null);
        assertFalse(economia.tieneDineroSuficiente(jugador, 1));
        assertThrows(IllegalArgumentException.class, () -> economia.restarDinero(jugador, 1));
    }

    @Test
    void rechazaDesbordamientoSinModificarElSaldo() {
        Jugador jugador = new Jugador();
        jugador.setDinero(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> economia.sumarDinero(jugador, 1));
        assertEquals(Integer.MAX_VALUE, jugador.getDinero());
    }
    @Test
    public void queSePuedaSumarDineroAlJugador() {
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);

        economia.sumarDinero(jugador, 500);

        assertEquals(1500, jugador.getDinero());
    }

    @Test
    public void queSePuedaRestarDineroAlJugador() {
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);

        economia.restarDinero(jugador, 300);

        assertEquals(700, jugador.getDinero());
    }

}
