package com.tallerwebi.dominio;

public final class ValidadorContrasenia {

  private static final int LONGITUD_MINIMA = 8;
  private static final String CARACTERES_ESPECIALES = "-_%$?!@";

  private ValidadorContrasenia() {
    // Constructor privado para clase utilitaria
  }

  public static String validarFortaleza(String contrasenia) {
    if (contrasenia == null || contrasenia.trim().isEmpty()) {
      return "INVALIDA";
    }

    if (contrasenia.length() < LONGITUD_MINIMA) {
      return "DEBIL";
    }

    boolean tieneNumero = contrasenia.matches(".*\\d.*");
    boolean tieneEspecial = contrasenia
      .chars()
      .anyMatch(ch -> CARACTERES_ESPECIALES.indexOf(ch) >= 0);

    if (tieneNumero && tieneEspecial) {
      return "MEDIANA";
    }

    return "DEBIL";
  }

  public static boolean esValida(String contrasenia) {
    String fortaleza = validarFortaleza(contrasenia);
    return !"INVALIDA".equalsIgnoreCase(fortaleza) && !"DEBIL".equalsIgnoreCase(fortaleza);
  }
}
