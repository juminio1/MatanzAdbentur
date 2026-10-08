
package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.excepcion.UsuarioYaEstaEnPartidaException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEsperaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;

public class ServicioSalaDeEsperaTest {

    private ServicioSalaDeEspera servicioSalaDeEspera;
    private RepositorioUsuario repositorioUsuarioMock;
    private RepositorioPartida repositorioPartidaMock;
    private NotificadorSala notificadorSalaMock;

    @BeforeEach
    public void init() {

        this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
        this.repositorioPartidaMock = mock(RepositorioPartida.class);
        this.notificadorSalaMock = mock(NotificadorSala.class);

        this.servicioSalaDeEspera = new ServicioSalaDeEsperaImpl(
                repositorioUsuarioMock,
                repositorioPartidaMock,
                notificadorSalaMock
        );
    }

    private Usuario crearUsuario(Long id, String username) {

        Usuario usuario = new Usuario();

        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setEmail(username + "@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        return usuario;
    }

    private Partida crearPartidaEnEspera(Usuario creador) {

        Partida partida = new Partida();

        partida.setCodigoUnico("ABC123");
        partida.setCreador(creador);
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.agregarUsuario(creador);

        return partida;
    }

    private SalaDeEspera crearSalaDeEspera(Usuario creador) {

        SalaDeEspera sala = new SalaDeEspera();

        sala.setCodigoGenerado("ABC123");
        sala.setUsuarios(new ArrayList<>(List.of(creador)));

        return sala;
    }

    @Test
    public void crearPartidaConUsuarioExistente() {

        Usuario usuario = crearUsuario(1L, "jugador1");

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        Partida partida = servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(partida);
        assertTrue(partida.getUsuarios().contains(usuario));
        assertEquals(usuario, partida.getCreador());
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertNotNull(partida.getCodigoUnico());
        assertNotNull(partida.getTiempoInicio());

        verify(repositorioPartidaMock, times(1))
                .guardarPartida(partida);
    }

    @Test
    public void noCrearPartidaSiUsuarioNoExiste() {

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(null);

        assertThrows(
                UsuarioNoEncontradoException.class,
                () -> servicioSalaDeEspera.crearSalaDeEspera(1L)
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any(Partida.class));
    }

    @Test
    public void verificaQueElCodigoUnicoDePartidaSeaUnico() {

        Usuario usuario = crearUsuario(1L, "jugador1");

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico(anyString()))
                .thenReturn(new Partida())
                .thenReturn(null);

        Partida partida = servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(partida);
        assertEquals(usuario, partida.getCreador());
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertNotNull(partida.getCodigoUnico());

        verify(repositorioPartidaMock, times(2))
                .buscarPartidaActivaPorCodigoUnico(anyString());

        verify(repositorioPartidaMock, times(1))
                .guardarPartida(partida);
    }

    @Test
    public void usuarioSeUneASalaDeEsperaExistente() throws UsuarioNoEncontradoException, PartidaNoEncontradaException, SalaDeEsperaLlenaException, PartidaIniciadaException, UsuarioYaEstaEnPartidaException {

        Usuario creador = crearUsuario(1L, "jugador1");
        Usuario usuario2 = crearUsuario(2L, "jugador2");

        Partida partida = crearPartidaEnEspera(creador);
        SalaDeEspera sala = crearSalaDeEspera(creador);

        when(repositorioUsuarioMock.buscarUsuarioPorId(2L))
                .thenReturn(usuario2);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico("ABC123"))
                .thenReturn(partida);

        Partida resultado = servicioSalaDeEspera
                .unirseASalaDeEspera(2L, "ABC123", sala);

        assertNotNull(resultado);
        assertTrue(resultado.getUsuarios().contains(usuario2));
        assertEquals(2, resultado.getUsuarios().size());

