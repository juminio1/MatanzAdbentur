package com.tallerwebi.dominio.excepcion;

public class SalaDeEsperaLlenaException extends Exception {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

  public SalaDeEsperaLlenaException() {
    super("La sala de espera está llena. No se pueden agregar más usuarios.");
  }

}
