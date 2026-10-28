package com.tallerwebi.dominio.servicios;

import com.tallerwebi.infraestructura.RepositorioPartida;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.resultados.PosicionFicha;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;

import java.util.Arrays;
import java.util.List;

import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

    private static final String CODIGO = "MATANZA123";
    private RepositorioPartida repositorioPartidaMock;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;
    private NotificadorPartida notificadorPartidaMock;
    private GestorTemporizadorPartida gestorTemporizadorMock;
    private ServicioPartida servicioPartida;
    private ServicioEvento servicioEventoMock;

    private PublicadorEventos<EventoJugadorAbandonado> publicadorAbandonoMock;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        gestorTemporizadorMock = mock(GestorTemporizadorPartida.class);
        servicioEventoMock = mock(ServicioEvento.class);
        publicadorAbandonoMock = mock(PublicadorEventos.class);

        servicioPartida = new ServicioPartidaImpl(
            repositorioPartidaMock,
            new ServicioInicioPartidaImpl(repositorioPartidaMock, repositorioSalaDeEsperaMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl())),
            gestorTemporizadorMock,
            new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()),
            new PublicadorEventos<>(List.of(new ObservadorTurnoWebSocket(notificadorPartidaMock))),
            publicadorAbandonoMock, servicioEventoMock
        );

    }

    @Test
    public void dadoQueHayDosJugadoresCuandoSeInicializanTurnosYDaUnOrdenParaCadaUno() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        Jugador jugador2 = new Jugador();
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
                partidaSimulada
        );

        servicioPartida.inicializarTurnos(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual());
        assertEquals(0, jugador1.getOrden());
        assertEquals(1, jugador2.getOrden());
        assertFalse(jugador1.getPierdeTurno());
    }

    @Test
    public void dadoQueHayJugadoresConEstadoAnteriorCuandoSeInicializanTurnosYDaJugadoresRestablecidos() {
        String codigo = "MATANZA123";
        Partida partida = new Partida();
        Jugador jugador = new Jugador();
        jugador.setOrden(3);
        jugador.setPierdeTurno(true);
        jugador.setEnBancarrota(true);
        jugador.setDinero(500);
        jugador.setPosicionActual(7);
        partida.setJugadores(List.of(jugador));
        partida.setIndiceTurnoActual(2);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);

        servicioPartida.inicializarTurnos(codigo);

        assertEquals(0, jugador.getOrden());
        assertFalse(jugador.getPierdeTurno());
        assertFalse(jugador.getEnBancarrota());
        assertEquals(500, jugador.getDinero());
        assertEquals(7, jugador.getPosicionActual());
        assertEquals(2, partida.getIndiceTurnoActual());
        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void dadoQueHayJugadoresEnUnaPartidaCuandoSeConsultaPorCodigoYDaSusPosiciones() {
        // partida falsa
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setPosicionActual(0);
        Jugador jugador2 = new Jugador();
        jugador2.setPosicionActual(7);
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        Partida partidaObtenida = servicioPartida.obtenerPartida(codigoPrueba);

        assertNotNull(partidaObtenida);
        assertEquals(2, partidaObtenida.getJugadores().size());
        assertEquals(0, partidaObtenida.getJugadores().get(0).getPosicionActual());
        assertEquals(7, partidaObtenida.getJugadores().get(1).getPosicionActual());
    }

    @Test
    public void dadoQueExistenJugadoresCuandoSeInicializanTurnosYDaOrdenDesdeCero() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        Jugador jugador2 = new Jugador();
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.inicializarTurnos(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual());
        assertEquals(0, jugador1.getOrden());
        assertEquals(1, jugador2.getOrden());
        assertFalse(jugador1.getPierdeTurno());
    }

    @Test
    public void dadoQueElIndiceEsUnoCuandoSeObtieneJugadorActualYDaJugadorDos() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(1);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPrueba);

        assertNotNull(jugadorActual);
        assertEquals(1, jugadorActual.getOrden());
    }

    @Test
    public void dadoQueJuegaElPrimeroCuandoSeAvanzaTurnoYDaElSegundo() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setCodigoUnico(codigoPrueba);
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(1, partidaSimulada.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueLaPartidaNoTieneJugadoresCuandoSeEjecutanOperacionesDeTurnoYDaSinCambios() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigo = "SIN-JUGADORES";
        Partida sinLista = new Partida();
        sinLista.setJugadores(null);
        Partida listaVacia = new Partida();
        listaVacia.setJugadores(List.of());

        for (Partida partida : Arrays.asList(null, sinLista, listaVacia)) {
            when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
                .thenReturn(partida);

            servicioPartida.inicializarTurnos(codigo);
            assertEquals(null, servicioPartida.obtenerJugadorActual(codigo));
            assertTrue(servicioPartida.obtenerPosicionesFichas(codigo).isEmpty());
            servicioPartida.avanzarTurno(codigo);
        }

        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueHayJugadorEnBancarrotaCuandoSeAvanzaTurnoYDaJugadorActivo() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigo = "BANCARROTA";
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        Jugador activo = new Jugador();
        activo.setOrden(0);
        Jugador enBancarrota = new Jugador();
        enBancarrota.setOrden(1);
        enBancarrota.setEnBancarrota(true);
        partida.setJugadores(List.of(activo, enBancarrota));
        partida.setIndiceTurnoActual(1);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);

        assertEquals(null, servicioPartida.obtenerJugadorActual(codigo));
        partida.setIndiceTurnoActual(0);
        servicioPartida.avanzarTurno(codigo);

        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(activo, servicioPartida.obtenerJugadorActual(codigo));
        assertTrue(enBancarrota.getEnBancarrota());
    }

    @Test
    public void dadoQueNoHayJugadorActivoCuandoSeIniciaTemporizadorYDaSinNotificacion() {
        String codigo = "SIN-JUGADORES";
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setJugadores(List.of());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(codigo), eq(30), any(Runnable.class))).thenReturn(true);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueNoHayPublicadorCuandoSeAvanzaTurnoYDaTemporizadorProgramado() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigo = "SIN-NOTIFICADOR";
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        Jugador jugador = new Jugador();
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));
        partida.setIndiceTurnoActual(null);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);
        ServicioPartida servicioSinNotificador = new ServicioPartidaImpl(
            repositorioPartidaMock,
            new ServicioInicioPartidaImpl(repositorioPartidaMock, repositorioSalaDeEsperaMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl())),
            gestorTemporizadorMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()), null, publicadorAbandonoMock, servicioEventoMock);

        servicioSinNotificador.avanzarTurno(codigo);

        assertEquals(0, partida.getIndiceTurnoActual());
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(codigo), eq(30), any(Runnable.class));
        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void dadoQueTodosPierdenTurnoCuandoSeAvanzaYDaPenalizacionesRestablecidas() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigo = "TODOS-PENALIZADOS";
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        primero.setPierdeTurno(true);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        segundo.setPierdeTurno(true);
        partida.setJugadores(List.of(primero, segundo));
        partida.setIndiceTurnoActual(0);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        servicioPartida.avanzarTurno(codigo);

        assertEquals(1, partida.getIndiceTurnoActual());
        assertFalse(primero.getPierdeTurno());
        assertFalse(segundo.getPierdeTurno());
        verify(repositorioPartidaMock).guardarPartida(partida);
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(codigo), eq(30), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq(codigo), any(TurnoDTO.class));
    }

    @Test
    public void dadoQueTodosEstanEnBancarrotaCuandoSeAvanzaYDaPartidaSinTurnoNuevo() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigo = "TODOS-EN-BANCARROTA";
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        primero.setEnBancarrota(true);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        segundo.setEnBancarrota(true);
        segundo.setPierdeTurno(true);
        partida.setJugadores(List.of(primero, segundo));
        partida.setIndiceTurnoActual(0);
        partida.setRondaActual(3);
        partida.setTurnosFinalizadosEnRonda(1);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        servicioPartida.avanzarTurno(codigo);

        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(3, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
        assertTrue(segundo.getPierdeTurno());
        verify(repositorioPartidaMock).guardarPartida(partida);
        verify(gestorTemporizadorMock, times(1)).cancelarTemporizador(codigo);
        verify(gestorTemporizadorMock, never()).programarTemporizador(
            any(), anyInt(), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueSeAvanzaTurnoCuandoSeNotificaYDaNuevoTurnoConDuracion() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setCodigoUnico(codigoPrueba);
        Jugador jugador1 = new Jugador();
        jugador1.setId(1L);
        jugador1.setApodo("Jugador1");
        jugador1.setOrden(0);

        Jugador jugador2 = new Jugador();
        jugador2.setId(2L);
        jugador2.setApodo("Jugador2");
        jugador2.setOrden(1);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        servicioPartida.avanzarTurno(codigoPrueba);

        verify(notificadorPartidaMock).notificarCambioDeTurno(
            eq(codigoPrueba),
            argThat(
                (TurnoDTO dto) ->
                    dto.getIdJugadorActual() != null
            )
        );
    }

    @Test
    public void dadoQueJuegaElUltimoCuandoSeAvanzaTurnoYDaElPrimero() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setCodigoUnico(codigoPrueba);
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(2);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueElSiguientePierdeTurnoCuandoSeAvanzaYDaTurnoSalteado() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setCodigoUnico(codigoPrueba);
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        jugador2.setPierdeTurno(true);
        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(2, partidaSimulada.getIndiceTurnoActual());
        assertFalse(jugador2.getPierdeTurno());
    }

    @Test
    public void dadoQueExistePartidaCuandoSeIniciaYDaJugadoresInicializados()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        Usuario creador = new Usuario();
        creador.setId(1L);

        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        sala.setCreador(creador);

        Partida partida = new Partida();
        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico()))
            .thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
            .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);

        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
        for (Jugador jugador : partida.getJugadores()) {
            assertEquals(2000, jugador.getDinero());
            assertEquals(0, jugador.getPosicionActual());
        }
    }

    @Test
    public void dadoQueSeIniciaPartidaCuandoSeNotificaYDaTurnoInicial()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        Usuario creador = new Usuario();
        creador.setId(1L);

        Jugador jugadorUno = new Jugador();
        jugadorUno.setId(1L);
        jugadorUno.setApodo("J1");
        Jugador jugadorDos = new Jugador();
        jugadorDos.setId(2L);
        jugadorDos.setApodo("J2");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);

        Partida partida = new Partida();
        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(
            repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())
        ).thenReturn(partida);
        when(gestorTemporizadorMock.obtenerSegundosRestantes(sala.getCodigoUnico())).thenReturn(null);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);

        verify(notificadorPartidaMock).notificarCambioDeTurno(
            eq("ABC123"),
            argThat(
                (TurnoDTO dto) ->
                    dto.getIdJugadorActual() != null && dto.getDuracionSegundos() == null
            )
        );
    }

    @Test
    public void dadoQueNoHayJugadoresCuandoSeIniciaPartidaYDaExcepcion() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList());

        Usuario creador = new Usuario();
        creador.setId(1L);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(
            repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())
        ).thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class, () ->
            servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L)
        );

        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueHayCuatroUsuariosCuandoSeIniciaPartidaYDaPartidaCreada()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        Usuario usuarioUno = new Usuario();
        usuarioUno.setUsername("usuario1");
        usuarioUno.setId(1L);

        Usuario usuarioDos = new Usuario();
        usuarioDos.setUsername("usuario2");

        Usuario usuarioTres = new Usuario();
        usuarioTres.setUsername("usuario3");

        Usuario usuarioCuatro = new Usuario();
        usuarioCuatro.setUsername("usuario4");
        usuarioUno.setFicha(ColorFicha.ROJO);
        usuarioDos.setFicha(ColorFicha.AZUL);
        usuarioTres.setFicha(ColorFicha.VERDE);
        usuarioCuatro.setFicha(ColorFicha.AMARILLO);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuarioUno);
        sala.setUsuarios(Arrays.asList(usuarioUno, usuarioDos, usuarioTres, usuarioCuatro));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(
            repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())
        ).thenReturn(null);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);

        verify(repositorioPartidaMock).guardarPartida(any(Partida.class));
    }

    @Test
    public void dadoQueExistenJugadoresCuandoSeIniciaPartidaYDaTurnoAleatorio()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        Usuario creador = new Usuario();
        creador.setId(1L);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(
            repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())
        ).thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);

        assertEquals(4, partida.getJugadores().size());
        Jugador jugadorSeleccionado = partida.getJugadorTurnoAleatorio();
        assertNotNull(jugadorSeleccionado);
        assertTrue(partida.getJugadores().contains(jugadorSeleccionado));
        assertEquals(jugadorSeleccionado.getOrden(), partida.getIndiceTurnoActual());
        assertEquals(
            jugadorSeleccionado,
            servicioPartida.obtenerJugadorActual(sala.getCodigoUnico())
        );
    }

    @Test
    public void dadoQueFaltanTurnosCuandoSeIncrementanRondasYDaMismaRonda() {
        Partida partida = new Partida();
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(0);

        servicioPartida.incrementarRondas(partida);

        assertEquals(1, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
    }

    @Test
    public void dadoQueTodosJugaronCuandoSeIncrementanRondasYDaNuevaRonda() {
        Partida partida = new Partida();
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(3);

        servicioPartida.incrementarRondas(partida);

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
    }

    @Test
    public void dadoQueContadoresSonNulosCuandoSeIncrementanRondasYDaValoresInicializados() {
        Partida partida = new Partida();
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos));
        partida.setRondaActual(null);
        partida.setTurnosFinalizadosEnRonda(null);

        servicioPartida.incrementarRondas(partida);

        assertEquals(1, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
    }

    @Test
    public void dadoQueHayCuatroJugadoresCuandoFinalizanTurnosYDaNuevaRonda() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";

        Jugador jugadorUno = new Jugador();
        jugadorUno.setOrden(0);
        Jugador jugadorDos = new Jugador();
        jugadorDos.setOrden(1);
        Jugador jugadorTres = new Jugador();
        jugadorTres.setOrden(2);
        Jugador jugadorCuatro = new Jugador();
        jugadorCuatro.setOrden(3);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoPrueba);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));
        partida.setIndiceTurnoActual(2);
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partida
        );

        servicioPartida.avanzarTurno(codigoPrueba);
        servicioPartida.avanzarTurno(codigoPrueba);
        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(1, partida.getRondaActual());
        assertEquals(3, partida.getTurnosFinalizadosEnRonda());

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(2, partida.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueHayJugadoresCuandoSeObtienePartidaPorCodigoYDaPosiciones() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setPosicionActual(0);
        Jugador jugador2 = new Jugador();
        jugador2.setPosicionActual(7);
        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        Partida partidaObtenida = servicioPartida.obtenerPartida(codigoPrueba);

        assertNotNull(partidaObtenida);
        assertEquals(2, partidaObtenida.getJugadores().size());
        assertEquals(0, partidaObtenida.getJugadores().get(0).getPosicionActual());
        assertEquals(7, partidaObtenida.getJugadores().get(1).getPosicionActual());
    }


    @Test
    public void dadoQueHayCincoJugadoresCuandoSeIniciaPartidaYDaCantidadMaximaSuperada() {
        String codigo = "ABC123";

        Usuario creador = new Usuario();
        creador.setId(1L);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico(codigo);
        sala.setCreador(creador);

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(
            Arrays.asList(new Jugador(), new Jugador(), new Jugador(), new Jugador(), new Jugador())
        );

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(codigo)).thenReturn(sala);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        assertThrows(CantidadMaximaJugadoresSuperadaException.class, () ->
            servicioPartida.iniciarPartida(codigo, 1L)
        );

        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());

    assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());

    verify(repositorioPartidaMock, never()).guardarPartida(any());
}

