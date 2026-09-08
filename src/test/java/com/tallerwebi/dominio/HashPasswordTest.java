package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class HashPasswordTest {

  @Test
  public void deberiaRetornarNullSiElPasswordEsNull() {
    assertThat(HashPassword.hashear(null), nullValue());
  }

  @Test
  public void deberiaRetornarHashDistintoAlTextoOriginal() {
    String original = "Password123$";
    String hasheada = HashPassword.hashear(original);

    assertThat(hasheada, not(equalTo(original)));
  }

  @Test
  public void deberiaGenerarElMismoHashParaElMismoPassword() {
    String p1 = HashPassword.hashear("Password123$");
    String p2 = HashPassword.hashear("Password123$");

    assertThat(p1, equalTo(p2));
  }
}
