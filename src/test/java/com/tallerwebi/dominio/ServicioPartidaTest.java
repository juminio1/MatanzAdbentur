package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ServicioPartida servicioPartida;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        servicioPartida = new ServicioPartidaImpl(repositorioPartidaMock);
    }

    @Test
    public void queSeObtenganLasPosicionesYSeMuestrenTodosLosJugadoresPorCodigo() {
        // partida falsa
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Jugador jugador1 = new Jugador();
        jugador1.setPosicionActual(0); // Casillero inicial

        Jugador jugador2 = new Jugador();
        jugador2.setPosicionActual(7); // Casillero avanzado

        partidaSimulada.setJugadores(Arrays.asList(jugador1, jugador2));

        // devuelve la partida falsa
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        Partida partidaObtenida = servicioPartida.obtenerPartida(codigoPrueba);

        // verifica que trae a los jugadores y sus posiciones exactas
        assertNotNull(partidaObtenida);
        assertEquals(2, partidaObtenida.getJugadores().size());
        assertEquals(0, partidaObtenida.getJugadores().get(0).getPosicionActual());
        assertEquals(7, partidaObtenida.getJugadores().get(1).getPosicionActual());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroIniciarlaExitosamente()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida); //when(LLAMADA_AL_MOCK).thenReturn(VALOR);

        // this.repositorioPartidaMock.guardarPartida(partida); Es un mock y conceptualmente no esta guardando un pingo

        this.servicioPartida.iniciarPartida();
        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaSinJugadores()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList());

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class, () ->
            this.servicioPartida.iniciarPartida()
        );
        EstadoPartida estadoEsperado = EstadoPartida.EN_ESPERA;
        EstadoPartida estadoObtenido = partida.getEstado();

        assertEquals(estadoEsperado, estadoObtenido);
        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon2Jugadores()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class, () ->
            this.servicioPartida.iniciarPartida()
        );

        EstadoPartida estadoEsperado = EstadoPartida.EN_ESPERA;
        EstadoPartida estadoObtenido = partida.getEstado();

        assertEquals(estadoEsperado, estadoObtenido);
        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon3Jugadores()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class, () ->
            this.servicioPartida.iniciarPartida()
        );
        EstadoPartida estadoEsperado = EstadoPartida.EN_ESPERA;
        EstadoPartida estadoObtenido = partida.getEstado();

        assertEquals(estadoEsperado, estadoObtenido);
        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon5Jugadores()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        Jugador jugadorCinco = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(
            Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro, jugadorCinco)
        );

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        assertThrows(CantidadMaximaJugadoresSuperadaException.class, () ->
            this.servicioPartida.iniciarPartida()
        );
        EstadoPartida estadoEsperado = EstadoPartida.EN_ESPERA;
        EstadoPartida estadoObtenido = partida.getEstado();

        assertEquals(estadoEsperado, estadoObtenido);
        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon1Jugador()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList(jugadorUno));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class, () ->
            this.servicioPartida.iniciarPartida()
        );
        EstadoPartida estadoEsperado = EstadoPartida.EN_ESPERA;
        EstadoPartida estadoObtenido = partida.getEstado();

        assertEquals(estadoEsperado, estadoObtenido);
        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueNoExisteUnaPartidaCuandoQuieroIniciarlaLanzaPartidaNoEncontradaException() {
        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(null);

        assertThrows(PartidaNoEncontradaException.class, () ->
            this.servicioPartida.iniciarPartida()
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any()); // si la partida no existe, nunca se guarda
    }

    @Test
    public void dadoQueExisteUnaPartidaConJugadoresDebenInicializarseCon10000DeDinero()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);

        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        this.servicioPartida.iniciarPartida();

        assertEquals(jugadorCuatro.getDinero(), 150000);
        assertEquals(jugadorTres.getDinero(), 150000);
        assertEquals(jugadorDos.getDinero(), 150000);
        assertEquals(jugadorUno.getDinero(), 150000);
    }



    @Test 
    public void dadoQueExisteUnaPartidaConJugadoresDebenPosicionarseInicialmenteEn0Exitosamente() throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException{
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        jugadorCuatro.setPosicionActual(2);
        jugadorDos.setPosicionActual(1);
        jugadorTres.setPosicionActual(3);
        jugadorUno.setPosicionActual(4);

        Partida partida = new Partida();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        this.servicioPartida.iniciarPartida();

        assertEquals(jugadorCuatro.getPosicionActual(), 0);
        assertEquals(jugadorTres.getPosicionActual(), 0);
        assertEquals(jugadorDos.getPosicionActual(), 0);
        assertEquals(jugadorUno.getPosicionActual(), 0);

    }

    @Test 
    public void dadoQueExisteUnaPartidaConJugadoresSeSeleccionaAleatoriamenteElTurnoInicialExitosamente() throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException{
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        Partida partida = new Partida();
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);
        this.servicioPartida.iniciarPartida();

        Integer cantidadJugadoresObtenidos = partida.getJugadores().size();
        Integer cantidadJugadoresEsperados = 4;
        assertEquals(cantidadJugadoresObtenidos, cantidadJugadoresEsperados);

        Jugador jugadorSeleccionado = partida.getJugadorTurnoAleatorio();

        assertNotNull(jugadorSeleccionado);
        assertTrue(partida.getJugadores().contains(jugadorSeleccionado));


    }

    @Test
    public void dadoQueLaPartidaEstaEnCursoNoDebeVolverAInicializarse() throws EstadoPartidaInvalidoException, PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);

        when(this.repositorioPartidaMock.buscarPartidaActiva()).thenReturn(partida);

        this.servicioPartida.iniciarPartida();

        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }
}
