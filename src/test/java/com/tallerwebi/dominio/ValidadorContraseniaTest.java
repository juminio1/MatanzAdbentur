package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import org.junit.jupiter.api.Test;

public class ValidadorContraseniaTest {

  @Test
  public void debeDevolverINVALIDACuandoLaContraseniaEsNula() {
    String resultado = ValidadorContrasenia.validarFortaleza(null);
    assertThat(resultado, equalToIgnoringCase("INVALIDA"));
  }

  @Test
  public void debeDevolverINVALIDACuandoLaContraseniaEsVacia() {
    String resultado = ValidadorContrasenia.validarFortaleza("");
    assertThat(resultado, equalToIgnoringCase("INVALIDA"));
  }

  @Test
  public void debeDevolverDEBILCuandoTieneMenosDeOchoCaracteres() {
    String resultado = ValidadorContrasenia.validarFortaleza("A1@");
    assertThat(resultado, equalToIgnoringCase("DEBIL"));
  }

  @Test
  public void debeDevolverDEBILCuandoTieneOchoCaracteresSinNumerosNiEspeciales() {
    String resultado = ValidadorContrasenia.validarFortaleza("AAAABBBB");
    assertThat(resultado, equalToIgnoringCase("DEBIL"));
  }

  @Test
  public void debeDevolverMEDIANACuandoTieneOchoCaracteresConNumeroYCaracterEspecial() {
    String resultado = ValidadorContrasenia.validarFortaleza("AAABBB$1");
    assertThat(resultado, equalToIgnoringCase("MEDIANA"));
  }
}
