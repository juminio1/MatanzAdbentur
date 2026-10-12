package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ServicioPartida servicioPartida;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        servicioPartida = new ServicioPartidaImpl(
                repositorioPartidaMock,
                repositorioSalaDeEsperaMock
        );
    }

    @Test
    public void queSeObtenganLasPosicionesYSeMuestrenTodosLosJugadoresPorCodigo() {
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
    public void dadoQueEsTurnoUnoAlObtenerJugadorActualDeberiaRetornarJugadorDos() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);

        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(1);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPrueba);

        assertNotNull(jugadorActual);
        assertEquals(1, jugadorActual.getOrden());
    }

    @Test
    public void alAvanzarTurnoDeberiaSumarUnoAlIndice() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));
        partidaSimulada.setIndiceTurnoActual(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(1, partidaSimulada.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueEsElUltimoJugadorAlAvanzarTurnoDeberiaReiniciarAlPrimerJugador() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();
        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);
        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(2);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(0, partidaSimulada.getIndiceTurnoActual());
    }

    @Test
    public void dadoQueElSiguientePierdeTurnoAlAvanzarTurnoDeberiaSaltarloYQuitarleLaPenalizacion() {
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setOrden(0);

        Jugador jugador2 = new Jugador();
        jugador2.setOrden(1);
        jugador2.setPierdeTurno(true);

        Jugador jugador3 = new Jugador();
        jugador3.setOrden(2);

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2, jugador3));
        partidaSimulada.setIndiceTurnoActual(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(partidaSimulada);

        servicioPartida.avanzarTurno(codigoPrueba);

        assertEquals(2, partidaSimulada.getIndiceTurnoActual());
        assertFalse(jugador2.getPierdeTurno());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroIniciarlaExitosamente()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {

        
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
            assertEquals(150000, jugador.getDinero());
            assertEquals(0, jugador.getPosicionActual());
        }
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaSinJugadores() {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList());

        Usuario creador = new Usuario();
        creador.setId(1L);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        
        sala.setCreador(creador);
        
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L));
                
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueNoExisteUnaPartidaPeroExisteUnaSalaConCuatroUsuariosQuieroCrearLaPartidaExitosamente()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {

        Usuario usuarioUno = new Usuario();
        usuarioUno.setUsername("usuario1");
        usuarioUno.setId(1L);

        Usuario usuarioDos = new Usuario();
        usuarioDos.setUsername("usuario2");

        Usuario usuarioTres = new Usuario();
        usuarioTres.setUsername("usuario3");

        Usuario usuarioCuatro = new Usuario();
        usuarioCuatro.setUsername("usuario4");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuarioUno); // Acá ya estaba asignado el creador
        sala.setUsuarios(Arrays.asList(
                usuarioUno, usuarioDos, usuarioTres, usuarioCuatro
        ));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico()))
                .thenReturn(sala);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(null);

        
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
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico(), 1L);

        assertEquals(4, partida.getJugadores().size());
        Jugador jugadorSeleccionado = partida.getJugadorTurnoAleatorio();
        assertNotNull(jugadorSeleccionado);
        assertTrue(partida.getJugadores().contains(jugadorSeleccionado));
    }
}