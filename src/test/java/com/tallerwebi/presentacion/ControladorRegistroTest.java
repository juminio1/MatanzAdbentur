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
      throws UsuarioExistenteException,
      ContraseniaInvalidaException,
      CamposObligatoriosException,
      EmailInvalidoException {

    RegistroDTO registro = new RegistroDTO();

    registro.setUsername("JuliMatanza");
    registro.setEmail("juliLaMasMejor@gmail.com");
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza1!");

    when(bindingResultMock.hasErrors()).thenReturn(false);

    ModelAndView modelAndView = controladorRegistro.registrarme(registro, bindingResultMock);

    assertEquals("redirect:/login",modelAndView.getViewName());

    verify(servicioRegistroMock, times(1)).registrar(registro);
  }

  @Test
  public void deberiaRedirigirAlLoginCuandoElRegistroEsExitoso()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(false);

    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertThat(modelAndView.getViewName(),equalToIgnoringCase("redirect:/login"));

    verify(servicioRegistroMock, times(1)).registrar(registroMock);
  }

  @Test
  public void deberiaVolverAlFormularioCuandoHayErroresDeValidacion()
      throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {

    when(bindingResultMock.hasErrors()).thenReturn(true);

    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("nuevo-usuario"));

    verify(servicioRegistroMock, never()).registrar(registroMock);
  }

  @Test
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
  }
}