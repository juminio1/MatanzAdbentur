package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.ServicioRegistro;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorLoginTest {

  private ControladorLogin controladorLogin;
  private Usuario usuarioMock;
  private LoginDTO datosLoginMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private ServicioLogin servicioLoginMock;
  private ControladorRegistro controladorRegistro;
  private ServicioRegistro servicioRegistroMock;

  @BeforeEach
  public void init() {
    datosLoginMock = new LoginDTO("dami@unlam.com", "123");
    usuarioMock = mock(Usuario.class);
    when(usuarioMock.getEmail()).thenReturn("dami@unlam.com");
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    servicioLoginMock = mock(ServicioLogin.class);
    controladorLogin = new ControladorLogin(servicioLoginMock);
    controladorRegistro = new ControladorRegistro(servicioRegistroMock);
  }

  @Test
  public void loginConUsuarioYPasswordIncorrectosDeberiaLlevarALoginNuevamente()
    throws CredencialesInvalidasException {
    // preparacion
    when(servicioLoginMock.autenticar(datosLoginMock.getEmail(), datosLoginMock.getPassword()))
      .thenThrow(new CredencialesInvalidasException("Usuario o clave incorrecta"));

    // ejecucion
    ModelAndView modelAndView = controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("login"));

    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Usuario o clave incorrecta")
    );
  }

  @Test
  public void loginConUsuarioYPasswordCorrectosDeberiaLLevarAHome()
    throws CredencialesInvalidasException {
    // preparacion
    Usuario usuarioEncontradoMock = mock(Usuario.class);

    when(usuarioEncontradoMock.getRol()).thenReturn("ADMIN");
    when(usuarioEncontradoMock.getUsername()).thenReturn("Dami");

    when(requestMock.getSession()).thenReturn(sessionMock);

    when(servicioLoginMock.autenticar(datosLoginMock.getEmail(), datosLoginMock.getPassword()))
      .thenReturn(usuarioEncontradoMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/home"));

    verify(sessionMock, times(1)).setAttribute("ROL", "ADMIN");

    verify(sessionMock, times(1)).setAttribute("NOMBRE", "Dami");
  }

  @Test
  public void irALoginDeberiaRetornarVistaLoginConDatosLogin() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irALogin();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("login"));
    assertThat(modelAndView.getModel().get("datosLogin"), instanceOf(LoginDTO.class));
  }

  @Test
  public void irAHomeDeberiaRetornarVistaHome() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));
  }

  @Test
  public void inicioDeberiaRedirigirALogin() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.inicio();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/login"));
  }

  @Test
  public void irAHomeConUsuarioLogueadoDeberiaMostrarSuNombre() {
    // preparacion
    when(requestMock.getSession()).thenReturn(sessionMock);
    when(sessionMock.getAttribute("NOMBRE")).thenReturn("Dami");

    controladorLogin = new ControladorLogin(servicioLoginMock, requestMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));

    assertThat(
      modelAndView.getModel().get("nombreJugador").toString(),
      equalToIgnoringCase("Dami")
    );
  }

  @Test
  public void irAHomeSinUsuarioLogueadoDeberiaMostrarNombrePorDefecto() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));

    assertThat(
      modelAndView.getModel().get("nombreJugador").toString(),
      equalToIgnoringCase("Vecino de La Matanza")
    );
  }
}
