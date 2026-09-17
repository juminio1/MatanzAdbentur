package com.tallerwebi.dominio;

import com.tallerwebi.infraestructura.RepositorioPartida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ServicioPartidaImplTest {

    @Test
    public void crearPartidaConUsuarioExistente() {

        RepositorioUsuario repositorioUsuario = mock(RepositorioUsuario.class);
        RepositorioPartida repositorioPartida = mock(RepositorioPartida.class);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        when(repositorioUsuario.buscarUsuarioPorId(1))
                .thenReturn(usuario);

        ServicioPartidaImpl servicio = new ServicioPartidaImpl(
                repositorioUsuario,
                repositorioPartida
        );

        Partida partida = servicio.crearPartida(1);

        assertNotNull(partida);

        verify(repositorioPartida).guardarPartida(partida);

        assertTrue(partida.getUsuarios().contains(usuario));

        assertNotNull(partida.getTablero());

        assertNotNull(partida.getTiempoInicio());
    }

    @Test
    public void noCrearPartidaSiUsuarioNoExiste() {

        RepositorioUsuario repositorioUsuario = mock(RepositorioUsuario.class);
        RepositorioPartida repositorioPartida = mock(RepositorioPartida.class);

        when(repositorioUsuario.buscarUsuarioPorId(1))
                .thenReturn(null);

        ServicioPartidaImpl servicio = new ServicioPartidaImpl(
                repositorioUsuario,
                repositorioPartida
        );

        Partida partida = servicio.crearPartida(1);

        assertNull(partida);

        verify(repositorioPartida, never()).guardarPartida(any(Partida.class));
    }

}
