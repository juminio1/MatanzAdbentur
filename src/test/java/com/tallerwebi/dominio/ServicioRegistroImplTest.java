package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.ContraseniaInvalida;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioRegistroImplTest {

  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioRegistro servicioRegistro;

  @BeforeEach
  public void init() {
    repositorioUsuarioMock = mock(RepositorioUsuario.class);
    servicioRegistro = new ServicioRegistroImpl(repositorioUsuarioMock);
  }

  @Test
  public void deberiaRegistrarUsuarioConPasswordValidaYHasheada()
    throws UsuarioExistente, ContraseniaInvalida {
    Usuario usuario = new Usuario();
    usuario.setEmail("jugador@monopoly.com");
    String passPlano = "Matanza2026$";
    usuario.setPassword(passPlano);

    when(repositorioUsuarioMock.buscarUsuario(anyString(), anyString())).thenReturn(null);

    servicioRegistro.registrar(usuario);

    verify(repositorioUsuarioMock, times(1)).guardar(usuario);
    assertThat(usuario.getPassword(), not(equalTo(passPlano)));
  }

  @Test
  public void deberiaLanzarExcepcionSiElUsuarioYaExiste() {
    Usuario usuario = new Usuario();
    usuario.setEmail("existente@monopoly.com");
    usuario.setPassword("Matanza2026$");

    when(repositorioUsuarioMock.buscarUsuario(usuario.getEmail(), usuario.getPassword()))
      .thenReturn(usuario);

    assertThrows(UsuarioExistente.class, () -> servicioRegistro.registrar(usuario));
    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionSiLaPasswordEsCorta() {
    Usuario usuario = new Usuario();
    usuario.setEmail("jugador@monopoly.com");
    usuario.setPassword("Ab1$");

    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(usuario));
    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionSiLaPasswordNoTieneNumero() {
    Usuario usuario = new Usuario();
    usuario.setEmail("jugador@monopoly.com");
    usuario.setPassword("MatanzaEspecial$");

    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(usuario));
    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionSiLaPasswordNoTieneCaracterEspecial() {
    Usuario usuario = new Usuario();
    usuario.setEmail("jugador@monopoly.com");
    usuario.setPassword("Matanza2026");

    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(usuario));
    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionSiLaPasswordEsNula() {
    Usuario usuario = new Usuario();
    usuario.setEmail("jugador@monopoly.com");
    usuario.setPassword(null);

    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(usuario));
    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionSiElUsuarioOEmailSonNulos() {
    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(null));

    Usuario usuarioSinEmail = new Usuario();
    usuarioSinEmail.setPassword("Matanza2026$");
    assertThrows(ContraseniaInvalida.class, () -> servicioRegistro.registrar(usuarioSinEmail));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }
}
