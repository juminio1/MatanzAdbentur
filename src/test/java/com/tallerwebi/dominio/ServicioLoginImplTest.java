package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import com.tallerwebi.dominio.servicios.ServicioLogin;
import com.tallerwebi.dominio.servicios.ServicioLoginImpl;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioLoginImplTest {

  private ServicioLogin servicioLogin;
  private RepositorioUsuario repositorioUsuarioMock;

  @BeforeEach
  public void init() {
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
    this.servicioLogin = new ServicioLoginImpl(this.repositorioUsuarioMock);
  }

  @Test
  public void queSePuedaAutenticarUnUsuarioConEmailCorrecto()
      throws CredencialesInvalidasException {
    // Preparación
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    // En la BD la contraseña estaría hasheada
    usuario.setPassword(
        "2286d87dd6a4c52b3fb1d2d27078fcbcd96e07d1c8b087d904080cba700e7132"
    );

    when(this.repositorioUsuarioMock.buscarUsuarioPorCredencial("juli@gmail.com"))
        .thenReturn(usuario);

    // Ejecución
    Usuario usuarioAutenticado =
        servicioLogin.autenticar("juli@gmail.com", "Julieta123$");

    // Verificación
    assertEquals(usuario, usuarioAutenticado);
  }

  @Test
  public void queSePuedaAutenticarUnUsuarioConUsernameCorrecto()
      throws CredencialesInvalidasException {
    // Preparación
    Usuario usuario = new Usuario();
    usuario.setUsername("juli123");

    // En la BD la contraseña estaría hasheada
    usuario.setPassword(
        "2286d87dd6a4c52b3fb1d2d27078fcbcd96e07d1c8b087d904080cba700e7132"
    );

    when(this.repositorioUsuarioMock.buscarUsuarioPorCredencial("juli123"))
        .thenReturn(usuario);

    // Ejecución
    Usuario usuarioAutenticado =
        servicioLogin.autenticar("juli123", "Julieta123$");

    // Verificación
    assertEquals(usuario, usuarioAutenticado);
  }

  @Test
  public void quieroConsultarUnUsuarioPorEmail() {
    // Preparación
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail()))
        .thenReturn(usuario);

    // Ejecución
    Usuario usuarioBuscado =
        servicioLogin.consultarUsuario(usuario.getEmail());

    // Verificación
    assertThat(
        usuarioBuscado.getEmail(),
        equalTo(usuario.getEmail())
    );
  }

  @Test
  public void queNoSePuedaAutenticarUnUsuarioQueNoExiste() {
    // Preparación
    when(this.repositorioUsuarioMock.buscarUsuarioPorCredencial("noexiste"))
        .thenReturn(null);

    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar("noexiste", "Julieta123$")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConUnaContraseniaIncorrecta() {
    // Preparación
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    // Hasheada de 1234
    usuario.setPassword(
        "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
    );

    when(this.repositorioUsuarioMock.buscarUsuarioPorCredencial("juli@gmail.com"))
        .thenReturn(usuario);

    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar("juli@gmail.com", "1235")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConCredencialVacia() {
    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar("", "1234")
    );

    verifyNoInteractions(repositorioUsuarioMock);
  }

  @Test
  public void queNoSePuedaAutenticarConCredencialNull() {
    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar(null, "1234")
    );

    verifyNoInteractions(repositorioUsuarioMock);
  }

  @Test
  public void queNoSePuedaAutenticarConContraseniaVacia() {
    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar("juli@gmail.com", "")
    );

    verifyNoInteractions(repositorioUsuarioMock);
  }

  @Test
  public void queNoSePuedaAutenticarConContraseniaNull() {
    // Ejecución y verificación
    assertThrows(
        CredencialesInvalidasException.class,
        () -> this.servicioLogin.autenticar("juli@gmail.com", null)
    );

    verifyNoInteractions(repositorioUsuarioMock);
  }
}