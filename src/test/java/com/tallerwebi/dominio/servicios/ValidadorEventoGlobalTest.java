package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidadorEventoGlobalTest {

    @Test
    public void dadoQueUnJugadorAbandonoCuandoElOtroYaTiroEntoncesPuedeResolverElEvento() {
        Jugador activo = new Jugador();
        activo.setId(1L);
        Jugador abandonado = new Jugador();
        abandonado.setId(2L);
        abandonado.setAbandono(true);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC");
        partida.setJugadores(List.of(activo, abandonado));

        boolean puedeResolver = ValidadorEventoGlobal.puedeResolverTrasAbandonos(partida, Map.of(1L, 8), Map.of());

        assertTrue(puedeResolver);
    }

    @Test
    public void dadoQueHayTiradasPendientesCuandoSeVerificaLaResolucionEntoncesNoResuelveElEvento() {
        Jugador primero = new Jugador();
        primero.setId(1L);
        Jugador segundo = new Jugador();
        segundo.setId(2L);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC");
        partida.setJugadores(List.of(primero, segundo));

        boolean puedeResolver = ValidadorEventoGlobal.puedeResolverTrasAbandonos(partida, Map.of(1L, 8), Map.of());

        assertFalse(puedeResolver);
    }

    @Test
    public void dadoQueFaltaLaPartidaOTiradasCuandoSeVerificaLaResolucionEntoncesNoSeResuelve() {
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC");

        assertFalse(ValidadorEventoGlobal.puedeResolverTrasAbandonos(null, Map.of(), Map.of()));
        assertFalse(ValidadorEventoGlobal.puedeResolverTrasAbandonos(partida, null, Map.of()));
    }

    @Test
    public void dadoQueYaExisteUnEventoCuandoSeIniciaOtroEntoncesLanzaLaExcepcion() {
        EventoGlobal evento = new EventoGlobal();

        assertThrows(EventoGlobalActivoException.class,
                () -> ValidadorEventoGlobal.validarAusenciaDeEventoAnterior(evento));
    }

    @Test
    public void dadoQueNoExisteEventoPrevioCuandoSeIniciaOtroEntoncesLaValidacionPermiteContinuar() {
        assertDoesNotThrow(() -> ValidadorEventoGlobal.validarAusenciaDeEventoAnterior(null));
    }

    @Test
    public void dadoQueElJugadorTieneDatosCuandoSeGeneraElEstadoEntoncesSusValoresSonRegistrables() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setPosicionActual(5);
        jugador.setDinero(1000);

        assertTrue(ValidadorEventoGlobal.tienePosicionRegistrable(jugador));
        assertTrue(ValidadorEventoGlobal.tieneSaldoRegistrable(jugador));
        assertTrue(ValidadorEventoGlobal.hayMovimientoPendiente(1L, new EventoGlobal()));
        assertFalse(ValidadorEventoGlobal.hayMovimientoPendiente(null, new EventoGlobal()));
    }
}
