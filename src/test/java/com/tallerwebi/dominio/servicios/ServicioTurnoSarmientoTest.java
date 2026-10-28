package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

public class ServicioTurnoSarmientoTest {

    @Test
    public void dadoQueElProximoJugadorPierdeElTurnoPorSarmientoCuandoAvanzaYDaSiguienteJugadorHabilitado() {
        Jugador uno = new Jugador();
        uno.setId(1L);
        uno.setOrden(0);
        Jugador dos = new Jugador();
        dos.setId(2L);
        dos.setOrden(1);
        dos.setPierdeTurno(true);
        Jugador tres = new Jugador();
        tres.setId(3L);
        tres.setOrden(2);
        Partida partida = new Partida();
        partida.setJugadores(List.of(uno, dos, tres));
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        when(participacion.estaActivo(uno)).thenReturn(true);
        when(participacion.estaActivo(dos)).thenReturn(true);
        when(participacion.estaActivo(tres)).thenReturn(true);
        ServicioTurnoImpl servicioTurno = new ServicioTurnoImpl(participacion);

        Jugador siguiente = servicioTurno.resolverSiguienteJugador(partida, 0);

        assertEquals(tres, siguiente);
        assertFalse(dos.getPierdeTurno());
    }
}
