package com.tallerwebi.dominio.excepcion;

import com.tallerwebi.dominio.enums.TipoEfectoEvento;

@SuppressWarnings("serial")
public class TipoEfectoEventoNoSoportadoException extends RuntimeException {
    public TipoEfectoEventoNoSoportadoException(TipoEfectoEvento tipoEfecto) {
        super("No se puede aplicar el efecto de evento: " + tipoEfecto);
    }
}
