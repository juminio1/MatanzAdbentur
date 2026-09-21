package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.presentacion.RegistroDTO;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioRegistro {
  void registrar(RegistroDTO registro)
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException;
}