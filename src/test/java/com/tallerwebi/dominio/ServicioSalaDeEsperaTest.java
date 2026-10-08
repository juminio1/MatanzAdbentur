package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioSalaDeEsperaTest {

    private ServicioSalaDeEspera servicioSalaDeEspera;
    private RepositorioUsuario repositorioUsuarioMock;
    private RepositorioPartida repositorioPartidaMock;

    @BeforeEach
    public void init() {
        this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
        this.repositorioPartidaMock = mock(RepositorioPartida.class);
        this.servicioSalaDeEspera = new ServicioSalaDeEsperaImpl(
            this.repositorioUsuarioMock,
            this.repositorioPartidaMock
        );
    }

    @Test
    public void crearPartidaConUsuarioExistente() throws UsuarioNoEncontradoException {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);

        Partida partida = this.servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(partida);
        verify(this.repositorioPartidaMock).guardarPartida(partida);
        assertTrue(partida.getUsuarios().contains(usuario));
        assertEquals(usuario, partida.getCreador());
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertNotNull(partida.getCodigoUnico());
        assertNotNull(partida.getTiempoInicio());
    }

    @Test
    public void noCrearPartidaSiUsuarioNoExiste() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.crearSalaDeEspera(1L)
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any(Partida.class));
    }

    @Test
    public void verificaQueElCodigoUnicoDePartidaSeaUnico() throws UsuarioNoEncontradoException {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(anyString()))
            .thenReturn(new Partida())
            .thenReturn(null);

        Partida partida = this.servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(partida);
        assertEquals(usuario, partida.getCreador());
        assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
        assertNotNull(partida.getCodigoUnico());

        verify(this.repositorioPartidaMock, times(2)).buscarPartidaActivaPorCodigoUnico(
            anyString()
        );
        verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void usuarioSeUneASalaDeEsperaExistente()
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException, SalaDeEsperaLlenaException, PartidaIniciadaException, UsuarioYaEstaEnPartidaException {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");
        usuario2.setPassword("123");
        usuario2.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setCreador(usuario);
        partida.setEstado(EstadoPartida.EN_ESPERA);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(usuario2);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        partida = this.servicioSalaDeEspera.unirseASalaDeEspera(2L, "ABC123");

        assertNotNull(partida);
        assertTrue(partida.getUsuarios().contains(usuario2));
        verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void usuarioInexistenteIntentaUnirseASalaDeEspera() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void usuarioSeUneASalaDeEsperaInexistente() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            null
        );

        assertThrows(PartidaNoEncontradaException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void usuarioSeUneASalaDeEsperaLlena() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");
        usuario2.setPassword("123");
        usuario2.setRol("USER");

        Usuario usuario3 = new Usuario();
        usuario3.setId(3L);
        usuario3.setEmail("jugador3@test.com");
        usuario3.setPassword("123");
        usuario3.setRol("USER");

        Usuario usuario4 = new Usuario();
        usuario4.setId(4L);
        usuario4.setEmail("jugador4@test.com");
        usuario4.setPassword("123");
        usuario4.setRol("USER");

        Usuario usuario5 = new Usuario();
        usuario5.setId(5L);
        usuario5.setEmail("jugador5@test.com");
        usuario5.setPassword("123");
        usuario5.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setCreador(usuario);
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.agregarUsuario(usuario);
        partida.agregarUsuario(usuario2);
        partida.agregarUsuario(usuario3);
        partida.agregarUsuario(usuario4);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(5L)).thenReturn(usuario5);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        assertThrows(SalaDeEsperaLlenaException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(5L, "ABC123")
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }

    @Test
    public void usuarioIntentaEntrarAPartidaIniciada() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setCreador(usuario);
        partida.setEstado(EstadoPartida.EN_CURSO);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        assertThrows(PartidaIniciadaException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    }
    
    @Test
    public void verificaQueUsuarioNoSeUnaDosVecesASalaDeEspera() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setCreador(usuario);
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.agregarUsuario(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

       assertThrows(UsuarioYaEstaEnPartidaException.class, () ->
        this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioPartidaMock, never()).guardarPartida(any());
    

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
