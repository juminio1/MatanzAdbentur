package com.tallerwebi.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashPassword {

  private static final int MASK_BYTE = 0xff;
  private static final int LONGITUD_UN_DIGITO = 1;

  private HashPassword() {
    // Constructor privado para clase utilitaria
  }

  public static String hashear(String password) {
    if (password == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
      for (byte b : encodedhash) {
        String hex = Integer.toHexString(MASK_BYTE & b);
        if (hex.length() == LONGITUD_UN_DIGITO) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Algoritmo de hash no soportado", e);
    }
  }
}
