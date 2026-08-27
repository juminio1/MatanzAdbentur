package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.NumeroRomanoInvalido;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;

public interface ServicioLogin {
  Usuario consultarUsuario(String email, String password);
  void registrar(Usuario usuario) throws UsuarioExistente;
  String validarContrasenia(String password);
  String clasificarTemperatura(Integer grados);
  Integer convertirNumeroRomanoAEntero(String numeroRomano) throws NumeroRomanoInvalido;
  
}
