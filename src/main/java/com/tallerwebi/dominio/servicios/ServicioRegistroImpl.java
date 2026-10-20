package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailExistenteException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.DTO.RegistroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioRegistro")
@Transactional
public class ServicioRegistroImpl implements ServicioRegistro {


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
    usuario.setPassword(HashContrasenia.generar(datosRegistro.getPassword()));
    usuario.setUsername(datosRegistro.getUsername());

    repositorioUsuario.guardar(usuario);
  }

  private void validarContrasenia(RegistroDTO registro) throws ContraseniaInvalidaException {
    if (!registro.getPassword().equals(registro.getPasswordRepetido())) {
      throw new ContraseniaInvalidaException("Las contraseñas deben coincidir");
    }
  }
}
