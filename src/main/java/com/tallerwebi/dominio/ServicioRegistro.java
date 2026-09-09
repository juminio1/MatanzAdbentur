package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.ContraseniaInvalida;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioRegistro {
  void registrar(Usuario usuario) throws UsuarioExistente, ContraseniaInvalida;
}
