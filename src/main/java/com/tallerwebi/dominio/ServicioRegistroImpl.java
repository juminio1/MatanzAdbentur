package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailExistenteException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.RegistroDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioRegistro")
@Transactional
public class ServicioRegistroImpl implements ServicioRegistro {

  private static final int MASK_BYTE = 0xff;
  private static final int LONGITUD_UN_DIGITO = 1;

  private final RepositorioUsuario repositorioUsuario;

  @Autowired
  public ServicioRegistroImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
  }

  @Override
  public void registrar(RegistroDTO datosRegistro)
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException, UsernameExistenteException {
    Usuario usuarioExistenteEmail = repositorioUsuario.buscarUsuarioPorEmail(
      datosRegistro.getEmail()
    );
    Usuario usuarioExistenteUsername = repositorioUsuario.buscarUsuarioPorUsername(
      datosRegistro.getUsername()
    );

    if (usuarioExistenteEmail != null) {
      throw new EmailExistenteException();
    }
    if (usuarioExistenteUsername != null) {
      throw new UsernameExistenteException();
    }

    this.validarContrasenia(datosRegistro);

    Usuario usuario = new Usuario();
    usuario.setEmail(datosRegistro.getEmail());
    usuario.setPassword(hashearContrasenia(datosRegistro.getPassword()));
    usuario.setUsername(datosRegistro.getUsername());

    repositorioUsuario.guardar(usuario);
  }

  private void validarContrasenia(RegistroDTO registro) throws ContraseniaInvalidaException {
    if (!registro.getPassword().equals(registro.getPasswordRepetido())) {
      throw new ContraseniaInvalidaException("Las contraseñas deben coincidir");
    }
  }

  private String hashearContrasenia(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256"); //Instancia un algoritmo
      byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8)); //El algoritmo codifica en utf_8
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
