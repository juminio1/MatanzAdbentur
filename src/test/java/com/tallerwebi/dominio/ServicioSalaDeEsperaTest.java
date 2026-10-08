package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
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
    public void usuarioSeleccionaFichaCorrectamente()
        throws UsuarioNoEncontradoException, FichaOcupadaException, PartidaNoEncontradaException {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setCreador(usuario);
        partida.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario.getId(),
            Ficha.AMARILLA
        );

        assertEquals(Ficha.AMARILLA, partida.getFichasSeleccionadas().get(usuario));
        verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void usuarioNoPuedeSeleccionarFichaOcupada()
        throws UsuarioNoEncontradoException, FichaOcupadaException, PartidaNoEncontradaException {
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setEmail("jugador1@test.com");
        usuario1.setPassword("123");
        usuario1.setRol("USER");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");
        usuario2.setPassword("123");
        usuario2.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setCreador(usuario1);
        partida.getUsuarios().add(usuario1);
        partida.getUsuarios().add(usuario2);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario1);
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(usuario2);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario1.getId(),
            Ficha.AMARILLA
        );

        assertThrows(FichaOcupadaException.class, () -> {
            this.servicioSalaDeEspera.seleccionarFicha(
                partida.getCodigoUnico(),
                usuario2.getId(),
                Ficha.AMARILLA
            );
        });

        assertEquals(Ficha.AMARILLA, partida.getFichasSeleccionadas().get(usuario1));
        verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void usuarioSleccionaFichaLaLiberaYOtroUsuarioLaSelecciona()
        throws UsuarioNoEncontradoException, FichaOcupadaException, PartidaNoEncontradaException {
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setEmail("jugador1@test.com");
        usuario1.setPassword("123");
        usuario1.setRol("USER");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");
        usuario2.setPassword("123");
        usuario2.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setCreador(usuario1);
        partida.getUsuarios().add(usuario1);
        partida.getUsuarios().add(usuario2);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario1);
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(usuario2);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario1.getId(),
            Ficha.AMARILLA
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario1.getId(),
            Ficha.AZUL
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario2.getId(),
            Ficha.AMARILLA
        );

        assertEquals(Ficha.AZUL, partida.getFichasSeleccionadas().get(usuario1));
        assertEquals(Ficha.AMARILLA, partida.getFichasSeleccionadas().get(usuario2));
        verify(this.repositorioPartidaMock, times(3)).guardarPartida(partida);
    }

    @Test
    public void usuarioNoPuedeSeleccionarFichaSiNoEstaEnPartida()
        throws UsuarioNoEncontradoException, FichaOcupadaException, PartidaNoEncontradaException {
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setEmail("jugador1@test.com");
        usuario1.setPassword("123");
        usuario1.setRol("USER");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");
        usuario2.setPassword("123");
        usuario2.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setCreador(usuario1);
        partida.getUsuarios().add(usuario1);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario1);
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(usuario2);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        this.servicioSalaDeEspera.seleccionarFicha(
            partida.getCodigoUnico(),
            usuario1.getId(),
            Ficha.AMARILLA
        );

        assertThrows(UsuarioNoEncontradoException.class, () -> {
            this.servicioSalaDeEspera.seleccionarFicha(
                partida.getCodigoUnico(),
                usuario2.getId(),
                Ficha.AZUL
            );
        });
        assertEquals(Ficha.AMARILLA, partida.getFichasSeleccionadas().get(usuario1));
        assertNull(partida.getFichasSeleccionadas().get(usuario2));
        verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
    }

    @Test
    public void usuarioAbandonaLaSalaYLiberaSuFicha()
        throws UsuarioNoEncontradoException, FichaOcupadaException, PartidaNoEncontradaException {
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setEmail("jugador1@test.com");
        usuario1.setPassword("123");
        usuario1.setRol("USER");

        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.getUsuarios().add(usuario1);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario1);
        when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123")).thenReturn(
            partida
        );

        // Selecciona ficha
        this.servicioSalaDeEspera.seleccionarFicha("ABC123", 1L, Ficha.ROJA);
        assertEquals(Ficha.ROJA, partida.getFichasSeleccionadas().get(usuario1));

        // Abandona la sala
        this.servicioSalaDeEspera.abandonarSala("ABC123", 1L);

        assertFalse(partida.getUsuarios().contains(usuario1));
        assertNull(partida.getFichasSeleccionadas().get(usuario1));
        verify(this.repositorioPartidaMock, times(2)).guardarPartida(partida);
    }
}
