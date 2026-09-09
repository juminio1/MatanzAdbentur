package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.ContraseniaInvalida;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioRegistro")
@Transactional
public class ServicioRegistroImpl implements ServicioRegistro {

  private static final int MASK_BYTE = 0xff;
  private static final int LONGITUD_UN_DIGITO = 1;
  private static final Pattern PATRON_CONTRASENIA = Pattern.compile(
    "^(?=.*[0-9])(?=.*[-_%$?!@]).{8,}$"
  );

  private final RepositorioUsuario repositorioUsuario;

  @Autowired
  public ServicioRegistroImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
  }

  @Override
  public void registrar(Usuario usuario) throws UsuarioExistente, ContraseniaInvalida {
    if (usuario == null || usuario.getEmail() == null || usuario.getPassword() == null) {
      throw new ContraseniaInvalida("Los datos del usuario son obligatorios.");
    }

    Usuario usuarioExistente = repositorioUsuario.buscarUsuario(
      usuario.getEmail(),
      usuario.getPassword()
    );
    if (usuarioExistente != null) {
      throw new UsuarioExistente();
    }

    validarContrasenia(usuario.getPassword());

    usuario.setPassword(hashearContrasenia(usuario.getPassword()));
    repositorioUsuario.guardar(usuario);
  }

  private void validarContrasenia(String password) throws ContraseniaInvalida {
    if (!PATRON_CONTRASENIA.matcher(password).matches()) {
      throw new ContraseniaInvalida(
        "La contraseña debe tener al menos 8 caracteres, 1 número y 1 símbolo (-_%$?!@)."
      );
    }
  }

  private String hashearContrasenia(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
      for (byte unByte : encodedhash) {
        String hex = Integer.toHexString(MASK_BYTE & unByte);
        if (hex.length() == LONGITUD_UN_DIGITO) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException excepcion) {
      throw new IllegalArgumentException("Algoritmo de hash no soportado", excepcion);
    }
  }
}
