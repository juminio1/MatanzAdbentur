package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.CantidadinsuficienteDeJugadoresException;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoCreadorException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;

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
  public void crearPartidaConUsuarioExistente() throws UsuarioNoEncontradoException {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setEmail("jugador@test.com");
    usuario.setPassword("123");
    usuario.setRol("USER");

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);

    Partida partida = this.servicioPartida.crearPartida(1L);

    assertNotNull(partida);

    verify(this.repositorioPartidaMock).guardarPartida(partida);

    assertTrue(partida.getUsuarios().contains(usuario));

    assertTrue(partida.getCreador().equals(usuario));

    assertTrue(partida.getEstado().equals(EstadoPartida.EN_ESPERA));

    assertNotNull(partida.getCodigoUnico());

    assertNotNull(partida.getTablero());

    assertNotNull(partida.getTiempoInicio());
  }

  @Test
  public void noCrearPartidaSiUsuarioNoExiste() {
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

    assertThrows(
      UsuarioNoEncontradoException.class,
      () -> {
        this.servicioPartida.crearPartida(1L);
      }
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

    Partida partida = this.servicioPartida.crearPartida(1L);

    assertNotNull(partida);
    assertEquals(usuario, partida.getCreador());
    assertEquals(EstadoPartida.EN_ESPERA, partida.getEstado());
    assertNotNull(partida.getCodigoUnico());

    verify(this.repositorioPartidaMock, times(2)).buscarPartidaActivaPorCodigoUnico(anyString());
    verify(this.repositorioPartidaMock, times(1)).guardarPartida(partida);
  }

  @Test
  public void iniciarPartidaCuandoElUsuarioEsCreadorYTieneJugadoresSuficientes() throws Exception {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Usuario jugador2 = new Usuario();
    jugador2.setId(2L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);
    partida.agregarUsuario(jugador2);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    this.servicioPartida.iniciarPartida(1L, "ABC123");

    assertEquals(EstadoPartida.EN_CURSO, partida.getEstado());
    assertNotNull(partida.getTiempoInicio());
    verify(this.repositorioPartidaMock).guardarPartida(partida);
  }

  @Test
  public void lanzarExcepcionAlIniciarPartidaConUsuarioNoCreador() {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Usuario noCreador = new Usuario();
    noCreador.setId(2L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);
    partida.agregarUsuario(noCreador);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(noCreador);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    assertThrows(
      UsuarioNoCreadorException.class,
      () -> this.servicioPartida.iniciarPartida(2L, "ABC123")
    );

    verify(this.repositorioPartidaMock, never()).guardarPartida(any(Partida.class));
  }

  @Test
  public void lanzarExcepcionAlIniciarPartidaConCantidadInsuficienteDeJugadores() {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador); // Solo 1 jugador

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    assertThrows(
      CantidadinsuficienteDeJugadoresException.class,
      () -> this.servicioPartida.iniciarPartida(1L, "ABC123")
    );

    verify(this.repositorioPartidaMock, never()).guardarPartida(any(Partida.class));
  }

  @Test
  public void lanzarExcepcionAlIniciarPartidaSiPartidaNoExiste() {
    Usuario usuario = new Usuario();
    usuario.setId(1L);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("INEXISTENTE"))
      .thenReturn(null);

    assertThrows(
      PartidaNoEncontradaException.class,
      () -> this.servicioPartida.iniciarPartida(1L, "INEXISTENTE")
    );
  }

  @Test
  public void jugadorSeleccionaUnaFichaDisponible()
    throws FichaOcupadaException, UsuarioNoEncontradoException, PartidaNoEncontradaException {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    this.servicioPartida.seleccionarFicha(1L, "ABC123", Ficha.FICHA_AMARILLA);

    assertEquals(Ficha.FICHA_AMARILLA, partida.getFichasPorUsuario().get(creador));
    verify(this.repositorioPartidaMock).guardarPartida(partida);
  }

  @Test
  public void jugadorSeleccionaUnaFichaOcupada()
    throws FichaOcupadaException, UsuarioNoEncontradoException, PartidaNoEncontradaException {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Usuario jugador2 = new Usuario();
    jugador2.setId(2L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);
    partida.agregarUsuario(jugador2);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(jugador2);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    this.servicioPartida.seleccionarFicha(1L, "ABC123", Ficha.FICHA_AMARILLA);

    assertThrows(
      FichaOcupadaException.class,
      () -> this.servicioPartida.seleccionarFicha(2L, "ABC123", Ficha.FICHA_AMARILLA)
    );
  }

  @Test
  public void jugadorLiberaUnaFicha()
    throws FichaOcupadaException, UsuarioNoEncontradoException, PartidaNoEncontradaException {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Usuario jugador2 = new Usuario();
    jugador2.setId(2L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);
    partida.agregarUsuario(jugador2);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(jugador2);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    this.servicioPartida.seleccionarFicha(1L, "ABC123", Ficha.FICHA_AMARILLA);
    this.servicioPartida.seleccionarFicha(1L, "ABC123", Ficha.FICHA_AZUL); // Libera ficha
    this.servicioPartida.seleccionarFicha(2L, "ABC123", Ficha.FICHA_AMARILLA); // Selecciona la ficha liberada

    assertEquals(Ficha.FICHA_AZUL, partida.getFichasPorUsuario().get(creador));
    assertEquals(Ficha.FICHA_AMARILLA, partida.getFichasPorUsuario().get(jugador2));
    verify(this.repositorioPartidaMock, times(3)).guardarPartida(partida);
  }

  @Test
  public void jugadorSeleccionaFichaSinPertenecerALaPartida()
    throws FichaOcupadaException, UsuarioNoEncontradoException, PartidaNoEncontradaException {
    Usuario creador = new Usuario();
    creador.setId(1L);

    Usuario jugadorColado = new Usuario();
    jugadorColado.setId(99L);

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setCreador(creador);
    partida.setEstado(EstadoPartida.EN_ESPERA);
    partida.agregarUsuario(creador);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(creador);
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(99L)).thenReturn(jugadorColado);
    when(this.repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC123"))
      .thenReturn(partida);

    assertThrows(
      UsuarioNoEncontradoException.class,
      () -> this.servicioPartida.seleccionarFicha(99L, "ABC123", Ficha.FICHA_AZUL)
    );

    verify(this.repositorioPartidaMock, never()).guardarPartida(partida);
  }
}
