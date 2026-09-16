package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioRegistro;
import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
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
    ModelAndView modelAndView = controladorRegistro.nuevoUsuario(); //YA TA

    assertEquals("nuevo-usuario", modelAndView.getViewName());
  }

  @Test
  public void deberiaAgregarUnRegistroVacioAlModeloAlMostrarElFormulario() {
    // ejecucion
    ModelAndView modelAndView = controladorRegistro.nuevoUsuario(); //VERIFICAR SI CUMPLE

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("nuevo-usuario"));
    assertThat(modelAndView.getModel().get("registro"), instanceOf(RegistroDTO.class));
  }

  @Test
  public void deberiaRegistrarUsuarioConDatosValidosExitosamente()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    RegistroDTO registro = new RegistroDTO();
    registro.setUsername("JuliMatanza");
    registro.setEmail("juliLaMasMejor@gmail.com");
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza1!"); // Le ingresa datos al usuario

    when(bindingResultMock.hasErrors()).thenReturn(false); // "Cuando el controlador pregunte si el formulario tiene
    // errores, decile que no."

    ModelAndView modelAndView = controladorRegistro.registrarme(registro, bindingResultMock); // Controlador, intentá
    // registrar este usuario.
    // Le pasas un usuario sin
    // horrores

    assertEquals("redirect:/login", modelAndView.getViewName()); // Compara que estas dos vistas sean iguales

    verify(servicioRegistroMock).registrar(registro); // Mockito, comprobame que el controlador llamó al método
    // registrar() del servicio.
  }

  @Test
  public void deberiaRedirigirAlLoginCuandoElRegistroEsExitoso()
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException {
    ModelAndView modelAndView = controladorRegistro.registrarme(registroMock, bindingResultMock);

    when(bindingResultMock.hasErrors()).thenReturn(false);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/login"));
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
}
