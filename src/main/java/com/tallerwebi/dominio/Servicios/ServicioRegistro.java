package com.tallerwebi.dominio.Servicios;

import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.presentacion.DTO.RegistroDTO;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioRegistro {
  void registrar(RegistroDTO registro)
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException, UsernameExistenteException;
}
