package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServicioPartidaDeudaPendienteTest {
    @Test
    public void dadoQueUnJugadorDebeDineroCuandoSeIntentaAvanzarYDaTurnoBloqueado() throws Exception {
        RepositorioPartida repositorio = mock(RepositorioPartida.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        ServicioTurno turnos = mock(ServicioTurno.class);
        ServicioPartidaImpl servicio = new ServicioPartidaImpl(repositorio, mock(ServicioInicioPartida.class), mock(GestorTemporizadorPartida.class), turnos,
                mock(PublicadorEventos.class), mock(PublicadorEventos.class), mock(ServicioEvento.class));
        servicio.configurarRepositorioDeudaPendiente(deudas);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        Partida partida = new Partida();
        partida.setCodigoUnico("PARTIDA");
        partida.setJugadores(List.of(jugador));
        when(repositorio.buscarPartidaActivaPorCodigoUnico("PARTIDA")).thenReturn(partida);
        when(deudas.buscarPorPartidaYJugador("PARTIDA", 1L)).thenReturn(new DeudaPendiente());

        servicio.avanzarTurno("PARTIDA");

        assertEquals(1, partida.getRondaActual());
        verify(turnos, never()).incrementarRondas(partida);
        verify(repositorio, never()).guardarPartida(partida);
    }
}
