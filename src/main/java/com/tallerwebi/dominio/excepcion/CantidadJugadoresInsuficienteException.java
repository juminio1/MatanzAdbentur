package com.tallerwebi.dominio.excepcion;

public class CantidadJugadoresInsuficienteException extends Exception{
private static final long serialVersionUID = 1L;


 public CantidadJugadoresInsuficienteException(String mensaje) {
    super(mensaje);
  }

}
