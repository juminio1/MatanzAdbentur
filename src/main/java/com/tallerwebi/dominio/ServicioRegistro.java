package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CamposObligatoriosException;
import com.tallerwebi.dominio.excepcion.ContraseniaInvalidaException;
import com.tallerwebi.dominio.excepcion.EmailInvalidoException;
import com.tallerwebi.dominio.excepcion.UsernameExistenteException;
import com.tallerwebi.dominio.excepcion.UsuarioExistenteException;
import com.tallerwebi.presentacion.RegistroDTO;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioRegistro {
  void registrar(RegistroDTO registro)
<<<<<<< HEAD
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException, UsuarioExistenteException, UsernameExistenteException;
=======
    throws UsuarioExistenteException, ContraseniaInvalidaException, CamposObligatoriosException, EmailInvalidoException;
>>>>>>> 86d2d47f8c47869e500e91aa26bd1814ced92204
}
