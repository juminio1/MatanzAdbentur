package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CredencialesInvalidasException;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioLogin")
@Transactional
public class ServicioLoginImpl implements ServicioLogin {

  private static final String ERROR_CREDENCIALES = "Usuario o clave incorrecta";

  private final RepositorioUsuario repositorioUsuario;

  @Autowired
  public ServicioLoginImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
  }

  @Override
  public Usuario consultarUsuario(String email) {
    return repositorioUsuario.buscarUsuarioPorEmail(email);
  }

  @Override
  public Usuario autenticar(String email, String password) throws CredencialesInvalidasException {
    validarParametros(email, password);

    Usuario usuario = repositorioUsuario.buscarUsuarioPorEmail(email);
    if (usuario == null) {
      throw new CredencialesInvalidasException(ERROR_CREDENCIALES);
    }

    validarPassword(password, usuario.getPassword());

    return usuario;
  }

  private void validarParametros(String email, String password)
    throws CredencialesInvalidasException {
    if (esInvalido(email) || esInvalido(password)) {
      throw new CredencialesInvalidasException(ERROR_CREDENCIALES);
    }
  }

  private boolean esInvalido(String dato) {
    return dato == null || dato.isBlank();
  }

  private void validarPassword(String passwordIngresada, String passwordAlmacenada)
    throws CredencialesInvalidasException {
    String passwordHasheada = hashearContrasenia(passwordIngresada);
    if (!passwordHasheada.equals(passwordAlmacenada)) {
      throw new CredencialesInvalidasException(ERROR_CREDENCIALES);
    }
  }

  private String hashearContrasenia(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException excepcion) {
      throw new IllegalArgumentException("Algoritmo de hash no soportado", excepcion);
    }
  }
}
