package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
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
  public void queSePuedaAutenticarUnUsuarioConCredencialesCorrectas()
    throws CredencialesInvalidasException {
    // Preparación
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    // En la BD la contraseña estaría hasheada
    usuario.setPassword("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail("juli@gmail.com")).thenReturn(usuario);

    // Ejecución
    Usuario usuarioAutenticado = servicioLogin.autenticar("juli@gmail.com", "1234");

    // Verificación
    assertEquals(usuario, usuarioAutenticado);
  }

  @Test
  public void queNoSePuedaAutenticarUnUsuarioQueNoExiste() {
    assertThrows(
      CredencialesInvalidasException.class,
      () -> this.servicioLogin.autenticar("juli@gmail.com", "1234")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConUnaContraseniaIncorrecta() {
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    // Hash SHA-256 de "1234"
    usuario.setPassword("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail())).thenReturn(usuario);

    assertThrows(
      CredencialesInvalidasException.class,
      () -> this.servicioLogin.autenticar("juli@gmail.com", "1235")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConEmailVacio() {
    Usuario usuario = new Usuario();
    usuario.setEmail("");

    // Hash SHA-256 de "1234"
    usuario.setPassword("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail())).thenReturn(usuario);

    assertThrows(
      CredencialesInvalidasException.class,
      () -> this.servicioLogin.autenticar("", "1234")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConEmailNull() {
    Usuario usuario = new Usuario();
    usuario.setEmail(null);

    // Hash SHA-256 de "1234"
    usuario.setPassword("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail())).thenReturn(usuario);

    assertThrows(
      CredencialesInvalidasException.class,
      () -> this.servicioLogin.autenticar(null, "1234")
    );
  }

  @Test
  public void queNoSePuedaAutenticarConContraseniaNull() {
    Usuario usuario = new Usuario();
    usuario.setEmail("juli@gmail.com");

    // Hash SHA-256 de "1234"
    usuario.setPassword(null);

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail())).thenReturn(usuario);

    assertThrows(
      CredencialesInvalidasException.class,
      () -> this.servicioLogin.autenticar("juli@gmail.com", null)
    );
  }

  @Test
  public void queSePuedaConsultarUnUsuarioPorEmail() {
    Usuario usuario = new Usuario();
    String email = "juli@gmail.com";
    usuario.setEmail(email);

    when(this.repositorioUsuarioMock.buscarUsuarioPorEmail(usuario.getEmail())).thenReturn(usuario);

    assertEquals(email, usuario.getEmail());
  }
}
