package com.tallerwebi.dominio.excepcion;

public class PartidaNoEncontradaException extends Exception {

  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  @java.io.Serial
  private static final long serialVersionUID = 1L;

  public PartidaNoEncontradaException() {
    super("Partida no encontrada");
  }
}
