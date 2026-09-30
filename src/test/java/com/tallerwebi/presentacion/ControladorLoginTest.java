package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import com.tallerwebi.dominio.servicios.ServicioLogin;
import com.tallerwebi.presentacion.DTO.LoginDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorLoginTest {

  private ControladorLogin controladorLogin;
  private LoginDTO datosLoginMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private ServicioLogin servicioLoginMock;

  @BeforeEach
  public void init() {
    datosLoginMock = new LoginDTO("dami@unlam.com", "123");
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    servicioLoginMock = mock(ServicioLogin.class);
    controladorLogin = new ControladorLogin(servicioLoginMock);
  }

  @Test
  public void loginConUsuarioYPasswordIncorrectosDeberiaLlevarALoginNuevamente()
      throws CredencialesInvalidasException {
    // preparacion
    when(servicioLoginMock.autenticar(
            datosLoginMock.getCredencial(),
            datosLoginMock.getPassword()))
        .thenThrow(new CredencialesInvalidasException("Usuario o clave incorrecta"));

    // ejecucion
    ModelAndView modelAndView =
        controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("login"));

    assertThat(
        modelAndView.getModel().get("error").toString(),
        equalToIgnoringCase("Usuario o clave incorrecta"));
  }

  @Test
  public void loginConUsuarioYPasswordCorrectosDeberiaLLevarAHome()
      throws CredencialesInvalidasException {
    // preparacion
    Usuario usuarioEncontradoMock = mock(Usuario.class);

    when(usuarioEncontradoMock.getRol()).thenReturn("ADMIN");
    when(usuarioEncontradoMock.getUsername()).thenReturn("Dami");

    when(requestMock.getSession()).thenReturn(sessionMock);

    when(servicioLoginMock.autenticar(
            datosLoginMock.getCredencial(),
            datosLoginMock.getPassword()))
        .thenReturn(usuarioEncontradoMock);

    // ejecucion
    ModelAndView modelAndView =
        controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("redirect:/home"));

    verify(sessionMock, times(1))
        .setAttribute("ROL", "ADMIN");

    verify(sessionMock, times(1))
        .setAttribute("NOMBRE", "Dami");
  }

  @Test
  public void loginCorrectoSinRequestPorParametroDeberiaUsarRequestDelControlador()
      throws CredencialesInvalidasException {
    // preparacion
    Usuario usuarioEncontradoMock = mock(Usuario.class);

    when(usuarioEncontradoMock.getRol()).thenReturn("ADMIN");
    when(usuarioEncontradoMock.getUsername()).thenReturn("Dami");

    when(requestMock.getSession()).thenReturn(sessionMock);

    when(servicioLoginMock.autenticar(
            datosLoginMock.getCredencial(),
            datosLoginMock.getPassword()))
        .thenReturn(usuarioEncontradoMock);

    controladorLogin = new ControladorLogin(servicioLoginMock, requestMock);

    // ejecucion
    ModelAndView modelAndView =
        controladorLogin.validarLogin(datosLoginMock, null);

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("redirect:/home"));

    verify(sessionMock).setAttribute("ROL", "ADMIN");
    verify(sessionMock).setAttribute("NOMBRE", "Dami");
  }

  @Test
  public void loginCorrectoConRequestPeroSinSesionDeberiaRedirigirAHome()
      throws CredencialesInvalidasException {
    // preparacion
    Usuario usuarioEncontradoMock = mock(Usuario.class);

    when(requestMock.getSession()).thenReturn(null);

    when(servicioLoginMock.autenticar(
            datosLoginMock.getCredencial(),
            datosLoginMock.getPassword()))
        .thenReturn(usuarioEncontradoMock);

    // ejecucion
    ModelAndView modelAndView =
        controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("redirect:/home"));

    verify(sessionMock, never())
        .setAttribute(anyString(), any());
  }

  @Test
  public void irALoginDeberiaRetornarVistaLoginConDatosLogin() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irALogin();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("login"));

    assertThat(
        modelAndView.getModel().get("datosLogin"),
        instanceOf(LoginDTO.class));
  }

  @Test
  public void irAHomeDeberiaRetornarVistaHome() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("home"));
  }

  @Test
  public void irAHomeConRequestPeroSinSesionDeberiaMostrarNombrePorDefecto() {
    // preparacion
    when(requestMock.getSession()).thenReturn(null);

    controladorLogin =
        new ControladorLogin(servicioLoginMock, requestMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("home"));

    assertThat(
        modelAndView.getModel().get("nombreJugador").toString(),
        equalToIgnoringCase("Vecino de La Matanza"));
  }

  @Test
  public void inicioDeberiaRedirigirALogin() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.inicio();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("redirect:/login"));
  }

  @Test
  public void irAHomeConUsuarioLogueadoDeberiaMostrarSuNombre() {
    // preparacion
    when(requestMock.getSession()).thenReturn(sessionMock);
    when(sessionMock.getAttribute("NOMBRE")).thenReturn("Dami");

    controladorLogin =
        new ControladorLogin(servicioLoginMock, requestMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("home"));

    assertThat(
        modelAndView.getModel().get("nombreJugador").toString(),
        equalToIgnoringCase("Dami"));
  }

  @Test
  public void irAHomeSinUsuarioLogueadoDeberiaMostrarNombrePorDefecto() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(
        modelAndView.getViewName(),
        equalToIgnoringCase("home"));

    assertThat(
        modelAndView.getModel().get("nombreJugador").toString(),
        equalToIgnoringCase("Vecino de La Matanza"));
  }

  @Test
  public void cerrarSesionDeberiaInvalidarLaSesionYRedirigirALogin() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(sessionMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.cerrarSesion(requestMock);

    // validacion
    verify(sessionMock, times(1)).invalidate();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/login"));
  }

  @Test
  public void cerrarSesionSinSesionActivaDeberiaRedirigirALoginSinFallar() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(null);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.cerrarSesion(requestMock);

    // validacion
    verify(sessionMock, never()).invalidate();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/login"));
  }
}