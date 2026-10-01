package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
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
    this.servicioSalaDeEspera =
      new ServicioSalaDeEsperaImpl(this.repositorioUsuarioMock, this.repositorioPartidaMock);
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

    assertThrows(
      UsuarioNoEncontradoException.class,
      () -> this.servicioSalaDeEspera.crearSalaDeEspera(1L)
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

    verify(this.repositorioPartidaMock, times(2)).buscarPartidaActivaPorCodigoUnico(anyString());
    verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
  }
}