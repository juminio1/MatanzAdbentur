package com.tallerwebi.dominio.enums;

/** Tipos del catálogo persistido de eventos globales. */
public enum TipoEventoGlobal {
    TRAMITES_AFIP, INUNDACION, PIQUETE, ROBO, FIAT_600, ACCIDENTE_SARMIENTO;

    public boolean requiereDados() {
        return this == TRAMITES_AFIP || this == INUNDACION || this == PIQUETE;
    }
}
