package com.tallerwebi.dominio.excepcion;

public class NumeroRomanoInvalido extends Exception{
  /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
  private static final long serialVersionUID = 1L;
  private String mensaje;

  public String getMensaje() {
    return mensaje;
}

  public void setMensaje(String mensaje) {
    this.mensaje = mensaje;
  }

  public NumeroRomanoInvalido(String mensaje){
    this.mensaje = mensaje;
  }


}
