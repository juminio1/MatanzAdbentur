package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.NumeroRomanoInvalido;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioLogin")
@Transactional
public class ServicioLoginImpl implements ServicioLogin {

  private RepositorioUsuario repositorioUsuario;

  @Autowired
  public ServicioLoginImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
  }

  @Override
  public Usuario consultarUsuario(String email, String password) {
    return repositorioUsuario.buscarUsuario(email, password);
  }

  @Override
  public void registrar(Usuario usuario) throws UsuarioExistente {
    Usuario usuarioEncontrado = repositorioUsuario.buscarUsuario(
        usuario.getEmail(),
        usuario.getPassword());
    if (usuarioEncontrado != null) {
      throw new UsuarioExistente();
    }
    repositorioUsuario.guardar(usuario);
  }

  @Override
  public String validarContrasenia(String password) {

    if (password == null || password.isEmpty()) {
      return "INVALIDA";
    }
    if (password.matches( "^(?=(?:.*[A-Za-z]){4,})(?=(?:.*\\d){2,})(?=(?:.*[-_%$?!@]){2,}).{8,}$")) {
      return "FUERTE";
    } else if (password.matches("^(?=.*\\d)(?=.*[-_%$?!@]).{8,}$")) {
      return "MEDIANA";
    } else if (password.matches("^.{1,8}$")) {
      return "DEBIL";
    }

    return "";
  }

  @Override
  public String clasificarTemperatura(Integer grados) {
    
    if(grados <= 0){
      return "CONGELANTE";
    }else if (grados > 0 && grados <= 15){
      return "FRIA";
    }else if (grados >= 16 && grados <= 25) {
      return "TEMPLADA";
    }else if (grados >= 26 && grados <= 35) {
      return "CALUROSA";
    }else if(grados > 35){
      return "PELIGROSA";
    }

    return "";
  }

  @Override
  public Integer convertirNumeroRomanoAEntero(String numeroRomano) throws NumeroRomanoInvalido {

    if(!(numeroRomano.toUpperCase().matches("[IVXLC]+"))){
      throw new NumeroRomanoInvalido("El numero romano ingresado es invalido");
    }
    for (int i = 0; i < numeroRomano.length() ; i++ ) {
      
    }
    this.valorNumerico(null);

    return null;

  }
  private Integer valorNumerico(Character letra){
    switch (letra) {
      case 'I':
        return 1;
      case 'V':
        return 5;
      case 'X':
        return 10;
      case 'L':
        return 50;
      case 'C':
        return 100;
    
    }
    return null;
  }

  

}
