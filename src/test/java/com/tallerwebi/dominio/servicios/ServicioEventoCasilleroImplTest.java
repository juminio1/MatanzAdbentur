package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.catalogo.CatalogoTablero;
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
import java.util.Set;
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

    @Test
    void elCatalogoIdentificaUnicamenteDiezYVeinteComoTrivia() {
        assertEquals(Set.of(10, 20), CatalogoTablero.POSICIONES_TRIVIA);
    }

    @Test
    void elCasilleroDiezNoConsultaTarjetasNiProduceEfectosGenericos() {
        verificarCasilleroDeTriviaSinEfectos(10);
    }

    @Test
    void elCasilleroVeinteNoConsultaTarjetasNiProduceEfectosGenericos() {
        verificarCasilleroDeTriviaSinEfectos(20);
    }

    private void verificarCasilleroDeTriviaSinEfectos(Integer posicion) {
        jugador.setPosicionActual(posicion);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                List.of(new CasilleroEvento(null, posicion, "EVENTOS", "", null, null)));
        EventoCasillero tarjeta = new EventoCasillero(
                1, "Premio", "Cobrar $100", TipoEfectoEvento.SUMA, 100, null, null);
        when(tarjetas.obtenerTodos()).thenReturn(List.of(tarjeta));

        var resultado = servicio.aplicarSiCorresponde("GLOBAL", jugador);

        assertAll(
                () -> assertTrue(resultado.isEmpty()),
                () -> verifyNoInteractions(tarjetas, economia, movimiento, quiebras),
                () -> assertEquals(posicion, jugador.getPosicionActual()),
                () -> assertEquals(2000, jugador.getDinero()));
    }

    @Test
    void elCasilleroTresSigueAplicandoTarjetasGenericas() {
        verificarCasilleroConTarjetaGenerica(3);
    }

    @Test
    void elCasilleroCatorceSigueAplicandoTarjetasGenericas() {
        verificarCasilleroConTarjetaGenerica(14);
    }

    @Test
    void elCasilleroVeintiochoSigueAplicandoTarjetasGenericas() {
        verificarCasilleroConTarjetaGenerica(28);
    }

    private void verificarCasilleroConTarjetaGenerica(Integer posicion) {
        jugador.setPosicionActual(posicion);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                List.of(new CasilleroEvento(null, posicion, "EVENTOS", "", null, null)));
        EventoCasillero tarjeta = new EventoCasillero(
                1, "Premio", "Cobrar $100", TipoEfectoEvento.SUMA, 100, null, null);
        when(tarjetas.obtenerTodos()).thenReturn(List.of(tarjeta));

        var resultado = servicio.aplicarSiCorresponde("GLOBAL", jugador).orElseThrow();

        assertSame(tarjeta, resultado.evento());
        verify(tarjetas, times(1)).obtenerTodos();
        verify(economia, times(1)).sumarDinero(jugador, 100);
        verifyNoMoreInteractions(economia);
        verifyNoInteractions(movimiento, quiebras);
        assertEquals(posicion, jugador.getPosicionActual());
    }

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

    @TestFactory
    Stream<DynamicTest> movimientosDeTarjetaQueCruzanInicioNoOtorganPremio() {
        return Stream.of(
                new EventoCasillero(1, "Avance", "", TipoEfectoEvento.AVANZA_CASILLEROS, null, 5, null),
                new EventoCasillero(2, "Destino", "", TipoEfectoEvento.AVANZA_A_POSICION, null, null, 3),
                new EventoCasillero(3, "Inicio", "", TipoEfectoEvento.AVANZA_A_POSICION, null, null, 0)
        ).map(tarjeta -> DynamicTest.dynamicTest(tarjeta.getNombre(), () -> {
            preparar();
            jugador.setPosicionActual(28);
            Partida partida = new Partida();
            partida.setCodigoUnico("GLOBAL");
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setJugadores(List.of(jugador));
            RepositorioPartida partidas = mock(RepositorioPartida.class);
            when(partidas.buscarPartidaPorJugador(1L)).thenReturn(partida);
            ServicioEconomia economiaReal = new ServicioEconomiaImpl();
            ServicioMovimiento movimientoReal = new ServicioMovimientoImpl(economiaReal, partidas,
                    new ServicioParticipacionImpl());
            servicio = new ServicioEventoCasilleroImpl(tarjetas, casilleros, economiaReal,
                    movimientoReal, quiebras, partidas, new ServicioParticipacionImpl());
            when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                    List.of(new CasilleroEvento(null, 28, "EVENTOS", "", null, null)));
            when(tarjetas.obtenerTodos()).thenReturn(List.of(tarjeta));

            var resultado = servicio.aplicarSiCorresponde("GLOBAL", jugador).orElseThrow();

            assertEquals(tarjeta.getPosicionDestino() == null ? 3 : tarjeta.getPosicionDestino(),
                    jugador.getPosicionActual());
            assertEquals(2000, jugador.getDinero());
            assertEquals(0, resultado.dineroPorInicio());
        }));
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
