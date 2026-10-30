package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.eventos.EventoGlobalFinalizado;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServicioEventoContinuarYEmpateTest {
    @Test
    public void dadoQueElKevinFinalizoCuandoSePresionaContinuarYDaPartidaReanudada() throws Exception {
        RepositorioPartida partidas = mock(RepositorioPartida.class);
        ApplicationEventPublisher publicador = mock(ApplicationEventPublisher.class);
        ServicioEventoImpl servicio = new ServicioEventoImpl(mock(RepositorioEvento.class), mock(ServicioDado.class),
                publicador, mock(GestorTemporizadorPartida.class), partidas);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        servicio.configurarServicioGestionQuiebra(quiebras);
        Jugador jugador = crearJugador(1L, 1500);
        Partida partida = crearPartida(jugador);
        when(partidas.buscarPartidaActivaPorCodigoUnico("PARTIDA")).thenReturn(partida);
        servicio.iniciarEventoGlobal(partida, crearEvento(TipoEventoGlobal.ROBO, TipoEfecto.PERDER_PLATA, 1000));

        assertEquals("FINALIZADO", servicio.consultarEstadoEventoGlobal("PARTIDA").fase());
        servicio.reanudarPartidaTrasEvento("PARTIDA");

        assertNull(servicio.consultarEstadoEventoGlobal("PARTIDA"));
        verify(publicador).publishEvent(any(EventoGlobalFinalizado.class));
    }

    @Test
    public void dadoQueHayDeudaCuandoSePresionaContinuarYDaPartidaPausada() throws Exception {
        RepositorioPartida partidas = mock(RepositorioPartida.class);
        ServicioEventoImpl servicio = new ServicioEventoImpl(mock(RepositorioEvento.class), mock(ServicioDado.class),
                mock(ApplicationEventPublisher.class), mock(GestorTemporizadorPartida.class), partidas);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        servicio.configurarServicioGestionQuiebra(quiebras);
        Jugador jugador = crearJugador(1L, 1500);
        Partida partida = crearPartida(jugador);
        when(partidas.buscarPartidaActivaPorCodigoUnico("PARTIDA")).thenReturn(partida);
        servicio.iniciarEventoGlobal(partida, crearEvento(TipoEventoGlobal.ROBO, TipoEfecto.PERDER_PLATA, 1000));
        when(quiebras.obtenerDeudaPendiente("PARTIDA", 1L)).thenReturn(new DeudaPendiente());

        assertThrows(IllegalStateException.class, () -> servicio.reanudarPartidaTrasEvento("PARTIDA"));
        assertEquals("FINALIZADO", servicio.consultarEstadoEventoGlobal("PARTIDA").fase());
    }

    @Test
    public void dadoQueEmpatanLosJugadoresCuandoTerminanLasTiradasEntoncesSePausaAntesDelDesempate() throws Exception {
        ServicioDado dados = mock(ServicioDado.class);
        GestorTemporizadorPartida temporizador = mock(GestorTemporizadorPartida.class);
        RepositorioPartida partidas = mock(RepositorioPartida.class);
        ServicioEventoImpl servicio = new ServicioEventoImpl(mock(RepositorioEvento.class), dados,
                mock(ApplicationEventPublisher.class), temporizador, partidas);
        Jugador primero = crearJugador(1L, 1500);
        Jugador segundo = crearJugador(2L, 1500);
        Partida partida = crearPartida(primero, segundo);
        when(partidas.buscarPartidaActivaPorCodigoUnico("PARTIDA")).thenReturn(partida);
        when(temporizador.obtenerSegundosRestantes("evento-global-empate:PARTIDA")).thenReturn(8);
        EventoGlobal afip = crearEvento(TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0);
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(2, 3), new ResultadoTirada(3, 2));
        servicio.iniciarEventoGlobal(partida, afip);

        servicio.realizarTiradaEventoGlobal(partida, primero, afip);
        servicio.realizarTiradaEventoGlobal(partida, segundo, afip);

        assertEquals("EMPATE", servicio.consultarEstadoEventoGlobal("PARTIDA").fase());
        assertEquals(2, servicio.consultarEstadoEventoGlobal("PARTIDA").participantes().size());
        verify(temporizador).programarTemporizador(eq("evento-global-empate:PARTIDA"), eq(8), any(Runnable.class));
        assertThrows(DatosEnventoInvalidoExceptions.class,
                () -> servicio.realizarTiradaEventoGlobal(partida, primero, afip));
    }

    private Jugador crearJugador(Long id, Integer saldo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setDinero(saldo);
        jugador.setPosicionActual(3);
        return jugador;
    }

    private Partida crearPartida(Jugador... jugadores) {
        Partida partida = new Partida();
        partida.setCodigoUnico("PARTIDA");
        partida.setJugadores(List.of(jugadores));
        return partida;
    }

    private EventoGlobal crearEvento(TipoEventoGlobal tipo, TipoEfecto efecto, Integer valor) {
        EventoGlobal evento = new EventoGlobal();
        evento.setId(1L);
        evento.setTipoEventoGlobal(tipo);
        evento.setTipoEfecto(efecto);
        evento.setValor(valor);
        evento.setDescripcion("Evento global de prueba");
        return evento;
    }
}
