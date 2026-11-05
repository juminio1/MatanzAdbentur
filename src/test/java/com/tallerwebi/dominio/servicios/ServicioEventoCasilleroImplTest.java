package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.RepositorioEventoCasillero;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ServicioEventoCasilleroImplTest {
    private RepositorioEventoCasillero tarjetas;
    private ServicioCasillero casilleros;
    private ServicioEconomia economia;
    private ServicioMovimiento movimiento;
    private ServicioGestionQuiebra quiebras;
    private ServicioEventoCasilleroImpl servicio;
    private Jugador jugador;

    @BeforeEach
    void preparar() {
        tarjetas = mock(RepositorioEventoCasillero.class);
        casilleros = mock(ServicioCasillero.class);
        economia = mock(ServicioEconomia.class);
        movimiento = mock(ServicioMovimiento.class);
        quiebras = mock(ServicioGestionQuiebra.class);
        RepositorioPartida partidas = mock(RepositorioPartida.class);
        jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(2000);
        jugador.setPosicionActual(3);
        Partida partida = new Partida();
        partida.setCodigoUnico("GLOBAL");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(jugador));
        when(partidas.buscarPartidaPorJugador(1L)).thenReturn(partida);
        servicio = new ServicioEventoCasilleroImpl(tarjetas, casilleros, economia, movimiento,
                quiebras, partidas, new ServicioParticipacionImpl());
    }

    @Test
    void dadoQueElCodigoEsDeOtraPartidaCuandoSeIntentaAplicarUnaTarjetaDaComoResultadoElRechazoSinEfectos() {
        // Preparación
        String codigoAjeno = "OTRA";

        // Ejecución
        assertThrows(IllegalStateException.class, () -> servicio.aplicarSiCorresponde(codigoAjeno, jugador));

        // Verificación
        assertEquals(2000, jugador.getDinero());
        verifyNoInteractions(casilleros, tarjetas, economia, movimiento, quiebras);
    }

    @Test
    void dadoQueElJugadorNoTienePosicionCuandoSeConsultaLaCaidaDaComoResultadoNingunaTarjeta() {
        // Preparación
        jugador.setPosicionActual(null);

        // Ejecución
        Boolean hayTarjeta = servicio.aplicarSiCorresponde("GLOBAL", jugador).isPresent();

        // Verificación
        assertFalse(hayTarjeta);
        verifyNoInteractions(casilleros, tarjetas, economia, movimiento, quiebras);
    }

    @TestFactory
    Stream<DynamicTest> dadoQueLaTarjetaRestaTieneMontoInvalidoCuandoSeAplicaDaComoResultadoElRechazoSinCobrar() {
        return Stream.<Integer>of(null, 0, -100).map(monto -> DynamicTest.dynamicTest("Monto " + monto, () -> {
            // Preparación
            preparar();
            EventoCasillero tarjeta = new EventoCasillero(1, "Pago", "Pago inválido", TipoEfectoEvento.RESTA, monto, null, null);
            when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                    List.of(new CasilleroEvento(null, 3, "EVENTOS", "Tarjeta", null, null)));
            when(tarjetas.obtenerTodos()).thenReturn(List.of(tarjeta));

            // Ejecución
            assertThrows(IllegalArgumentException.class, () -> servicio.aplicarSiCorresponde("GLOBAL", jugador));

            // Verificación
            assertEquals(2000, jugador.getDinero());
            verifyNoInteractions(economia, movimiento, quiebras);
        }));
    }

    @Test
    void dadoQueYaExisteUnPagoPendienteCuandoSaleUnaTarjetaRestaDaComoResultadoElRechazoSinOtraDeuda() {
        // Preparación
        EventoCasillero tarjeta = new EventoCasillero(1, "Pago", "Pagá $500", TipoEfectoEvento.RESTA, 500, null, null);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                List.of(new CasilleroEvento(null, 3, "EVENTOS", "Tarjeta", null, null)));
        when(tarjetas.obtenerTodos()).thenReturn(List.of(tarjeta));
        when(quiebras.obtenerDeudaPendiente("GLOBAL", 1L)).thenReturn(new DeudaPendiente());

        // Ejecución
        assertThrows(IllegalStateException.class, () -> servicio.aplicarSiCorresponde("GLOBAL", jugador));

        // Verificación
        verify(quiebras).obtenerDeudaPendiente("GLOBAL", 1L);
        verifyNoMoreInteractions(quiebras);
        verifyNoInteractions(economia, movimiento);
        assertEquals(2000, jugador.getDinero());
    }
}
