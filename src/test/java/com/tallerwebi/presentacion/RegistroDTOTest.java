package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RegistroDTOTest {

  @Test
  public void dadoQueExisteUnFormularioDeRegistroDeberiaDevolverUnRegistroConDatosCargados() {
    RegistroDTO registro = new RegistroDTO();

    registro.setUsername("Juli");
    registro.setEmail("juli@gmail.com");
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza1!");

    assertThat(registro.getUsername(), is("Juli"));
    assertThat(registro.getEmail(), is("juli@gmail.com"));
    assertThat(registro.getPassword(), is("Matanza1!"));
    assertThat(registro.getPasswordRepetido(), is("Matanza1!"));
  }

  @Test 
  public void quieroMostrarLosDatosDeUnFormulario(){

    RegistroDTO registro = new RegistroDTO();

    registro.setUsername("Juli");
    registro.setEmail("juli@gmail.com");
    registro.setPassword("Matanza1!");
    registro.setPasswordRepetido("Matanza1!");

    assertTrue(registro.getUsername() != null);
    assertTrue(registro.getEmail() != null);
    assertTrue(registro.getPassword() != null);
    assertTrue(registro.getPasswordRepetido() != null);

  }
}