@Test
public void dadoQueUsuariosTienenFichasCuandoSeIniciaPartidaYDaFichasConvertidas() {
    String codigo = "ABC123";

    Usuario usuarioUno = new Usuario();
    usuarioUno.setId(1L);
    usuarioUno.setUsername("JugadorRojo");
    usuarioUno.setFicha(ColorFicha.ROJO);

    Usuario usuarioDos = new Usuario();
    usuarioDos.setUsername("JugadorAzul");
    usuarioDos.setFicha(ColorFicha.AZUL);

    Usuario usuarioTres = new Usuario();
    usuarioTres.setUsername("JugadorVerde");
    usuarioTres.setFicha(ColorFicha.VERDE);

    Usuario usuarioCuatro = new Usuario();
    usuarioCuatro.setUsername("JugadorAmarillo");
    usuarioCuatro.setFicha(ColorFicha.AMARILLO);

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico(codigo);
    sala.setCreador(usuarioUno);
    sala.setUsuarios(Arrays.asList(
            usuarioUno, usuarioDos, usuarioTres, usuarioCuatro
    ));

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_ESPERA);

    when(repositorioSalaDeEsperaMock.buscarPorCodigo(codigo))
            .thenReturn(sala);

    when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);

    assertDoesNotThrow(() -> servicioPartida.iniciarPartida(codigo, 1L));

    assertEquals(4, partida.getJugadores().size());

    assertEquals(ColorFicha.ROJO, partida.getJugadores().get(0).getFicha());
    assertEquals(ColorFicha.AZUL, partida.getJugadores().get(1).getFicha());
    assertEquals(ColorFicha.VERDE, partida.getJugadores().get(2).getFicha());
    assertEquals(ColorFicha.AMARILLO, partida.getJugadores().get(3).getFicha());

    assertEquals("JugadorRojo", partida.getJugadores().get(0).getApodo());
    assertEquals("JugadorAmarillo", partida.getJugadores().get(3).getApodo());
}

    @Test
    public void dadoQueUnJugadorNoTieneFichaCuandoSeIniciaPartidaYDaFichaNoDisponibleException() {
        String codigo = "SALA01";
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico(codigo);

        Usuario creador = new Usuario();
        creador.setId(1L);
        creador.setFicha(ColorFicha.ROJO);

        Usuario invitado = new Usuario();
        invitado.setId(2L);
        invitado.setFicha(null); // Sin ficha seleccionada

        sala.setCreador(creador);
        sala.getUsuarios().addAll(List.of(creador, invitado));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(codigo)).thenReturn(sala);

        // Si usas JUnit 5:
        assertThrows(FichaNoDisponibleException.class, () -> {
            servicioPartida.iniciarPartida(codigo, 1L);
        });

        // (Si tu proyecto usa JUnit 4 con @Test(expected = ...), puedes usar esa anotación en lugar de assertThrows)
    }

    @Test
    public void dadoQueUnUsuarioNoEsCreadorCuandoSeIniciaPartidaYDaExcepcion() {
        String codigo = "SALA01";
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico(codigo);

        Usuario creador = new Usuario();
        creador.setId(1L);

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(2L);

        sala.setCreador(creador);
        sala.getUsuarios().addAll(List.of(creador, otroUsuario));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(codigo)).thenReturn(sala);

        // Intenta iniciar el usuario con id 2L (no es el creador)
        assertThrows(IllegalArgumentException.class, () -> {
            servicioPartida.iniciarPartida(codigo, 2L);
        });
    }

    @Test
    public void dadoQuePartidaEstaEnCursoCuandoSeIntentaIniciarYDaPartidaIniciadaException() {
        String codigo = "SALA01";
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico(codigo);

        Usuario creador = new Usuario();
        creador.setId(1L);
        creador.setFicha(ColorFicha.ROJO);

        Usuario invitado = new Usuario();
        invitado.setId(2L);
        invitado.setFicha(ColorFicha.AZUL);

        sala.setCreador(creador);
        sala.getUsuarios().addAll(List.of(creador, invitado));

        // Simulamos que ya existe una partida en curso para este código
        Partida partidaExistente = new Partida();
        partidaExistente.setCodigoUnico(codigo);
        partidaExistente.setEstado(EstadoPartida.EN_CURSO); // o como identifiques que ya está iniciada

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(codigo)).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partidaExistente);

        assertThrows(PartidaIniciadaException.class, () -> {
            servicioPartida.iniciarPartida(codigo, 1L);
        });
    }

    @Test
    public void dadoQueUnJugadorAbandonaCuandoSeProcesaElAbandonoYDaJugadorActualizadoYEventoNotificado() throws PartidaNoEncontradaException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoPrueba = "MATANZA123";
        Long idJugador = 1L;

        Partida partidaSimulada = new Partida();
        partidaSimulada.setCodigoUnico(codigoPrueba);

        Jugador jugadorSimulado = new Jugador();
        jugadorSimulado.setId(idJugador);
        jugadorSimulado.setAbandono(false);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);
        when(repositorioPartidaMock.buscarJugadorPorId(idJugador)).thenReturn(jugadorSimulado);

        servicioPartida.abandonarPartida(codigoPrueba, idJugador);

        assertTrue(jugadorSimulado.getAbandono());
        verify(repositorioPartidaMock).actualizarJugador(jugadorSimulado);
        verify(publicadorAbandonoMock).notificar(any(EventoJugadorAbandonado.class));
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeIntentaAbandonarYDaPartidaNoEncontradaException() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        String codigoInexistente = "ERROR404";
        Long idJugador = 1L;

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoInexistente)).thenReturn(null);

        assertThrows(PartidaNoEncontradaException.class, () -> {
            servicioPartida.abandonarPartida(codigoInexistente, idJugador);
        });

        verify(repositorioPartidaMock, never()).actualizarJugador(any());
        verify(publicadorAbandonoMock, never()).notificar(any());
    }

    //TEST DE PAUSAR LA PARTIDA CUANDO COMIENZA UN EVENTO GLOBAL
    @Test
    public void dadoQueSeActivaEventoGlobalCuandoFinalizaRondaYDaPartidaPausada() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException  {
        // Preparación
        String codigo = "MATANZA123";

        Jugador jugadorUno = new Jugador();
        jugadorUno.setOrden(0);
        jugadorUno.setEnBancarrota(false);

        Jugador jugadorDos = new Jugador();
        jugadorDos.setOrden(1);
        jugadorDos.setEnBancarrota(false);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(1);
        partida.setIndiceTurnoActual(0);
        partida.setJugadores(List.of(jugadorUno, jugadorDos));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(codigo)).thenReturn(false, true);

        // Ejecución
        this.servicioPartida.avanzarTurno(codigo);

        // Comprobación
        assertEquals(5, partida.getRondaActual());
        assertEquals(1, partida.getIndiceTurnoActual());

        verify(servicioEventoMock).verificarActivacionEventoGlobal(partida);

        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(codigo),eq(30),any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    //TEST IMPEDIR AVANZAR TURNOS DURANTE UN EVENTO GLOBAL
    @Test
    public void dadoQueHayEventoGlobalActivoCuandoSeIntentaAvanzarYDaTurnoSinCambios()
        throws Exception {
        // Preparación
        String codigo = "MATANZA123";

        Jugador jugadorUno = new Jugador();
        jugadorUno.setOrden(0);
        jugadorUno.setEnBancarrota(false);

        Jugador jugadorDos = new Jugador();
        jugadorDos.setOrden(1);
        jugadorDos.setEnBancarrota(false);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setRondaActual(5);
        partida.setTurnosFinalizadosEnRonda(0);
        partida.setIndiceTurnoActual(0);
        partida.setJugadores(List.of(jugadorUno, jugadorDos));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(codigo)).thenReturn(true);

        // Ejecución
        this.servicioPartida.avanzarTurno(codigo);

        // Comprobación
        assertEquals(5, partida.getRondaActual());
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());

        verify(repositorioPartidaMock, never()).guardarPartida(partida);
    }

    @Test
    public void dadoQueHayEventoGlobalActivoCuandoSeIniciaTemporizadorYDaRelojBloqueado() {

        String codigo = "MATANZA123";

        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(codigo)).thenReturn(true);

        this.servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);

        verify(gestorTemporizadorMock, never()).programarTemporizadorSiNoExiste(eq(codigo),eq(30),any(Runnable.class));


    }


    // TESTS ADICIONALES DE SERVICIO PARTIDA Y EVENTOS GLOBALES

    @Test
    public void dadoQueExisteJugadorCuandoSeActualizaYDaRepositorioInvocado() {
        Jugador jugador = new Jugador();
        jugador.setId(8L);

        servicioPartida.actualizarJugador(jugador);

        verify(repositorioPartidaMock).actualizarJugador(jugador);
    }

    @Test
    public void dadoQueExisteJugadorCuandoSeBuscaPorIdYDaJugadorEncontrado() {
        Jugador jugador = new Jugador();
        jugador.setId(8L);
        when(repositorioPartidaMock.buscarJugadorPorId(8L)).thenReturn(jugador);

        Jugador encontrado = servicioPartida.obtenerJugadorPorId(8L);

        assertEquals(jugador, encontrado);
        verify(repositorioPartidaMock).buscarJugadorPorId(8L);
    }

    @Test
    public void dadoQueQuedanDiezSegundosCuandoSeConsultanYDaDiez() {
        when(gestorTemporizadorMock.obtenerSegundosRestantes(CODIGO)).thenReturn(10);

        Integer restantes = servicioPartida.obtenerSegundosRestantesTurno(CODIGO);

        assertEquals(10, restantes);
        verify(gestorTemporizadorMock).obtenerSegundosRestantes(CODIGO);
    }

    @Test
    public void dadoQueEventoGlobalEstaActivoCuandoSeConsultaYDaVerdadero() {
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(true);

        Boolean activo = servicioPartida.verificarSiHayEventoGlobalActivo(CODIGO);

        assertEquals(true, activo);
        verify(servicioEventoMock).verificarSiHayEventoGlobalActivo(CODIGO);
    }

    @Test
    public void dadoQueNoExisteEventoGlobalCuandoSeConsultaYDaFalso() {
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(false);

        Boolean activo = servicioPartida.verificarSiHayEventoGlobalActivo(CODIGO);

        assertEquals(false, activo);
        verify(servicioEventoMock).verificarSiHayEventoGlobalActivo(CODIGO);
    }

    @Test
    public void dadoQueLaConsultaDeEventoDevuelveNullCuandoSeVerificaYDaFalso() {
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(null);

        Boolean activo = servicioPartida.verificarSiHayEventoGlobalActivo(CODIGO);

        assertEquals(false, activo);
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeIniciaTemporizadorYDaSinProgramacion() {
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(null);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verify(gestorTemporizadorMock, never())
            .programarTemporizadorSiNoExiste(eq(CODIGO), eq(30), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueLaPartidaNoTieneEventoCuandoSeIniciaTemporizadorYDaRelojProgramado() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(false);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(true);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq(CODIGO), any(TurnoDTO.class));
        verify(gestorTemporizadorMock, never()).programarTemporizador(
            eq(CODIGO), anyInt(), any(Runnable.class));
    }

    @Test
    public void dadoQueElTemporizadorYaExisteCuandoSeIntentaIniciarYDaSinOtraNotificacion() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(false);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueElTemporizadorDevuelveNullCuandoSeIntentaIniciarYDaSinNotificacion() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(null);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueTerminaElEventoGlobalCuandoSeReanudaYDaTemporizadorNormal() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(true, false);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(true);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verify(gestorTemporizadorMock, times(1)).programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq(CODIGO), any(TurnoDTO.class));
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSePreparaTiempoTurnoYDaSinNotificacion() {
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(null);

        servicioPartida.prepararTiempoTurno(CODIGO, 5);

        verifyNoInteractions(gestorTemporizadorMock, notificadorPartidaMock);
    }

    @Test
    public void dadoQueLaPartidaExisteCuandoSePreparaTiempoTurnoYDaDuracionDePresentacion() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);

        servicioPartida.prepararTiempoTurno(CODIGO, 8);

        verify(gestorTemporizadorMock).programarTemporizador(
            eq(CODIGO), eq(38), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq(CODIGO), any(TurnoDTO.class));
    }

    @Test
    public void dadoQueLaPosicionEsNulaCuandoSeConsultanFichasYDaPosicionInicial() {
        Partida partida = new Partida();
        Jugador jugador = new Jugador();
        jugador.setId(5L);
        jugador.setApodo("Primero");
        jugador.setPosicionActual(null);
        jugador.setFicha(ColorFicha.ROJO);
        partida.setJugadores(List.of(jugador));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);

        List<PosicionFicha> posiciones = servicioPartida.obtenerPosicionesFichas(CODIGO);

        assertEquals(1, posiciones.size());
        assertEquals(0, posiciones.get(0).getPosicion());
        assertEquals("ROJO", posiciones.get(0).getColor());
        assertEquals(5L, posiciones.get(0).getIdJugador());
        assertEquals("Primero", posiciones.get(0).getApodo());
    }

    @Test
    public void dadoQueLaFichaEsNulaCuandoSeConsultanPosicionesYDaColorNulo() {
        Partida partida = new Partida();
        Jugador jugador = new Jugador();
        jugador.setId(5L);
        jugador.setPosicionActual(4);
        jugador.setFicha(null);
        partida.setJugadores(List.of(jugador));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);

        List<PosicionFicha> posiciones = servicioPartida.obtenerPosicionesFichas(CODIGO);

        assertEquals(4, posiciones.get(0).getPosicion());
        assertEquals(null, posiciones.get(0).getColor());
    }

    @Test
    public void dadoQuePartidaEstaEnRondaCuatroCuandoNoCambiaRondaYDaEventoSinVerificar() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(0);
        partida.setIndiceTurnoActual(0);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);

        servicioPartida.avanzarTurno(CODIGO);

        assertEquals(4, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
        assertEquals(1, partida.getIndiceTurnoActual());
        verify(servicioEventoMock, never()).verificarActivacionEventoGlobal(partida);
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(CODIGO), eq(30), any(Runnable.class));
    }

    @Test
    public void dadoQuePartidaCompletaRondaCuandoNoSeActivaEventoYDaTemporizadorNormal() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(3);
        partida.setTurnosFinalizadosEnRonda(1);
        partida.setIndiceTurnoActual(0);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(false);

        servicioPartida.avanzarTurno(CODIGO);

        assertEquals(4, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(1, partida.getIndiceTurnoActual());
        verify(servicioEventoMock).verificarActivacionEventoGlobal(partida);
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(CODIGO), eq(30), any(Runnable.class));
    }

    @Test
    public void dadoQueElEventoEstaActivoCuandoSeBloqueaTurnoYDaSinCancelarTemporizador() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(5);
        partida.setIndiceTurnoActual(0);
        Jugador jugador = new Jugador();
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(true);

        servicioPartida.avanzarTurno(CODIGO);

        assertEquals(5, partida.getRondaActual());
        assertEquals(0, partida.getIndiceTurnoActual());
        verify(gestorTemporizadorMock, never()).cancelarTemporizador(CODIGO);
        verify(servicioEventoMock, never()).verificarActivacionEventoGlobal(partida);
        verify(repositorioPartidaMock, never()).guardarPartida(partida);
    }

    @Test
    public void dadoQueElSiguienteJugadorYaTiroCuandoSeAvanzaTurnoYDaTiradaReiniciada() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(0);
        partida.setIndiceTurnoActual(0);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        segundo.setTiroEnTurno(true);
        partida.setJugadores(List.of(primero, segundo));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);

        servicioPartida.avanzarTurno(CODIGO);

        assertEquals(false, segundo.getTiroEnTurno());
        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void dadoQueEventoGlobalLanzaExcepcionCuandoSeVerificaActivacionYDaIllegalStateException() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(1);
        partida.setIndiceTurnoActual(0);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        doThrow(new EventoGlobalActivoException("Evento activo"))
            .when(servicioEventoMock).verificarActivacionEventoGlobal(partida);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> servicioPartida.avanzarTurno(CODIGO));

        assertEquals(EventoGlobalActivoException.class, error.getCause().getClass());
        verify(servicioEventoMock).verificarActivacionEventoGlobal(partida);
        verify(repositorioPartidaMock, never()).guardarPartida(partida);
    }

    @Test
    public void dadoQueNoExisteLaPartidaDelEventoCuandoSeVerificaActivacionYDaIllegalStateException() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(1);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        doThrow(new PartidaNoEncontradaException())
            .when(servicioEventoMock).verificarActivacionEventoGlobal(partida);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> servicioPartida.avanzarTurno(CODIGO));

        assertEquals(PartidaNoEncontradaException.class, error.getCause().getClass());
        verify(servicioEventoMock).verificarActivacionEventoGlobal(partida);
    }

    @Test
    public void dadoQueNoExisteJugadorCuandoSeIntentaAbandonarYDaIllegalArgumentException() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(repositorioPartidaMock.buscarJugadorPorId(44L)).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> servicioPartida.abandonarPartida(CODIGO, 44L));

        assertEquals("El jugador especificado no existe.", error.getMessage());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }

    @Test
    public void dadoQueElPublicadorLanzaExcepcionCuandoSeNotificaTurnoYDaIllegalStateException() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        PublicadorEventos<EventoTurnoCambiado> publicadorTurnoMock = spy(new PublicadorEventos<EventoTurnoCambiado>(List.of()));
        ServicioPartida servicioConPublicadorFallando = new ServicioPartidaImpl(
            repositorioPartidaMock,
            new ServicioInicioPartidaImpl(repositorioPartidaMock, repositorioSalaDeEsperaMock,
                new ServicioTurnoImpl(new ServicioParticipacionImpl())),
            gestorTemporizadorMock,
            new ServicioTurnoImpl(new ServicioParticipacionImpl()),
            publicadorTurnoMock, publicadorAbandonoMock, servicioEventoMock
        );

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(true);
        doThrow(new PartidaNoEncontradaException())
            .when(publicadorTurnoMock).notificar(any(EventoTurnoCambiado.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> servicioConPublicadorFallando.iniciarTemporizadorTurnoSiNoExiste(CODIGO));

        assertEquals(PartidaNoEncontradaException.class, error.getCause().getClass());
        verify(publicadorTurnoMock).notificar(any(EventoTurnoCambiado.class));
    }


    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSePreparaTiempoYDaSinTemporizador() {
        Partida partida = new Partida();
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(null)).thenReturn(partida);

        servicioPartida.prepararTiempoTurno(null, 5);

        verify(gestorTemporizadorMock, never()).programarTemporizador(
            any(), anyInt(), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(
            eq(null), any(TurnoDTO.class));
    }

    // TESTS DE PAUSA Y REANUDACIÓN CON EVENTOS GLOBALES

    @Test
    public void dadoQueHayEventoGlobalActivoCuandoSePreparaTiempoTurnoYDaSinTemporizadorNiNotificacion() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(true);

        servicioPartida.prepararTiempoTurno(CODIGO, 8);

        verify(servicioEventoMock).verificarSiHayEventoGlobalActivo(CODIGO);
        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(CODIGO), anyInt(), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueSeActivaEventoGlobalCuandoAvanzaLaRondaYDaTurnoPreparadoSinNotificacionNormal() throws Exception {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(1);
        partida.setIndiceTurnoActual(0);

        Jugador jugadorUno = new Jugador();
        jugadorUno.setId(1L);
        jugadorUno.setOrden(0);

        Jugador jugadorDos = new Jugador();
        jugadorDos.setId(2L);
        jugadorDos.setOrden(1);
        jugadorDos.setTiroEnTurno(true);

        partida.setJugadores(List.of(jugadorUno, jugadorDos));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(false, true);

        servicioPartida.avanzarTurno(CODIGO);

        assertEquals(5, partida.getRondaActual());
        assertEquals(1, partida.getIndiceTurnoActual());
        assertEquals(false, jugadorDos.getTiroEnTurno());
        verify(servicioEventoMock).verificarActivacionEventoGlobal(partida);
        verify(repositorioPartidaMock).guardarPartida(partida);
        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(CODIGO), anyInt(), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueFinalizoEventoGlobalCuandoSeIntentaReanudarDosVecesYDaUnaNotificacion() {
        Partida partida = new Partida();
        partida.setCodigoUnico(CODIGO);

        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setApodo("Jugador1");
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(CODIGO)).thenReturn(partida);
        when(servicioEventoMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(false);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(eq(CODIGO), eq(30), any(Runnable.class))).thenReturn(true, false);

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(CODIGO);

        verify(gestorTemporizadorMock, times(2)).programarTemporizadorSiNoExiste(eq(CODIGO), eq(30), any(Runnable.class));
        verify(notificadorPartidaMock, times(1)).notificarCambioDeTurno(eq(CODIGO), any(TurnoDTO.class));
        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(CODIGO), anyInt(), any(Runnable.class));
    }

}
