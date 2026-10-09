package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.hamcrest.Matchers.is;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import com.tallerwebi.dominio.servicios.ServicioLogin;
import com.tallerwebi.presentacion.DTO.LoginDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Test
  public void obtenerPerfilSinSesionDeberiaRetornarUnauthorized() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(null);

    // ejecucion
    org.springframework.http.ResponseEntity<com.tallerwebi.presentacion.DTO.UsuarioPerfilDTO> respuesta =
        controladorLogin.obtenerPerfil(requestMock);

    // validacion
    assertThat(respuesta.getStatusCode(), org.hamcrest.Matchers.is(org.springframework.http.HttpStatus.UNAUTHORIZED));
  }

  @Test
  public void obtenerPerfilConSesionSinIdDeberiaRetornarUnauthorized() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("id")).thenReturn(null);

    // ejecucion
    org.springframework.http.ResponseEntity<com.tallerwebi.presentacion.DTO.UsuarioPerfilDTO> respuesta =
        controladorLogin.obtenerPerfil(requestMock);

    // validacion
    assertThat(respuesta.getStatusCode(), org.hamcrest.Matchers.is(org.springframework.http.HttpStatus.UNAUTHORIZED));
  }

  @Test
  public void obtenerPerfilConUsuarioLogueadoDeberiaRetornarOkYDatos() {
    // preparacion
    Long idUsuario = 10L;
    Usuario usuarioMock = mock(Usuario.class);
    when(usuarioMock.getId()).thenReturn(idUsuario);
    when(usuarioMock.getUsername()).thenReturn("rafaelposs");
    when(usuarioMock.getRol()).thenReturn("USUARIO");
    when(usuarioMock.getAvatar()).thenReturn("https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/default.png");

    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("id")).thenReturn(idUsuario);
    when(servicioLoginMock.buscarPorId(idUsuario)).thenReturn(usuarioMock);

    // ejecucion
    org.springframework.http.ResponseEntity<com.tallerwebi.presentacion.DTO.UsuarioPerfilDTO> respuesta =
        controladorLogin.obtenerPerfil(requestMock);

    // validacion
    assertThat(respuesta.getStatusCode(), org.hamcrest.Matchers.is(org.springframework.http.HttpStatus.OK));
    assertThat(respuesta.getBody(), org.hamcrest.Matchers.notNullValue());
    assertThat(respuesta.getBody().getUsername(), org.hamcrest.Matchers.is("rafaelposs"));
    assertThat(respuesta.getBody().getRol(), org.hamcrest.Matchers.is("USUARIO"));
    assertThat(respuesta.getBody().getAvatar(), org.hamcrest.Matchers.containsString("default.png"));
  }

@Test
  public void actualizarAvatar_SinSesionActiva_DebeRetornarUnauthorized() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(null);

    java.util.Map<String, String> payload = new java.util.HashMap<>();
    payload.put("avatar", "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/avatar1.png");

    ResponseEntity<Void> respuesta = this.controladorLogin.actualizarAvatar(payload, request);

    assertThat(respuesta.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
  }

  @Test
  public void actualizarAvatar_ConSesionYAvatarValido_DebeRetornarOk() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("id")).thenReturn(1L);

    java.util.Map<String, String> payload = new java.util.HashMap<>();
    payload.put("avatar", "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/avatar2.png");

    ResponseEntity<Void> respuesta = this.controladorLogin.actualizarAvatar(payload, request);

    assertThat(respuesta.getStatusCode(), is(HttpStatus.OK));
    verify(this.servicioLoginMock, times(1)).actualizarAvatar(1L, "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/avatar2.png");
  }

  @Test
  public void actualizarAvatar_ConAvatarVacio_DebeRetornarBadRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("id")).thenReturn(1L);

    java.util.Map<String, String> payload = new java.util.HashMap<>();
    payload.put("avatar", "");

    ResponseEntity<Void> respuesta = this.controladorLogin.actualizarAvatar(payload, request);

    assertThat(respuesta.getStatusCode(), is(HttpStatus.BAD_REQUEST));
  }
}