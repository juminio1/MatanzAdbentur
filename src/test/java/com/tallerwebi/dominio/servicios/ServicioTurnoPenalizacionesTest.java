package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioTurnoPenalizacionesTest {

    @Test
    void dadoQueUnJugadorTieneDosPenalizacionesCuandoPasanDosOportunidadesDaComoResultadoQueNoPuedeJugar() {
        Partida partida = partidaConDosJugadores();
        Jugador primero = partida.getJugadores().get(0);
        Jugador segundo = partida.getJugadores().get(1);
        segundo.setPierdeTurno(true);
        segundo.setPierdeTurno(true);
        ServicioTurnoImpl servicio = new ServicioTurnoImpl(new ServicioParticipacionImpl());

        assertSame(primero, servicio.resolverSiguienteJugador(partida, primero.getOrden()));
        assertTrue(segundo.getPierdeTurno());
        assertSame(primero, servicio.resolverSiguienteJugador(partida, primero.getOrden()));
        assertFalse(segundo.getPierdeTurno());
        assertSame(segundo, servicio.resolverSiguienteJugador(partida, primero.getOrden()));
    }

    @Test
    void dadoQueAmbosJugadoresEstanPenalizadosCuandoSeBuscaElSiguienteDaComoResultadoQueSeConsumenLosCastigos() {
        Partida partida = partidaConDosJugadores();
        partida.getJugadores().forEach(jugador -> jugador.setPierdeTurno(true));
        ServicioTurnoImpl servicio = new ServicioTurnoImpl(new ServicioParticipacionImpl());

        assertSame(partida.getJugadores().get(1), servicio.resolverSiguienteJugador(partida, 0));
        assertFalse(partida.getJugadores().get(0).getPierdeTurno());
        assertFalse(partida.getJugadores().get(1).getPierdeTurno());
        assertEquals(2, partida.getRondaActual());
    }

    private Partida partidaConDosJugadores() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));
        return partida;
    }
}
