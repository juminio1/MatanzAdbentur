package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.EventoGlobal;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import java.util.List;
import org.junit.jupiter.api.Test;

class AplicadorConsecuenciaEventoGlobalReglasTest {

    @Test
    void dadoQueUnJugadorEstaPresoCuandoGanaAfipDaComoResultadoQueNoSeMueve() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        AplicadorConsecuenciaEventoGlobal aplicador = aplicador(movimiento);
        Jugador jugador = jugadorPreso();
        EventoGlobal evento = evento(TipoEventoGlobal.TRAMITES_AFIP, 0);
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));

        String detalle = aplicador.aplicar(partida, evento, jugador);

        assertEquals(8, jugador.getPosicionActual());
        assertTrue(detalle.contains("preso"));
        verifyNoInteractions(movimiento);
    }

    @Test
    void dadoQueUnJugadorEstaPresoCuandoHayInundacionDaComoResultadoQueNoRetrocede() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        AplicadorConsecuenciaEventoGlobal aplicador = aplicador(movimiento);
        Jugador jugador = jugadorPreso();

        aplicador.aplicar(new Partida(), evento(TipoEventoGlobal.INUNDACION, -3), jugador);

        assertEquals(8, jugador.getPosicionActual());
        verifyNoInteractions(movimiento);
    }

    @Test
    void dadoQueUnJugadorEstaPresoCuandoHayPiqueteDaComoResultadoQueNoVuelveAInicio() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        AplicadorConsecuenciaEventoGlobal aplicador = aplicador(movimiento);
        Jugador jugador = jugadorPreso();

        aplicador.aplicar(new Partida(), evento(TipoEventoGlobal.PIQUETE, 0), jugador);

        assertEquals(8, jugador.getPosicionActual());
        verifyNoInteractions(movimiento);
    }

    @Test
    void dadoQueUnJugadorTieneMilPesosCuandoRecibeElFiatDaComoResultadoMilCuatrocientos() {
        ServicioEconomia economia = mock(ServicioEconomia.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        jugador.setApodo("test123");
        when(participacion.estaActivo(jugador)).thenReturn(true);
        doAnswer(invocacion -> {
            jugador.setDinero(jugador.getDinero() + (Integer) invocacion.getArgument(1));
            return null;
        }).when(economia).sumarDinero(eq(jugador), eq(400));
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(economia,
                movimiento, mock(ServicioGestionQuiebra.class), participacion);

        String detalle = aplicador.aplicar(new Partida(), evento(TipoEventoGlobal.FIAT_600, 400), jugador);

        assertEquals(1400, jugador.getDinero());
        assertTrue(detalle.contains("$1000"));
        assertTrue(detalle.contains("$400"));
        assertTrue(detalle.contains("$1400"));
    }

    private AplicadorConsecuenciaEventoGlobal aplicador(ServicioMovimiento movimiento) {
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        when(participacion.estaActivo(org.mockito.ArgumentMatchers.any(Jugador.class))).thenReturn(true);
        return new AplicadorConsecuenciaEventoGlobal(mock(ServicioEconomia.class), movimiento,
                mock(ServicioGestionQuiebra.class), participacion);
    }

    private Jugador jugadorPreso() {
        Jugador jugador = new Jugador();
        jugador.setApodo("test123");
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();
        return jugador;
    }

    private EventoGlobal evento(TipoEventoGlobal tipo, int valor) {
        EventoGlobal evento = new EventoGlobal();
        evento.setTipoEventoGlobal(tipo);
        evento.setValor(valor);
        return evento;
    }
}
