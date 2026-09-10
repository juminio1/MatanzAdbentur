package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.presentacion.RegistroDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioRegistroImplTest {

  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioRegistro servicioRegistro;

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

    servicioRegistro.registrar(registro);

    verify(repositorioUsuarioMock, times(1)).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaGuardarElEmailDelUsuarioCorrectamente() throws Exception {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);

    servicioRegistro.registrar(registro);

    verify(repositorioUsuarioMock)
      .guardar(argThat(usuario -> EMAIL_VALIDO.equals(usuario.getEmail())));
  }

  @Test
  public void deberiaGuardarLaContraseniaHasheada() throws Exception {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(null);

    servicioRegistro.registrar(registro);

    verify(repositorioUsuarioMock)
      .guardar(
        argThat(usuario ->
          usuario.getPassword() != null && !PASSWORD_VALIDA.equals(usuario.getPassword())
        )
      );
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElUsuarioYaExiste() {
    RegistroDTO registro = crearRegistroValido();

    when(repositorioUsuarioMock.buscarUsuarioPorEmail(EMAIL_VALIDO)).thenReturn(new Usuario());

    assertThrows(UsuarioExistenteException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElEmailEstaVacio() {
    RegistroDTO registro = crearRegistroValido();
    registro.setEmail("");

    assertThrows(CamposObligatoriosException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaEstaVacia() {
    RegistroDTO registro = crearRegistroValido();
    registro.setPassword("");

    assertThrows(CamposObligatoriosException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElApodoEstaVacio() {
    RegistroDTO registro = crearRegistroValido();
    registro.setUsername("");

    assertThrows(CamposObligatoriosException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoElEmailTieneFormatoInvalido() {
    RegistroDTO registro = crearRegistroValido();
    registro.setEmail("email-invalido");

    assertThrows(EmailInvalidoException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaEsMuyCorta() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("Ma1!");
    registro.setPasswordRepetido("Ma1!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaNoTieneMayuscula() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("matanza1!");
    registro.setPasswordRepetido("matanza1!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaNoTieneMinuscula() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("MATANZA1!");
    registro.setPasswordRepetido("MATANZA1!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaNoTieneNumero() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("Matanza!!");
    registro.setPasswordRepetido("Matanza!!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLaContraseniaNoTieneCaracterEspecial() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("Matanza12");
    registro.setPasswordRepetido("Matanza12");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  @Test
  public void deberiaLanzarExcepcionCuandoLasContraseniasNoCoinciden() {
    RegistroDTO registro = crearRegistroValido();

    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza2!");

    assertThrows(ContraseniaInvalidaException.class, () -> servicioRegistro.registrar(registro));

    verify(repositorioUsuarioMock, never()).guardar(any(Usuario.class));
  }

  private RegistroDTO crearRegistroValido() {
    RegistroDTO registro = new RegistroDTO();

    registro.setUsername("Juli");
    registro.setEmail(EMAIL_VALIDO);
    registro.setPassword(PASSWORD_VALIDA);
    registro.setPasswordRepetido(PASSWORD_VALIDA);

    return registro;
  }
}
