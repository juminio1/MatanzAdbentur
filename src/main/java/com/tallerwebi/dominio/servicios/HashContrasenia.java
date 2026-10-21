package com.tallerwebi.dominio.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class HashContrasenia {

    private HashContrasenia() {}

    static String generar(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalArgumentException("Algoritmo de hash no soportado", excepcion);
        }
    }
}
