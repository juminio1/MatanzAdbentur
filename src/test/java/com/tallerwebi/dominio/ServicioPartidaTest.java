package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.UsuarioNoEncontrado;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

  private ServicioPartida servicioPartida;
  private RepositorioUsuario repositorioUsuarioMock;
  private RepositorioPartida repositorioPartidaMock;

  @BeforeEach
  public void init() {
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
    this.repositorioPartidaMock = mock(RepositorioPartida.class);
    this.servicioPartida =
      new ServicioPartidaImpl(this.repositorioUsuarioMock, this.repositorioPartidaMock);
  }

  @Test
  public void crearPartidaConUsuarioExistente() {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setEmail("jugador@test.com");
    usuario.setPassword("123");
    usuario.setRol("USER");

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1)).thenReturn(usuario);

    Partida partida = this.servicioPartida.crearPartida(1);

    assertNotNull(partida);

    verify(this.repositorioPartidaMock).guardarPartida(partida);

    assertTrue(partida.getUsuarios().contains(usuario));

    assertNotNull(partida.getTablero());

    assertNotNull(partida.getTiempoInicio());
  }

  @Test
  public void noCrearPartidaSiUsuarioNoExiste() {
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1)).thenReturn(null);

    assertThrows(
      UsuarioNoEncontrado.class,
      () -> {
        this.servicioPartida.crearPartida(1);
      }
    );

    verify(this.repositorioPartidaMock, never()).guardarPartida(any(Partida.class));
  }
}
