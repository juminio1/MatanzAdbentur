package com.tallerwebi.dominio;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
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
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.ServicioInicioPartidaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;

import java.util.Arrays;
import java.util.List;

import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;
    private NotificadorPartida notificadorPartidaMock;
    private GestorTemporizadorPartida gestorTemporizadorMock;
    private ServicioPartida servicioPartida;
    private PublicadorEventos<EventoJugadorAbandonado> publicadorAbandonoMock;
    private ServicioFinalizacionPartida finalizacion;
    private org.springframework.transaction.PlatformTransactionManager transacciones;

    private static final int TOTAL_CASILLEROS = 30;

    @Test
    public void visualizacionConsultaTodosLosEstadosSinEfectos() {
        for (EstadoPartida estado : EstadoPartida.values()) {
            Partida partida = new Partida();
            partida.setEstado(estado);
            when(repositorioPartidaMock.buscarPartidaPorCodigoUnico("VISTA")).thenReturn(partida);

            org.junit.jupiter.api.Assertions.assertSame(partida,
                    servicioPartida.obtenerPartidaParaVisualizacion("VISTA"));

            assertEquals(estado, partida.getEstado());
        }
        verify(repositorioPartidaMock, times(EstadoPartida.values().length))
                .buscarPartidaPorCodigoUnico("VISTA");
        verifyNoMoreInteractions(repositorioPartidaMock);
        verifyNoInteractions(finalizacion, gestorTemporizadorMock, notificadorPartidaMock,
                publicadorAbandonoMock);
    }

    @Test
    public void visualizacionInexistenteDevuelveNull() {
        org.junit.jupiter.api.Assertions.assertNull(servicioPartida.obtenerPartidaParaVisualizacion("NO-EXISTE"));
        verify(repositorioPartidaMock).buscarPartidaPorCodigoUnico("NO-EXISTE");
        verifyNoMoreInteractions(repositorioPartidaMock);
    }

    @Test
    public void visualizacionEsTransaccionalDeSoloLectura() throws Exception {
        var transaccion = ServicioPartidaImpl.class.getMethod("obtenerPartidaParaVisualizacion", String.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNotNull(transaccion);
        assertTrue(transaccion.readOnly());
    }

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        gestorTemporizadorMock = mock(GestorTemporizadorPartida.class);
        publicadorAbandonoMock = mock(PublicadorEventos.class);
        finalizacion = mock(ServicioFinalizacionPartida.class);
        transacciones = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(transacciones.getTransaction(any()))
                .thenReturn(mock(org.springframework.transaction.TransactionStatus.class));

        servicioPartida = new ServicioPartidaImpl(
            repositorioPartidaMock,
            new ServicioInicioPartidaImpl(repositorioPartidaMock, repositorioSalaDeEsperaMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl())),
            gestorTemporizadorMock,
            new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()),
            new PublicadorEventos<>(List.of(new ObservadorTurnoWebSocket(notificadorPartidaMock))),
            publicadorAbandonoMock, null, null, finalizacion, transacciones
        );
        org.springframework.test.util.ReflectionTestUtils.setField(servicioPartida, "participacion",
                new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }

    @Test
    public void dadoQueHayDosJugadoresCuandoSeInicializanTurnosDaComoResultadoUnOrdenParaCadaUno() {
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
    public void alInicializarTurnosSeRestablecenJugadoresYSeConservaElIndiceExistente() {
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
    public void queSeObtenganLasPosicionesYSeMuestrenTodosLosJugadoresPorCodigo() {
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
    public void alInicializarTurnosDeberiaAsignarOrdenACadaJugadorYComenzarEnCero() {
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
    public void dadoQueElIndiceActualEsUnoCuandoSeObtieneJugadorActualDaComoResultadoJugadorDos() {
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
    public void dadoQueEsTurnoDelPrimerJugadorCuandoSeAvanzaTurnoDaComoResultadoElSegundoJugador() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setEstado(EstadoPartida.EN_CURSO);
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
    public void lasOperacionesDeTurnoIgnoranPartidasSinJugadores() {
        String codigo = "SIN-JUGADORES";
        Partida sinLista = new Partida();
        sinLista.setEstado(EstadoPartida.EN_CURSO);
        sinLista.setJugadores(null);
        Partida listaVacia = new Partida();
        listaVacia.setEstado(EstadoPartida.EN_CURSO);
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
    public void alAvanzarTurnoSeOmiteAlJugadorEnBancarrota() {
        String codigo = "BANCARROTA";
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
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
    public void alIniciarTemporizadorSinJugadorActivoNoSeNotificaTurno() {
        String codigo = "SIN-JUGADORES";
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
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
    public void alAvanzarTurnoSinNotificadorSeProgramaElTemporizadorIgualmente() {
        String codigo = "SIN-NOTIFICADOR";
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setCodigoUnico(codigo);
        Jugador jugador = new Jugador();
        jugador.setOrden(0);
        partida.setJugadores(List.of(jugador));
        partida.setIndiceTurnoActual(null);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);
        ServicioPartida servicioSinNotificador = new ServicioPartidaImpl(
            repositorioPartidaMock,
            new ServicioInicioPartidaImpl(repositorioPartidaMock, repositorioSalaDeEsperaMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl())),
            gestorTemporizadorMock, new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()), null, publicadorAbandonoMock, null, null, finalizacion, transacciones);

        org.springframework.test.util.ReflectionTestUtils.setField(servicioSinNotificador, "participacion",
                new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        servicioSinNotificador.avanzarTurno(codigo);

        assertEquals(0, partida.getIndiceTurnoActual());
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(codigo), eq(30), any(Runnable.class));
        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void alAvanzarConTodosPenalizadosSeRestablecenPenalizacionesYContinuaLaPartida() {
        String codigo = "TODOS-PENALIZADOS";
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setCodigoUnico(codigo);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        primero.setPierdeTurno(true);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        segundo.setPierdeTurno(true);
        partida.setJugadores(List.of(primero, segundo));
        partida.setIndiceTurnoActual(0);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);

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
    public void alAvanzarConTodosEnBancarrotaSeGuardaSinCambiarTurnoNiProgramarTemporizador() {
        String codigo = "TODOS-EN-BANCARROTA";
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
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
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo))
            .thenReturn(partida);

        servicioPartida.avanzarTurno(codigo);

        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(3, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
        assertTrue(segundo.getPierdeTurno());
        verify(repositorioPartidaMock).guardarPartida(partida);
        verify(gestorTemporizadorMock, times(2)).cancelarTemporizador(codigo);
        verify(gestorTemporizadorMock, never()).programarTemporizador(
            any(), anyInt(), any(Runnable.class));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    public void dadoQueSeAvanzaTurnoSeNotificaNuevoTurnoConDuracion() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueEsElUltimoJugadorCuandoSeAvanzaTurnoDaComoResultadoElPrimero() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueElSiguienteJugadorPierdeTurnoCuandoSeAvanzaDaComoResultadoQueSeLoSaltea() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        partidaSimulada.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueExisteUnaPartidaCuandoSeIniciaDaComoResultadoJugadoresInicializados()
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
    public void dadoQueSeIniciaPartidaSeNotificaTurnoInicialConDuracion()
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
    public void dadoQueNoHayJugadoresCuandoSeIntentaIniciarPartidaDaComoResultadoUnaExcepcion() {
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
    public void dadoQueHaySalaConCuatroUsuariosCuandoSeIniciaPartidaDaComoResultadoPartidaCreada()
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
    public void dadoQueExisteUnaPartidaConJugadoresSeSeleccionaAleatoriamenteElTurnoInicialExitosamente()
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
    public void dadoQueFaltanJugadoresCuandoSeIncrementanRondasDaComoResultadoMismaRonda() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueTodosJugaronCuandoSeIncrementanRondasDaComoResultadoNuevaRonda() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(3);
        jugadorUno.setUltimaRondaConOportunidad(1);
        jugadorDos.setUltimaRondaConOportunidad(1);
        jugadorTres.setUltimaRondaConOportunidad(1);
        jugadorCuatro.setOrden(3);
        partida.setIndiceTurnoActual(3);

        servicioPartida.incrementarRondas(partida);

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
    }

    @Test
    public void dadoQueLosContadoresSonNulosCuandoSeIncrementanRondasDaComoResultadoValoresInicializados() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueHayCuatroJugadoresCuandoTodosFinalizanSuTurnoDaComoResultadoNuevaRonda() {
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
        partida.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueHayJugadoresCuandoSeObtieneLaPartidaPorCodigoDaComoResultadoSusPosiciones() {
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
    public void dadoQueHayMasDeCuatroJugadoresCuandoSeIniciaLaPartidaDaComoResultadoCantidadMaximaSuperada() {
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
    public void capturaElAbandonoAntesDeMarcarActualizarYPublicar() throws PartidaNoEncontradaException {
        Jugador jugador = spy(new Jugador());
        jugador.setId(1L);
        Partida partida = prepararPartidaParaCapturarAbandono(jugador);
        clearInvocations(jugador);

        servicioPartida.abandonarPartida("CAPTURA", 1L);

        org.mockito.InOrder orden = inOrder(finalizacion, jugador, repositorioPartidaMock,
                publicadorAbandonoMock);
        orden.verify(finalizacion).conservarResultadoEconomico(
                partida, jugador, EstadoParticipante.ABANDONADO);
        orden.verify(jugador).setAbandono(true);
        orden.verify(repositorioPartidaMock).actualizarJugador(jugador);
        orden.verify(publicadorAbandonoMock).notificar(new EventoJugadorAbandonado(partida, jugador));
    }

    @Test
    public void siFallaLaCapturaNoMarcaNiPublicaElAbandono() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(500);
        Partida partida = prepararPartidaParaCapturarAbandono(jugador);
        IllegalStateException fallo = new IllegalStateException("No se pudo guardar");
        when(finalizacion.conservarResultadoEconomico(
                partida, jugador, EstadoParticipante.ABANDONADO)).thenThrow(fallo);

        org.junit.jupiter.api.Assertions.assertSame(fallo,
                assertThrows(IllegalStateException.class,
                        () -> servicioPartida.abandonarPartida("CAPTURA", 1L)));

        assertFalse(jugador.getAbandono());
        assertEquals(500, jugador.getDinero());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
        verifyNoInteractions(publicadorAbandonoMock);
    }

    @Test
    public void abandonoRepetidoConservaLaFotografiaOriginal() throws PartidaNoEncontradaException {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(500);
        Partida partida = prepararPartidaParaCapturarAbandono(jugador);
        partida.setId(10L);
        com.tallerwebi.infraestructura.RepositorioPropiedad propiedades =
                mock(com.tallerwebi.infraestructura.RepositorioPropiedad.class);
        com.tallerwebi.infraestructura.RepositorioDeudaPendiente deudas =
                mock(com.tallerwebi.infraestructura.RepositorioDeudaPendiente.class);
        com.tallerwebi.infraestructura.RepositorioFinalizarPartida resultados =
                mock(com.tallerwebi.infraestructura.RepositorioFinalizarPartida.class);
        ServicioFinalizacionPartida capturaReal =
                new com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl(
                        propiedades, deudas, resultados,
                        new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        when(finalizacion.conservarResultadoEconomico(
                partida, jugador, EstadoParticipante.ABANDONADO))
                .thenAnswer(invocacion -> capturaReal.conservarResultadoEconomico(
                        partida, jugador, EstadoParticipante.ABANDONADO));
        servicioPartida.abandonarPartida("CAPTURA", 1L);
        ArgumentCaptor<com.tallerwebi.dominio.entidades.FinalizarPartida> captura =
                ArgumentCaptor.forClass(com.tallerwebi.dominio.entidades.FinalizarPartida.class);
        verify(resultados).guardar(captura.capture());
        com.tallerwebi.dominio.entidades.FinalizarPartida original = captura.getValue();
        when(resultados.buscarPorPartidaYJugador(10L, 1L)).thenReturn(original);
        jugador.setDinero(0);
        clearInvocations(propiedades, deudas);

        servicioPartida.abandonarPartida("CAPTURA", 1L);

        assertEquals(500, original.getDineroFinal());
        assertEquals(500, original.getPatrimonioTotal());
        assertEquals(EstadoParticipante.ABANDONADO, original.getEstadoParticipante());
        verify(resultados, times(1)).guardar(any());
        verifyNoInteractions(propiedades, deudas);
    }

    private Partida prepararPartidaParaCapturarAbandono(Jugador jugador) {
        Partida partida = new Partida();
        partida.setCodigoUnico("CAPTURA");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.getJugadores().add(jugador);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("CAPTURA")).thenReturn(partida);
        when(repositorioPartidaMock.buscarJugadorPorId(jugador.getId())).thenReturn(jugador);
        return partida;
    }

@Test
public void dadoQueLosUsuariosTienenFichasSeleccionadasCuandoSeIniciaLaPartidaDaComoResultadoLasFichasConvertidas() {
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
    public void queFalleAlIniciarPartidaSiUnJugadorNoTieneFicha() {
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
    public void queFalleAlIniciarPartidaSiUsuarioNoEsElCreador() {
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
    public void queFalleAlIniciarPartidaSiYaEstaEnCurso() {
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
    public void dadoQueUnJugadorAbandonaLaPartidaSeNotificaElEventoYSeActualizaElJugador() throws PartidaNoEncontradaException {
        String codigoPrueba = "MATANZA123";
        Long idJugador = 1L;

        Partida partidaSimulada = new Partida();
        partidaSimulada.setEstado(EstadoPartida.EN_CURSO);
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
    public void dadoQueLaPartidaNoExisteCuandoUnJugadorIntentaAbandonarSeLanzaExcepcion() {
        String codigoInexistente = "ERROR404";
        Long idJugador = 1L;

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoInexistente)).thenReturn(null);

        assertThrows(PartidaNoEncontradaException.class, () -> {
            servicioPartida.abandonarPartida(codigoInexistente, idJugador);
        });

        verify(repositorioPartidaMock, never()).actualizarJugador(any());
        verify(publicadorAbandonoMock, never()).notificar(any());
    }

    @Test
    public void siElTurnoYaAvanzoCuandoVenceElTemporizadorNoSeAvanzaOtraVez() {
        String codigo = "VENCE-TARDE";
        Partida partida = crearPartidaConDosJugadoresEnTurnoCero(codigo);
        Runnable vencimiento = iniciarTemporizadorYCapturarVencimiento(codigo);

        servicioPartida.avanzarTurno(codigo);
        assertEquals(1, partida.getIndiceTurnoActual());

        vencimiento.run();

        assertEquals(1, partida.getIndiceTurnoActual());
        verify(repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void siElTurnoSigueVigenteCuandoVenceElTemporizadorSeAvanza() {
        String codigo = "VENCE-A-TIEMPO";
        Partida partida = crearPartidaConDosJugadoresEnTurnoCero(codigo);
        Runnable vencimiento = iniciarTemporizadorYCapturarVencimiento(codigo);

        vencimiento.run();

        assertEquals(1, partida.getIndiceTurnoActual());
        verify(repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    private Partida crearPartidaConDosJugadoresEnTurnoCero(String codigo) {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setCodigoUnico(codigo);
        Jugador primero = new Jugador();
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setOrden(1);
        partida.setJugadores(List.of(primero, segundo));
        partida.setIndiceTurnoActual(0);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);
        return partida;
    }

    private Runnable iniciarTemporizadorYCapturarVencimiento(String codigo) {
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(codigo), eq(30), any(Runnable.class))).thenReturn(true);
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(eq(codigo), eq(30), captor.capture());
        return captor.getValue();
    }


    @Test
    public void rondaNormalEsperaElTurnoDeLosTresJugadores() {
        Partida partida = prepararRondaConTresJugadores("RONDA-NORMAL");

        servicioPartida.avanzarTurno("RONDA-NORMAL");
        assertEquals(1, partida.getRondaActual());
        assertEquals(1, partida.getIndiceTurnoActual());

        servicioPartida.avanzarTurno("RONDA-NORMAL");
        assertEquals(1, partida.getRondaActual());
        assertEquals(2, partida.getIndiceTurnoActual());

        servicioPartida.avanzarTurno("RONDA-NORMAL");
        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    @Test
    public void quiebraDuranteElTurnoNoCierraLaRondaAntesDelUltimoActivo() {
        Partida partida = prepararRondaConTresJugadores("RONDA-QUIEBRA");
        servicioPartida.avanzarTurno("RONDA-QUIEBRA");
        Jugador quebrado = partida.getJugadores().get(1);
        quebrado.setEnBancarrota(true);
        com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra observador =
                new com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra(
                        servicioPartida, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());

        observador.actualizar(new com.tallerwebi.dominio.eventos.EventoJugadorQuebrado(partida, quebrado));

        assertEquals(2, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());

        servicioPartida.avanzarTurno("RONDA-QUIEBRA");

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    @Test
    public void abandonoDuranteElTurnoNoCierraLaRondaAntesDelUltimoActivo() {
        Partida partida = prepararRondaConTresJugadores("RONDA-ABANDONO");
        servicioPartida.avanzarTurno("RONDA-ABANDONO");
        Jugador abandonado = partida.getJugadores().get(1);
        abandonado.setAbandono(true);
        com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono observador =
                new com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono(
                        servicioPartida, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());

        observador.actualizar(new EventoJugadorAbandonado(partida, abandonado));

        assertEquals(2, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());

        servicioPartida.avanzarTurno("RONDA-ABANDONO");

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    @Test
    public void eliminarFueraDeTurnoAQuienYaJugoNoAdelantaElCierreDeLaRonda() {
        Partida partida = prepararRondaConTresJugadores("RONDA-YA-JUGO");
        servicioPartida.avanzarTurno("RONDA-YA-JUGO");
        Jugador eliminado = partida.getJugadores().get(0);
        eliminado.setEnBancarrota(true);
        com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra observador =
                new com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra(
                        servicioPartida, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        observador.actualizar(new com.tallerwebi.dominio.eventos.EventoJugadorQuebrado(partida, eliminado));
        assertEquals(1, partida.getIndiceTurnoActual());

        servicioPartida.avanzarTurno("RONDA-YA-JUGO");

        assertEquals(2, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());

        servicioPartida.avanzarTurno("RONDA-YA-JUGO");

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(1, partida.getIndiceTurnoActual());
    }

    @Test
    public void eliminarFueraDeTurnoAQuienNoJugoPermiteCerrarSinEsperarlo() {
        Partida partida = prepararRondaConTresJugadores("RONDA-NO-JUGO");
        servicioPartida.avanzarTurno("RONDA-NO-JUGO");
        Jugador eliminado = partida.getJugadores().get(2);
        eliminado.setAbandono(true);
        com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono observador =
                new com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono(
                        servicioPartida, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        observador.actualizar(new EventoJugadorAbandonado(partida, eliminado));
        assertEquals(1, partida.getIndiceTurnoActual());

        servicioPartida.avanzarTurno("RONDA-NO-JUGO");

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    @Test
    public void vencimientoDelTemporizadorCuentaUnaOportunidadYCierraLaRonda() {
        Partida partida = crearPartidaConDosJugadoresEnTurnoCero("RONDA-TIEMPO");
        Runnable primerVencimiento = iniciarTemporizadorYCapturarVencimiento("RONDA-TIEMPO");

        primerVencimiento.run();

        assertEquals(1, partida.getRondaActual());
        assertEquals(1, partida.getTurnosFinalizadosEnRonda());
        assertEquals(1, partida.getIndiceTurnoActual());
        ArgumentCaptor<Runnable> siguienteVencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(gestorTemporizadorMock).programarTemporizador(
                eq("RONDA-TIEMPO"), eq(30), siguienteVencimiento.capture());

        siguienteVencimiento.getValue().run();

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    @Test
    public void turnoPerdidoConsumeLaOportunidadSinEsperarOtraVueltaDelJugador() {
        Partida partida = prepararRondaConTresJugadores("RONDA-PENALIZADA");
        partida.getJugadores().get(1).setPierdeTurno(true);

        servicioPartida.avanzarTurno("RONDA-PENALIZADA");

        assertEquals(2, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertFalse(partida.getJugadores().get(1).getPierdeTurno());

        servicioPartida.avanzarTurno("RONDA-PENALIZADA");

        assertEquals(2, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getIndiceTurnoActual());
    }

    private Partida prepararRondaConTresJugadores(String codigo) {
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setEstado(EstadoPartida.EN_CURSO);
        Jugador primero = new Jugador();
        primero.setId(1L);
        primero.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setId(2L);
        segundo.setOrden(1);
        Jugador tercero = new Jugador();
        tercero.setId(3L);
        tercero.setOrden(2);
        partida.setJugadores(List.of(primero, segundo, tercero));
        partida.setIndiceTurnoActual(0);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);
        return partida;
    }

    @Test
    public void rondaTreintaIncompletaContinuaSiElCierreDevuelveFalse() {
        Partida partida = prepararRondaConTresJugadores("CIERRE-INCOMPLETA");
        partida.setRondaActual(30);

        servicioPartida.avanzarTurno("CIERRE-INCOMPLETA");

        assertEquals(30, partida.getRondaActual());
        assertEquals(1, partida.getIndiceTurnoActual());
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
        verify(finalizacion, times(2)).finalizarSiCorresponde(partida);
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq("CIERRE-INCOMPLETA"), any(TurnoDTO.class));
    }

    @Test
    public void ultimaOportunidadDeLaRondaTreintaCierraAntesDeAsignarONotificarOtroTurno() {
        Partida partida = prepararRondaConTresJugadores("CIERRE-ULTIMA");
        partida.setRondaActual(30);
        partida.setIndiceTurnoActual(2);
        partida.getJugadores().get(0).setUltimaRondaConOportunidad(30);
        partida.getJugadores().get(1).setUltimaRondaConOportunidad(30);
        partida.getJugadores().get(0).setTiroEnTurno(true);
        when(finalizacion.finalizarSiCorresponde(partida)).thenAnswer(invocacion -> {
            assertEquals(31, partida.getRondaActual());
            partida.setEstado(EstadoPartida.FINALIZADA);
            return true;
        });

        servicioPartida.avanzarTurno("CIERRE-ULTIMA");

        assertEquals(EstadoPartida.FINALIZADA, partida.getEstado());
        assertEquals(2, partida.getIndiceTurnoActual());
        assertTrue(partida.getJugadores().get(0).getTiroEnTurno());
        verify(finalizacion, times(1)).finalizarSiCorresponde(partida);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verifyNoInteractions(notificadorPartidaMock);
        verify(gestorTemporizadorMock, never()).programarTemporizador(any(), any(), any());
    }

    @Test
    public void ultimaOportunidadPenalizadaCierraTrasResolverSinAsignarElCandidato() {
        Partida partida = prepararRondaConTresJugadores("CIERRE-PENALIZACION");
        partida.setRondaActual(30);
        partida.setIndiceTurnoActual(1);
        partida.getJugadores().get(0).setUltimaRondaConOportunidad(30);
        partida.getJugadores().get(0).setTiroEnTurno(true);
        partida.getJugadores().get(2).setPierdeTurno(true);
        when(finalizacion.finalizarSiCorresponde(partida)).thenAnswer(invocacion -> {
            if (partida.getRondaActual() > 30) {
                partida.setEstado(EstadoPartida.FINALIZADA);
                return true;
            }
            return false;
        });

        servicioPartida.avanzarTurno("CIERRE-PENALIZACION");

        assertEquals(31, partida.getRondaActual());
        assertEquals(EstadoPartida.FINALIZADA, partida.getEstado());
        assertEquals(1, partida.getIndiceTurnoActual());
        assertEquals(30, partida.getJugadores().get(2).getUltimaRondaConOportunidad());
        assertFalse(partida.getJugadores().get(2).getPierdeTurno());
        assertTrue(partida.getJugadores().get(0).getTiroEnTurno());
        verify(finalizacion, times(2)).finalizarSiCorresponde(partida);
        verifyNoInteractions(notificadorPartidaMock);
        verify(gestorTemporizadorMock, never()).programarTemporizador(any(), any(), any());
    }

    @Test
    public void siElServicioNoCierraSeContinuaSinDuplicarSuCondicionEnElAvance() {
        Partida partida = prepararRondaConTresJugadores("CIERRE-FALSE");
        partida.setRondaActual(30);
        partida.setIndiceTurnoActual(2);
        partida.getJugadores().get(0).setUltimaRondaConOportunidad(30);
        partida.getJugadores().get(1).setUltimaRondaConOportunidad(30);
        when(finalizacion.finalizarSiCorresponde(partida)).thenReturn(false);

        servicioPartida.avanzarTurno("CIERRE-FALSE");

        assertEquals(31, partida.getRondaActual());
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
        verify(finalizacion, times(2)).finalizarSiCorresponde(partida);
        verify(repositorioPartidaMock).guardarPartida(partida);
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq("CIERRE-FALSE"), any(TurnoDTO.class));
    }

    @Test
    public void unaPartidaFinalizadaNoModificaTurnosNiConsultaOtroCierre() {
        Partida partida = prepararRondaConTresJugadores("CIERRE-YA-FINALIZADA");
        partida.setEstado(EstadoPartida.FINALIZADA);

        assertThrows(IllegalStateException.class,
                () -> servicioPartida.avanzarTurno("CIERRE-YA-FINALIZADA"));

        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, partida.getJugadores().get(0).getUltimaRondaConOportunidad());
        verifyNoInteractions(finalizacion, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(gestorTemporizadorMock, never()).programarTemporizador(any(), any(), any());
    }

    @Test
    public void callbackPendienteNoProgramaNiNotificaSiLaConsultaVigenteYaNoEncuentraPartidaActiva() {
        Partida original = prepararRondaConTresJugadores("CALLBACK-CERRADO");
        List<org.springframework.transaction.support.TransactionSynchronization> callbacks =
                registrarCallbackPendiente("CALLBACK-CERRADO");
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("CALLBACK-CERRADO")).thenReturn(null);

        callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        assertEquals(EstadoPartida.EN_CURSO, original.getEstado());
        verifyNoInteractions(gestorTemporizadorMock, notificadorPartidaMock);
    }

    @Test
    public void callbackPendienteRechazaUnaPartidaVigenteFinalizadaAunqueLaCapturadaSigaEnCurso() {
        Partida original = prepararRondaConTresJugadores("CALLBACK-FINALIZADA");
        List<org.springframework.transaction.support.TransactionSynchronization> callbacks =
                registrarCallbackPendiente("CALLBACK-FINALIZADA");
        Partida actualizada = new Partida();
        actualizada.setCodigoUnico("CALLBACK-FINALIZADA");
        actualizada.setJugadores(original.getJugadores());
        actualizada.setEstado(EstadoPartida.FINALIZADA);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("CALLBACK-FINALIZADA"))
                .thenReturn(actualizada);

        callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        assertEquals(EstadoPartida.EN_CURSO, original.getEstado());
        verifyNoInteractions(gestorTemporizadorMock, notificadorPartidaMock);
    }

    @Test
    public void callbackConMarcaAntiguaNoProgramaNiPublicaUnTurnoNuevo() {
        Partida original = prepararRondaConTresJugadores("CALLBACK-ANTIGUO");
        List<org.springframework.transaction.support.TransactionSynchronization> callbacks =
                registrarCallbackPendiente("CALLBACK-ANTIGUO");
        Partida actualizada = new Partida();
        actualizada.setCodigoUnico("CALLBACK-ANTIGUO");
        actualizada.setEstado(EstadoPartida.EN_CURSO);
        actualizada.setJugadores(original.getJugadores());
        actualizada.setIndiceTurnoActual(1);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("CALLBACK-ANTIGUO"))
                .thenReturn(actualizada);

        callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        assertEquals(0, original.getIndiceTurnoActual());
        verifyNoInteractions(gestorTemporizadorMock, notificadorPartidaMock);
    }

    @Test
    public void callbackConMarcaVigenteProgramaYNotificaNormalmente() {
        prepararRondaConTresJugadores("CALLBACK-VALIDO");
        List<org.springframework.transaction.support.TransactionSynchronization> callbacks =
                registrarCallbackPendiente("CALLBACK-VALIDO");
        verifyNoInteractions(gestorTemporizadorMock, notificadorPartidaMock);

        callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        verify(gestorTemporizadorMock).programarTemporizador(
                eq("CALLBACK-VALIDO"), eq(30), any(Runnable.class));
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq("CALLBACK-VALIDO"), any(TurnoDTO.class));
    }

    @Test
    public void vencimientoTardioDePartidaFinalizadaNoAplicaPenalizacionesNiAvanza() {
        Partida original = prepararRondaConTresJugadores("VENCIMIENTO-CERRADO");
        Runnable vencimiento = iniciarTemporizadorYCapturarVencimiento("VENCIMIENTO-CERRADO");
        Partida actualizada = new Partida();
        actualizada.setCodigoUnico("VENCIMIENTO-CERRADO");
        actualizada.setJugadores(original.getJugadores());
        actualizada.setEstado(EstadoPartida.FINALIZADA);
        Jugador jugador = actualizada.getJugadores().get(0);
        jugador.getEstadoCarcel().encarcelar();
        int intentos = jugador.getEstadoCarcel().getIntentosSalirCarcel();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("VENCIMIENTO-CERRADO"))
                .thenReturn(actualizada);
        clearInvocations(gestorTemporizadorMock, notificadorPartidaMock, finalizacion);

        vencimiento.run();

        assertEquals(intentos, jugador.getEstadoCarcel().getIntentosSalirCarcel());
        assertEquals(0, actualizada.getIndiceTurnoActual());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verifyNoInteractions(finalizacion, gestorTemporizadorMock, notificadorPartidaMock);
    }

    private List<org.springframework.transaction.support.TransactionSynchronization> registrarCallbackPendiente(
            String codigo) {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            servicioPartida.prepararTiempoTurno(codigo, 0);
            return org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
    @Test
    public void avanzarTurnoRechazaEN_ESPERASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.avanzarTurno("BLOQUEO"));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void iniciarTemporizadorTurnoSiNoExisteRechazaEN_ESPERASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.iniciarTemporizadorTurnoSiNoExiste("BLOQUEO"));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void abandonarPartidaRechazaEN_ESPERASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.abandonarPartida("BLOQUEO", 1L));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void avanzarTurnoRechazaFINALIZADASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.FINALIZADA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.avanzarTurno("BLOQUEO"));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void iniciarTemporizadorTurnoSiNoExisteRechazaFINALIZADASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.FINALIZADA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.iniciarTemporizadorTurnoSiNoExiste("BLOQUEO"));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void abandonarPartidaRechazaFINALIZADASinEfectos() {
        Partida partida = prepararRondaConTresJugadores("BLOQUEO");
        partida.setEstado(EstadoPartida.FINALIZADA);
        Jugador jugador = partida.getJugadores().get(0);
        when(repositorioPartidaMock.buscarJugadorPorId(1L)).thenReturn(jugador);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(IllegalStateException.class, () -> servicioPartida.abandonarPartida("BLOQUEO", 1L));
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().isEmpty());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        assertEquals(0, partida.getIndiceTurnoActual());
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getTurnosFinalizadosEnRonda());
        assertEquals(0, jugador.getUltimaRondaConOportunidad());
        assertFalse(Boolean.TRUE.equals(jugador.getAbandono()));
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock, notificadorPartidaMock);
        verify(repositorioPartidaMock, never()).guardarPartida(any());
        verify(repositorioPartidaMock, never()).actualizarJugador(any());
    }
    @Test
    public void inicializarTurnosPermiteUnaPartidaEnEspera() {
        Partida partida = prepararRondaConTresJugadores("PREPARACION");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.getJugadores().forEach(jugador -> jugador.setOrden(null));

        servicioPartida.inicializarTurnos("PREPARACION");

        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertEquals(0, partida.getJugadores().get(0).getOrden());
        assertEquals(1, partida.getJugadores().get(1).getOrden());
        assertEquals(2, partida.getJugadores().get(2).getOrden());
        verify(repositorioPartidaMock).guardarPartida(partida);
        verifyNoInteractions(gestorTemporizadorMock, finalizacion, publicadorAbandonoMock);
    }
}
