package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.RegistroDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioRegistroImplTest {

  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioRegistro servicioRegistro;

  private static final String USERNAME_VALIDO = "ElMatancero";
  private static final String EMAIL_VALIDO = "juli@gmail.com";
  private static final String PASSWORD_VALIDA = "Matanza1!";

  @BeforeEach
  public void init() {
    repositorioUsuarioMock = mock(RepositorioUsuario.class);
    servicioRegistro = new ServicioRegistroImpl(repositorioUsuarioMock);
  }

  @Test
  public void deberiaRegistrarUnUsuarioConDatosValidos() throws Exception {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);
    when(repositorioUsuarioMock.buscarUsuarioPorUsername(USERNAME_VALIDO)).thenReturn(null);

    servicioRegistro.registrar(registro);

    verify(repositorioUsuarioMock, times(1)).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaGuardarLosDatosDelUsuarioCorrectamente() throws Exception {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);
    when(repositorioUsuarioMock.buscarUsuarioPorUsername(USERNAME_VALIDO)).thenReturn(null);

    servicioRegistro.registrar(registro);

    ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(repositorioUsuarioMock).guardar(usuarioCaptor.capture());

    Usuario usuarioGuardado = usuarioCaptor.getValue();
    assertThat(usuarioGuardado.getEmail(), equalTo(EMAIL_VALIDO));
    assertThat(usuarioGuardado.getUsername(), equalTo(USERNAME_VALIDO));
  }

  @Test
  public void deberiaGuardarLaContraseniaHasheadaYNoEnTextoPlano() throws Exception {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);
    when(repositorioUsuarioMock.buscarUsuarioPorUsername(USERNAME_VALIDO)).thenReturn(null);

    servicioRegistro.registrar(registro);

    ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(repositorioUsuarioMock).guardar(usuarioCaptor.capture());

    Usuario usuarioGuardado = usuarioCaptor.getValue();
    assertThat(usuarioGuardado.getPassword(), notNullValue());
    assertThat(usuarioGuardado.getPassword(), not(equalTo(PASSWORD_VALIDA)));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElEmailYaExiste() {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(new Usuario());

    assertThrows(UsuarioExistenteException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElUsernameYaExiste() {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);
    when(repositorioUsuarioMock.buscarUsuarioPorUsername(USERNAME_VALIDO))
      .thenReturn(new Usuario());

    assertThrows(UsuarioExistenteException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLasContraseniasNoCoinciden() {
    RegistroDTO registro = crearRegistroValido();
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("OtraPasswordDistinta!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  private RegistroDTO crearRegistroValido() {
    RegistroDTO registro = new RegistroDTO();
    registro.setUsername(USERNAME_VALIDO);
    registro.setEmail(EMAIL_VALIDO);
    registro.setPassword(PASSWORD_VALIDA);
    registro.setPasswordRepetido(PASSWORD_VALIDA);
    return registro;
  }
}
