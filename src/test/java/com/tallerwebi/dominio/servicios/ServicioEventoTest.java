package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.infraestructura.RepositorioEvento; // <-- Importar esto
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock; // <-- Y agregar este import de Mockito

public class ServicioEventoTest {

    private ServicioEvento servicioEvento;
    private RepositorioEvento repositorioEventoMock; // <-- Declaramos el mock

    @BeforeEach
    public void init() {
        repositorioEventoMock = mock(RepositorioEvento.class);
        //servicioEvento = new ServicioEventoImpl(repositorioEventoMock);
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoGanarPlataSeLeDebeSumarElDinero() {
        // Preparación
        Jugador jugador = new Jugador();
        jugador.setDinero(1500);

        Evento evento = new Evento();
        evento.setDescripcion("Ganaste el quini 6");
        evento.setTipoEfecto(TipoEfecto.GANAR_PLATA);
        evento.setValor(1000);

        // Ejecución
        servicioEvento.aplicarEvento(jugador, evento);

        // Comprobación
        assertEquals(2500, jugador.getDinero());
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoPerderPlataSeLeDebeRestarElDinero() {
        // Preparación
        Jugador jugador = new Jugador();
        jugador.setDinero(1500);

        Evento evento = new Evento();
        evento.setDescripcion("Control de tránsito en camino de cintura");
        evento.setTipoEfecto(TipoEfecto.PERDER_PLATA);
        evento.setValor(500);

        // Ejecución
        servicioEvento.aplicarEvento(jugador, evento);

        // Comprobación
        assertEquals(1000, jugador.getDinero());
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoPerderTurnoSeLeDebeMarcarQuePierdeElTurno() {
        Jugador jugador = new Jugador();
        jugador.setPierdeTurno(false);

        Evento evento = new Evento();
        evento.setTipoEfecto(TipoEfecto.PERDER_TURNO);
        evento.setValor(1);

        servicioEvento.aplicarEvento(jugador, evento);

        assertEquals(true, jugador.getPierdeTurno());
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoAvanzarCasillerosSuPosicionDebeAumentar() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(5);

        Evento evento = new Evento();
        evento.setTipoEfecto(TipoEfecto.MOVER_CASILLEROS);
        evento.setValor(2); // Avanza 2

        servicioEvento.aplicarEvento(jugador, evento);

        assertEquals(7, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoRetrocederCasillerosDesdeElInicioDebeDarLaVueltaAlTablero() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(1);

        Evento evento = new Evento();
        evento.setTipoEfecto(TipoEfecto.MOVER_CASILLEROS);
        evento.setValor(-2); // Retrocede 2

        servicioEvento.aplicarEvento(jugador, evento);

        // Estaba en 1, retrocede a 0, luego a la casilla 29 (asumiendo 30 casilleros)
        assertEquals(29, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorCaeEnEventoIrACasilleroDebeIrDirectamenteAesaPosicion() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(5);

        Evento evento = new Evento();
        evento.setTipoEfecto(TipoEfecto.IR_A_CASILLERO);
        evento.setValor(15); // Va a la casilla 15 (ej: UNLaM)

        servicioEvento.aplicarEvento(jugador, evento);

        assertEquals(15, jugador.getPosicionActual());
    }
}