        verify(repositorioPartidaMock, times(1))
                .guardarPartida(resultado);
    }

    @Test
    public void usuarioInexistenteIntentaUnirseASalaDeEspera() {

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoGenerado("ABC123");

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(null);

        assertThrows(
                UsuarioNoEncontradoException.class,
                () -> servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123", sala
                )
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any());

        verify(notificadorSalaMock, never())
                .notificarSalaActualizada(any(SalaActualizadaDTO.class));
    }

    @Test
    public void usuarioSeUneASalaDeEsperaInexistente() {

        Usuario usuario = crearUsuario(1L, "jugador1");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoGenerado("ABC123");

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico("ABC123"))
                .thenReturn(null);

        assertThrows(
                PartidaNoEncontradaException.class,
                () -> servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123", sala
                )
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any());

        verify(notificadorSalaMock, never())
                .notificarSalaActualizada(any(SalaActualizadaDTO.class));
    }

    @Test
    public void usuarioSeUneASalaDeEsperaLlena() {

        Usuario usuario1 = crearUsuario(1L, "jugador1");
        Usuario usuario2 = crearUsuario(2L, "jugador2");
        Usuario usuario3 = crearUsuario(3L, "jugador3");
        Usuario usuario4 = crearUsuario(4L, "jugador4");
        Usuario usuario5 = crearUsuario(5L, "jugador5");

        Partida partida = crearPartidaEnEspera(usuario1);
        SalaDeEspera sala = crearSalaDeEspera(usuario1);

        partida.agregarUsuario(usuario2);
        partida.agregarUsuario(usuario3);
        partida.agregarUsuario(usuario4);

        sala.getUsuarios().add(usuario2);
        sala.getUsuarios().add(usuario3);
        sala.getUsuarios().add(usuario4);

        when(repositorioUsuarioMock.buscarUsuarioPorId(5L))
                .thenReturn(usuario5);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico("ABC123"))
                .thenReturn(partida);

        assertThrows(
                SalaDeEsperaLlenaException.class,
                () -> servicioSalaDeEspera.unirseASalaDeEspera(
                        5L, "ABC123", sala
                )
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any());

        verify(notificadorSalaMock, never())
                .notificarSalaActualizada(any(SalaActualizadaDTO.class));
    }

    @Test
    public void usuarioIntentaEntrarAPartidaIniciada() {

        Usuario creador = crearUsuario(1L, "jugador1");
        Usuario usuario2 = crearUsuario(2L, "jugador2");

        Partida partida = crearPartidaEnEspera(creador);
        SalaDeEspera sala = crearSalaDeEspera(creador);

        partida.setEstado(EstadoPartida.EN_CURSO);

        when(repositorioUsuarioMock.buscarUsuarioPorId(2L))
                .thenReturn(usuario2);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico("ABC123"))
                .thenReturn(partida);

        assertThrows(
                PartidaIniciadaException.class,
                () -> servicioSalaDeEspera.unirseASalaDeEspera(
                        2L, "ABC123", sala
                )
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any());

        verify(notificadorSalaMock, never())
                .notificarSalaActualizada(any(SalaActualizadaDTO.class));
    }

    @Test
    public void verificaQueUsuarioNoSeUnaDosVecesASalaDeEspera() {

        Usuario usuario = crearUsuario(1L, "jugador1");

        Partida partida = crearPartidaEnEspera(usuario);
        SalaDeEspera sala = crearSalaDeEspera(usuario);

        when(repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(repositorioPartidaMock
                .buscarPartidaActivaPorCodigoUnico("ABC123"))
                .thenReturn(partida);

        assertThrows(
                UsuarioYaEstaEnPartidaException.class,
                () -> servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123", sala
                )
        );

        verify(repositorioPartidaMock, never())
                .guardarPartida(any());

        verify(notificadorSalaMock, never())
                .notificarSalaActualizada(any(SalaActualizadaDTO.class));
    }

    @Test
    public void obtenerEstadoSalaDevuelveLosUsuariosDeLaSala() {

        Usuario usuario1 = crearUsuario(1L, "jugador1");
        Usuario usuario2 = crearUsuario(2L, "jugador2");

        SalaDeEspera sala = crearSalaDeEspera(usuario1);
        sala.getUsuarios().add(usuario2);

        SalaActualizadaDTO resultado =
                servicioSalaDeEspera.obtenerEstadoSala(sala);

        assertNotNull(resultado);
        assertEquals("ABC123", resultado.getCodigo());
        assertEquals(2, resultado.getUsernames().size());
        assertTrue(resultado.getUsernames().contains("jugador1"));
        assertTrue(resultado.getUsernames().contains("jugador2"));
    }

    @Test
    public void obtenerEstadoSalaSinUsuariosDevuelveListaVacia() {

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoGenerado("ABC123");

        SalaActualizadaDTO resultado =
                servicioSalaDeEspera.obtenerEstadoSala(sala);

        assertNotNull(resultado);
        assertEquals("ABC123", resultado.getCodigo());
        assertNotNull(resultado.getUsernames());
        assertTrue(resultado.getUsernames().isEmpty());
    }

    @Test
    public void obtenerEstadoSalaConUsuarioNuloLanzaExcepcion() {

        SalaDeEspera sala = new SalaDeEspera();

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(null);

        sala.setCodigoGenerado("ABC123");
        sala.setUsuarios(usuarios);

        assertThrows(
                UsuarioNoEncontradoException.class,
                () -> servicioSalaDeEspera.obtenerEstadoSala(sala)
        );
    }

    @Test
    public void alCrearUnaSalaDeEsperaLaListaDeUsuariosDebeEstarInicializada() {

        SalaDeEspera salaDeEspera = new SalaDeEspera();

        assertNotNull(salaDeEspera.getUsuarios());
        assertTrue(salaDeEspera.getUsuarios().isEmpty());
    }

    @Test
    public void sePuedeAsignarYObtenerUnaListaDeUsuarios() {

        SalaDeEspera salaDeEspera = new SalaDeEspera();

        List<Usuario> usuarios = new ArrayList<>();

        salaDeEspera.setUsuarios(usuarios);

        assertEquals(usuarios, salaDeEspera.getUsuarios());
    }

    @Test
    public void sePuedeAsignarYObtenerElCodigoGenerado() {

        SalaDeEspera salaDeEspera = new SalaDeEspera();

        salaDeEspera.setCodigoGenerado("ABC123");

        assertEquals("ABC123", salaDeEspera.getCodigoGenerado());
    }
}
