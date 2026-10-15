package com.tallerwebi.dominio.excepcion;

public class DatosEnventoInvalidoExceptions extends Exception {

    private static final long serialVersionUID = 1L;
    
    public DatosEnventoInvalidoExceptions(String mensaje) {
        super(mensaje);
    }

}
