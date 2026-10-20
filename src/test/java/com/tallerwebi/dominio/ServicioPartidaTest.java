package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.InicioPartidaNoAutorizadoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.PartidaSinJugadoresException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.servicios.GestorTemporizadorPartida;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class ServicioPartidaTest {
    private RepositorioPartida repositorioPartidaMock;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;
    private ServicioDado servicioDadoMock;
    private NotificadorPartida notificadorPartidaMock;
    private GestorTemporizadorPartida gestorTemporizadorMock;
    private ServicioPartida servicioPartida;
   // private static final Integer TOTAL_CASILLEROS = 30;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        servicioDadoMock = mock(ServicioDado.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        gestorTemporizadorMock = mock(GestorTemporizadorPartida.class);
        servicioPartida = new ServicioPartidaImpl(
            repositorioPartidaMock,
            repositorioSalaDeEsperaMock,
            servicioDadoMock,
            gestorTemporizadorMock,
            notificadorPartidaMock
        );
    }

    @Test
    public void dadoQueHayDosJugadoresCuandoSeInicializanTurnosDaComoResultadoUnOrdenParaCadaUno() throws PartidaIniciadaException {
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
    public void alInicializarTurnosDeberiaAsignarOrdenACadaJugadorYComenzarEnCero() throws PartidaIniciadaException {
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
    public void dadoQueElIndiceActualEsUnoCuandoSeObtieneJugadorActualDaComoResultadoJugadorDos() throws PartidaIniciadaException {
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
    public void dadoQueEsTurnoDelPrimerJugadorCuandoSeAvanzaTurnoDaComoResultadoElSegundoJugador() throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
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
    public void dadoQueSeAvanzaTurnoSeNotificaNuevoTurnoConDuracion() throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
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
    public void dadoQueEsElUltimoJugadorCuandoSeAvanzaTurnoDaComoResultadoElPrimero() throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
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
    public void dadoQueElSiguienteJugadorPierdeTurnoCuandoSeAvanzaDaComoResultadoQueSeLoSaltea() throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
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
    public void dadoQueExisteUnaPartidaCuandoSeIniciaDaComoResultadoJugadoresInicializados()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException {
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
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException {
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
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())).thenReturn(partida);
        when(gestorTemporizadorMock.obtenerSegundosRestantes(sala.getCodigoUnico())).thenReturn(null);
        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);
        verify(notificadorPartidaMock).notificarCambioDeTurno(eq("ABC123"),argThat((TurnoDTO dto) ->
                    dto.getIdJugadorActual() != null && dto.getDuracionSegundos() == null));
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
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException {
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
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())).thenReturn(null);
        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);
        verify(repositorioPartidaMock).guardarPartida(any(Partida.class));
    }

    @Test
    public void dadoQueExisteUnaPartidaConJugadoresSeSeleccionaAleatoriamenteElTurnoInicialExitosamente()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException {
        
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
        
        partida.setCodigoUnico(sala.getCodigoUnico());

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico())).thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);
        assertEquals(4, partida.getJugadores().size());
        Jugador jugadorSeleccionado = partida.getJugadorTurnoAleatorio();
        
        assertNotNull(jugadorSeleccionado);
        assertTrue(partida.getJugadores().contains(jugadorSeleccionado));
        assertEquals(jugadorSeleccionado.getOrden(), partida.getIndiceTurnoActual());
        assertEquals(jugadorSeleccionado, servicioPartida.obtenerJugadorActual(sala.getCodigoUnico()));
    }

    @Test
    public void dadoQueFaltanJugadoresCuandoSeIncrementanRondasDaComoResultadoMismaRonda() {
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
    public void dadoQueTodosJugaronCuandoSeIncrementanRondasDaComoResultadoNuevaRonda() {
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
    public void dadoQueLosContadoresSonNulosCuandoSeIncrementanRondasDaComoResultadoValoresInicializados() {
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
    public void dadoQueHayCuatroJugadoresCuandoTodosFinalizanSuTurnoDaComoResultadoNuevaRonda() throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
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
    public void queSePuedaSumarDineroAlJugador() {
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        servicioPartida.sumarDinero(jugador, 500);
        assertEquals(1500, jugador.getDinero());
    }

    @Test
    public void queSePuedaRestarDineroAlJugador() {
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        servicioPartida.restarDinero(jugador, 300);
        assertEquals(700, jugador.getDinero());
    }

    @Test
    public void queElJugadorActualiceSuPosicionAlMoverseUnEspacioValidoDentroDelTablero() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(5);
        int avanceExacto = 7;
        int totalCasilleros = 40;
        servicioPartida.moverJugador(jugador, avanceExacto, totalCasilleros);
        assertEquals(12, jugador.getPosicionActual());
    }

    @Test
    public void queSiLaFichaSuperaElUltimoCasilleroDelTableroContinueDesdeElComienzo() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(28);
        int avance = 5;
        int totalCasilleros = 30;
        servicioPartida.moverJugador(jugador, avance, totalCasilleros);
        assertEquals(3, jugador.getPosicionActual());
    }

    @Test
    public void queAlCaerExactamenteEnElUltimoCasilleroElJugadorVuelvaALaPosicionCero() {
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(25);
        int avance = 5;
        int totalCasilleros = 30;
        servicioPartida.moverJugador(jugador, avance, totalCasilleros);
        assertEquals(0, jugador.getPosicionActual());
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
    public void queElServicioPartidaDevuelvaLaTiradaDeDadosCorrectamente() {
        ResultadoTiradaDTO resultadoMock = new ResultadoTiradaDTO(4, 3);
        when(servicioDadoMock.tirarDados()).thenReturn(resultadoMock);
        ResultadoTiradaDTO resultado = servicioPartida.tirarDados();
        assertEquals(4, resultado.getDado1());
        assertEquals(3, resultado.getDado2());
        assertEquals(7, resultado.sumarDados());
        verify(servicioDadoMock, times(1)).tirarDados();
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
    partida.setCodigoUnico(sala.getCodigoUnico());
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
        assertThrows(InicioPartidaNoAutorizadoException.class, () -> {
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
    public void dadoQueUnJugadorTieneMilCuandoSuSaldoQuedaExactamenteEnDoscientosCincuentaDaComoResultadoUnJugadorEnBancarrota() {
        // Dado
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        jugador.setEnBancarrota(false);

        // Cuando
        servicioPartida.restarDinero(jugador, 750);

        // Entonces
        assertTrue(jugador.getEnBancarrota());
    }


    @Test
    public void dadoQueUnJugadorTieneMilCuandoSuSaldoQuedaPorEncimaDeDoscientosCincuentaDaComoResultadoUnJugadorSinBancarrota() {
        // Dado
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        jugador.setEnBancarrota(false);

        // Cuando
        servicioPartida.restarDinero(jugador, 749);

        // Entonces
        assertFalse(jugador.getEnBancarrota());
    }


    @Test
    public void dadoQueUnJugadorTieneMilCuandoSuSaldoQuedaPorDebajoDeDoscientosCincuentaDaComoResultadoUnJugadorEnBancarrota() {
        // Dado
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        jugador.setEnBancarrota(false);

        // Cuando
        servicioPartida.restarDinero(jugador, 800);

        // Entonces
        assertTrue(jugador.getEnBancarrota());
    }


    @Test
    public void dadoQueElSiguienteJugadorEstaEnBancarrotaCuandoSeAvanzaTurnoDaComoResultadoElTercerJugador()
        throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(3);
        partida.getJugadores().get(1).setEnBancarrota(true);

        // Cuando
        servicioPartida.avanzarTurno(partida.getCodigoUnico());

        // Entonces
        assertEquals(2, partida.getIndiceTurnoActual());
    }


    @Test
    public void dadoQueTodosLosJugadoresPierdenTurnoCuandoSeAvanzaDaComoResultadoUnTurnoDisponible()
        throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.getJugadores().get(0).setPierdeTurno(true);
        partida.getJugadores().get(1).setPierdeTurno(true);

        // Cuando
        servicioPartida.avanzarTurno(partida.getCodigoUnico());

        // Entonces
        assertEquals(1, partida.getIndiceTurnoActual());
    }


    @Test
    public void dadoQueTodosLosJugadoresEstanEnBancarrotaCuandoSeAvanzaTurnoDaComoResultadoEstadoPartidaInvalidoException() {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.getJugadores().get(0).setEnBancarrota(true);
        partida.getJugadores().get(1).setEnBancarrota(true);
        String codigo = partida.getCodigoUnico();

        // Cuando / Entonces
        assertThrows(EstadoPartidaInvalidoException.class, () -> servicioPartida.avanzarTurno(codigo));
    }


    @Test
    public void dadoQueLaPartidaNoTieneJugadoresCuandoSeAvanzaTurnoDaComoResultadoPartidaSinJugadoresException() {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(0);
        String codigo = partida.getCodigoUnico();

        // Cuando / Entonces
        assertThrows(PartidaSinJugadoresException.class, () -> servicioPartida.avanzarTurno(codigo));
    }


    @Test
    public void dadoQueLaListaDeJugadoresEsNullCuandoSeAvanzaTurnoDaComoResultadoPartidaSinJugadoresException() {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.setJugadores(null);
        String codigo = partida.getCodigoUnico();

        // Cuando / Entonces
        assertThrows(PartidaSinJugadoresException.class, () -> servicioPartida.avanzarTurno(codigo));
    }


    @Test
    public void dadoQueUnJugadorEstaEnBancarrotaCuandoLosActivosTerminanLaRondaDaComoResultadoUnaNuevaRonda() {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(3);
        partida.getJugadores().get(2).setEnBancarrota(true);
        partida.setTurnosFinalizadosEnRonda(1);

        // Cuando
        servicioPartida.incrementarRondas(partida);

        // Entonces
        assertEquals(2, partida.getRondaActual());
    }


    @Test
    public void dadoQueNoHayJugadoresActivosCuandoSeIncrementanRondasDaComoResultadoLaRondaSinCambios() {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.setRondaActual(3);
        partida.getJugadores().get(0).setEnBancarrota(true);
        partida.getJugadores().get(1).setEnBancarrota(true);

        // Cuando
        servicioPartida.incrementarRondas(partida);

        // Entonces
        assertEquals(3, partida.getRondaActual());
    }


    @Test
    public void dadoQueElIndiceActualEsUnoCuandoSeInicializanTurnosDaComoResultadoElMismoIndice()
        throws PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.setIndiceTurnoActual(1);

        // Cuando
        servicioPartida.inicializarTurnos(partida.getCodigoUnico());

        // Entonces
        assertEquals(1, partida.getIndiceTurnoActual());
    }


    @Test
    public void dadoQueUnJugadorEstaEnBancarrotaCuandoSeInicializanTurnosDaComoResultadoEseEstadoReiniciado()
        throws PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        Jugador jugador = partida.getJugadores().get(0);
        jugador.setEnBancarrota(true);

        // Cuando
        servicioPartida.inicializarTurnos(partida.getCodigoUnico());

        // Entonces
        assertFalse(jugador.getEnBancarrota());
    }


    @Test
    public void dadoQueLosJugadoresNoTienenOrdenCuandoSeObtieneElJugadorActualDaComoResultadoElOrdenInicializado()
        throws PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.getJugadores().get(0).setOrden(null);
        partida.getJugadores().get(1).setOrden(null);

        // Cuando
        servicioPartida.obtenerJugadorActual(partida.getCodigoUnico());

        // Entonces
        assertEquals(1, partida.getJugadores().get(1).getOrden());
    }


    @Test
    public void dadoQueElJugadorActualEstaEnBancarrotaCuandoSeLoConsultaDaComoResultadoNull()
        throws PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        partida.getJugadores().get(0).setEnBancarrota(true);

        // Cuando
        Jugador resultado = servicioPartida.obtenerJugadorActual(partida.getCodigoUnico());

        // Entonces
        assertNull(resultado);
    }


    @Test
    public void dadoQueNoExisteLaPartidaCuandoSeIniciaElTemporizadorDaComoResultadoPartidaNoEncontradaException() {
        // Dado
        String codigo = "INEXISTENTE";
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(null);

        // Cuando / Entonces
        assertThrows(PartidaNoEncontradaException.class, () ->
            servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo)
        );
    }


    @Test
    public void dadoQueExisteUnaPartidaCuandoSeIniciaElTemporizadorDaComoResultadoUnaProgramacionDeTreintaSegundos()
        throws PartidaNoEncontradaException, PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        String codigo = partida.getCodigoUnico();

        // Cuando
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);

        // Entonces
        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(
            eq(codigo), eq(30), any(Runnable.class)
        );
    }


    @Test
    public void dadoQueElTemporizadorYaExisteCuandoSeIntentaIniciarDaComoResultadoNingunaNuevaNotificacion()
        throws PartidaNoEncontradaException, PartidaIniciadaException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        String codigo = partida.getCodigoUnico();
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(
            eq(codigo), eq(30), any(Runnable.class)
        )).thenReturn(false);

        // Cuando
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);

        // Entonces
        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(
            eq(codigo), eq(30), any(Runnable.class)
        );
        verify(notificadorPartidaMock, never()).notificarCambioDeTurno(
            eq(codigo), any(TurnoDTO.class)
        );
    }


    @Test
    public void dadoQueHayUnJugadorSiguienteCuandoSeAvanzaTurnoDaComoResultadoUnNuevoTemporizadorDeTreintaSegundos()
        throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        String codigo = partida.getCodigoUnico();

        // Cuando
        servicioPartida.avanzarTurno(codigo);

        // Entonces
        verify(gestorTemporizadorMock).programarTemporizador(
            eq(codigo), eq(30), any(Runnable.class)
        );
    }


    @Test
    public void dadoQueElNotificadorNoEstaConfiguradoCuandoSeAvanzaTurnoDaComoResultadoElSiguienteJugador()
        throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
        // Dado
        Partida partida = dadoQueExisteUnaPartidaConJugadores(2);
        servicioPartida = new ServicioPartidaImpl(
            repositorioPartidaMock, repositorioSalaDeEsperaMock,
            servicioDadoMock, gestorTemporizadorMock, null
        );

        // Cuando
        servicioPartida.avanzarTurno(partida.getCodigoUnico());

        // Entonces
        assertEquals(1, partida.getIndiceTurnoActual());
    }

    private Partida dadoQueExisteUnaPartidaConJugadores(int cantidad) {
        Partida partida = new Partida();
        partida.setCodigoUnico("MATANZA123");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setIndiceTurnoActual(0);
        partida.setRondaActual(1);
        partida.setTurnosFinalizadosEnRonda(0);
        partida.setJugadores(new ArrayList<>());

        for (int indice = 0; indice < cantidad; indice++) {
            Jugador jugador = new Jugador();
            jugador.setId((long) indice + 1);
            jugador.setOrden(indice);
            jugador.setEnBancarrota(false);
            jugador.setPierdeTurno(false);
            partida.getJugadores().add(jugador);
        }

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(partida.getCodigoUnico()))
            .thenReturn(partida);
        return partida;
    }
}
