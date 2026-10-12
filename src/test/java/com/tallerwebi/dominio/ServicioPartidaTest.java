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
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioDado;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.enums.ColorFicha;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ServicioPartida servicioPartida;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;
    private ServicioDado servicioDadoMock;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        servicioPartida = new ServicioPartidaImpl(
                repositorioPartidaMock,
                repositorioSalaDeEsperaMock,
                servicioDadoMock
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

        // devuelve la partida falsa
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
    public void dadoQueExisteUnaPartidaQuieroIniciarlaExitosamente()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {

        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        Partida partida = new Partida();
        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico()))
                .thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

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

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon2Jugadores() {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon3Jugadores() {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon5Jugadores() {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        Jugador jugadorCinco = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(
                jugadorUno, jugadorDos, jugadorTres, jugadorCuatro, jugadorCinco));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadMaximaJugadoresSuperadaException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void dadoQueExisteUnaPartidaQuieroInicializarUnaPartidaCon1Jugador() {
        Jugador jugadorUno = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(CantidadJugadoresInsuficienteException.class,
                () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
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

        Usuario usuarioDos = new Usuario();
        usuarioDos.setUsername("usuario2");

        Usuario usuarioTres = new Usuario();
        usuarioTres.setUsername("usuario3");

        Usuario usuarioCuatro = new Usuario();
        usuarioCuatro.setUsername("usuario4");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuarioUno);
        sala.setUsuarios(Arrays.asList(
                usuarioUno, usuarioDos, usuarioTres, usuarioCuatro
        ));

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico()))
                .thenReturn(sala);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(null);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

        verify(repositorioPartidaMock).guardarPartida(any(Partida.class));
    }

    @Test
    public void dadoQueExisteUnaPartidaSinJugadoresAlIniciarlaDebenAsignarseLasFichasDeLosUsuarios()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {

        Usuario usuarioUno = new Usuario();
        usuarioUno.setUsername("usuario1");
        usuarioUno.setFicha(Ficha.ROJA);

        Usuario usuarioDos = new Usuario();
        usuarioDos.setUsername("usuario2");
        usuarioDos.setFicha(Ficha.AZUL);

        Usuario usuarioTres = new Usuario();
        usuarioTres.setUsername("usuario3");
        usuarioTres.setFicha(Ficha.VERDE);

        Usuario usuarioCuatro = new Usuario();
        usuarioCuatro.setUsername("usuario4");
        usuarioCuatro.setFicha(Ficha.AMARILLA);

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setUsuarios(Arrays.asList(
                usuarioUno, usuarioDos, usuarioTres, usuarioCuatro
        ));

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);

        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico()))
                .thenReturn(sala);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

        assertEquals(4, partida.getJugadores().size());

        assertEquals(ColorFicha.ROJO, partida.getJugadores().get(0).getFicha());
        assertEquals(ColorFicha.AZUL, partida.getJugadores().get(1).getFicha());
        assertEquals(ColorFicha.VERDE, partida.getJugadores().get(2).getFicha());
        assertEquals(ColorFicha.AMARILLO, partida.getJugadores().get(3).getFicha());

        assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());

        verify(repositorioPartidaMock).guardarPartida(partida);
    }

    @Test
    public void dadoQueExisteUnaPartidaConJugadoresDebenInicializarseCon10000DeDinero()
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

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

        assertEquals(150000, jugadorCuatro.getDinero());
        assertEquals(150000, jugadorTres.getDinero());
        assertEquals(150000, jugadorDos.getDinero());
        assertEquals(150000, jugadorUno.getDinero());
    }

    @Test
    public void dadoQueExisteUnaPartidaConJugadoresDebenPosicionarseInicialmenteEn0Exitosamente()
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();
        jugadorCuatro.setPosicionActual(2);
        jugadorDos.setPosicionActual(1);
        jugadorTres.setPosicionActual(3);
        jugadorUno.setPosicionActual(4);

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

        assertEquals(0, jugadorCuatro.getPosicionActual());
        assertEquals(0, jugadorTres.getPosicionActual());
        assertEquals(0, jugadorDos.getPosicionActual());
        assertEquals(0, jugadorUno.getPosicionActual());
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

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        servicioPartida.iniciarPartida(sala.getCodigoUnico());

        Integer cantidadJugadoresObtenidos = partida.getJugadores().size();
        Integer cantidadJugadoresEsperados = 4;
        assertEquals(cantidadJugadoresEsperados, cantidadJugadoresObtenidos);
        Jugador jugadorSeleccionado = partida.getJugadorTurnoAleatorio();
        assertNotNull(jugadorSeleccionado);
        assertTrue(partida.getJugadores().contains(jugadorSeleccionado));
    }

    @Test
    public void dadoQueLaPartidaEstaEnCursoNoDebeVolverAInicializarse() {
        Jugador jugadorUno = new Jugador();
        Jugador jugadorDos = new Jugador();
        Jugador jugadorTres = new Jugador();
        Jugador jugadorCuatro = new Jugador();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(Arrays.asList(jugadorUno, jugadorDos, jugadorTres, jugadorCuatro));

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        when(repositorioSalaDeEsperaMock.buscarPorCodigo(sala.getCodigoUnico())).thenReturn(sala);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(sala.getCodigoUnico()))
                .thenReturn(partida);

        assertThrows(PartidaIniciadaException.class, () -> servicioPartida.iniciarPartida(sala.getCodigoUnico()));
        verify(repositorioPartidaMock, never()).guardarPartida(any());
    }
}
