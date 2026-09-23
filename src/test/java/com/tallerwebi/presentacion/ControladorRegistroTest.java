package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.ServicioRegistro;
import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailExistenteException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

public class ControladorRegistroTest {

  private ServicioRegistro servicioRegistroMock;
  private ControladorRegistro controladorRegistro;
  private BindingResult bindingResultMock;
  private RegistroDTO registroMock;

  @BeforeEach
  public void init() {

    servicioRegistroMock = mock(ServicioRegistro.class);
    bindingResultMock = mock(BindingResult.class);
    registroMock = mock(RegistroDTO.class);

    controladorRegistro = new ControladorRegistro(servicioRegistroMock);
  }

  @Test
  public void deberiaMostrarElFormularioDeRegistro() {

    ModelAndView modelAndView = controladorRegistro.nuevoUsuario();

    assertEquals("nuevo-usuario",modelAndView.getViewName());
  }

  @Test
  public void deberiaAgregarUnRegistroVacioAlModeloAlMostrarElFormulario() {

    ModelAndView modelAndView = controladorRegistro.nuevoUsuario();

    assertThat(modelAndView.getViewName(),equalToIgnoringCase("nuevo-usuario"));

    assertThat(modelAndView.getModel().get("registro"),instanceOf(RegistroDTO.class));
  }

  @Test
  public void deberiaRegistrarUsuarioConDatosValidosExitosamente()
<<<<<<< HEAD
      throws UsuarioExistenteException,
      ContraseniaInvalidaException,
      CamposObligatoriosException,
      EmailInvalidoException {

=======
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204
    RegistroDTO registro = new RegistroDTO();

    registro.setUsername("JuliMatanza");
    registro.setEmail("juliLaMasMejor@gmail.com");
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza1!");

    when(bindingResultMock.hasErrors()).thenReturn(false);

    ModelAndView modelAndView = controladorRegistro.registrarme(registro, bindingResultMock);

    assertEquals("redirect:/login",modelAndView.getViewName());

<<<<<<< HEAD
    verify(servicioRegistroMock, times(1)).registrar(registro);
=======
    verify(servicioRegistroMock).registrar(registro); // Mockito, comprobame que el controlador llamó al método
    // registrar() del servicio.
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204
  }

  @Test
  public void deberiaRedirigirAlLoginCuandoElRegistroEsExitoso()
<<<<<<< HEAD
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
=======
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204

    when(bindingResultMock.hasErrors()).thenReturn(false);

    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertThat(modelAndView.getViewName(),equalToIgnoringCase("redirect:/login"));

    verify(servicioRegistroMock, times(1)).registrar(registroMock);
  }

  @Test
  public void deberiaVolverAlFormularioCuandoHayErroresDeValidacion()
<<<<<<< HEAD
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

=======
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204
    when(bindingResultMock.hasErrors()).thenReturn(true);

    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("nuevo-usuario"));

    verify(servicioRegistroMock, never()).registrar(registroMock);
  }

  @Test
<<<<<<< HEAD
  public void cuandoElEmailYaExisteDebeVolverAlFormularioYAgregarErrorAlEmail()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new EmailExistenteException()).when(servicioRegistroMock).registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario",resultado.getViewName());

    verify(bindingResultMock).rejectValue("email","email.existente", "Este email ya se encuentra registrado");
  }

  @Test
  public void cuandoElUsernameYaExisteDebeVolverAlFormularioYAgregarErrorAlUsername()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new UsernameExistenteException()).when(servicioRegistroMock).registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario", resultado.getViewName());

    verify(bindingResultMock).rejectValue("username", "username.existente", "Este apodo ya se encuentra registrado");
  }

  @Test
  public void cuandoLasContraseniasNoCoincidenDebeAgregarErrorEnPasswordRepetido()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new ContraseniaInvalidaException("Las contraseñas no coinciden")).when(servicioRegistroMock).registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario", resultado.getViewName());

    verify(bindingResultMock).rejectValue("passwordRepetido", "password.no.coincide", "Las contraseñas no coinciden");
  }

  @Test
  public void cuandoElEmailEsInvalidoDebeAgregarErrorAlCampoEmail()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException,
      EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new EmailInvalidoException("El email no tiene un formato válido")).when(servicioRegistroMock)
        .registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario", resultado.getViewName());

    verify(bindingResultMock).rejectValue("email", "email.invalido", "El email no tiene un formato válido");
  }

  @Test
  public void cuandoFaltanCamposObligatoriosDebeVolverAlFormulario()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException,
      EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new CamposObligatoriosException("Todos los campos son obligatorios")).when(servicioRegistroMock)
        .registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario", resultado.getViewName());

    verify(bindingResultMock).reject("campos.obligatorios", "Todos los campos son obligatorios");
  }

  @Test
  public void cuandoOcurreUnUsuarioExistenteGeneralDebeVolverAlFormulario()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException,
      EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new UsuarioExistenteException("El usuario ya existe")).when(servicioRegistroMock).registrar(registroMock);

    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals("nuevo-usuario", resultado.getViewName());

    verify(bindingResultMock).reject("usuario.existente", "El usuario ya se encuentra registrado");
=======
  public void cuandoRegistroUnUsuarioDuplicadoDebeLanzarExcepcion()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new UsuarioExistenteException()).when(servicioRegistroMock).registrar(registroMock);
    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals(resultado.getViewName(), "nuevo-usuario");
    assertEquals("El usuario ya existe", resultado.getModel().get("error"));
  }

  @Test
  public void cuandoRegistroUnUsuarioConContraseniaInvalidaDebeLanzarUnaException()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new ContraseniaInvalidaException("La contraseña no es válida"))
      .when(servicioRegistroMock)
      .registrar(registroMock);
    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals(resultado.getViewName(), "nuevo-usuario");
    assertEquals("La contraseña no es válida", resultado.getModel().get("error"));
  }

  @Test
  public void cuandoRegistroUnUsuarioConEmailInvalidoDebeLanzarUnaException()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new EmailInvalidoException("El email no tiene un formato valido"))
      .when(servicioRegistroMock)
      .registrar(registroMock);
    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals(resultado.getViewName(), "nuevo-usuario");
    assertEquals("El email no tiene un formato valido", resultado.getModel().get("error"));
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204
  }

  @Test
  public void cuandoRegistroUnUsuarioSinCompletarLosCamposObligatoriosDebeLanzarUnaException()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    when(bindingResultMock.hasErrors()).thenReturn(false);

    doThrow(new CamposObligatoriosException("Los campos deben ser obligatorios"))
      .when(servicioRegistroMock)
      .registrar(registroMock);
    ModelAndView resultado = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertEquals(resultado.getViewName(), "nuevo-usuario");
    assertEquals("Los campos deben ser obligatorios", resultado.getModel().get("error"));
  }
}
