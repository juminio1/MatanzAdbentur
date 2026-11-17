package com.tallerwebi.dominio.enums;

public enum DificultadTrivia {
    SIMPLE,
    MEDIA,
    COMPLICADA;

    public static DificultadTrivia desdeRuleta(DificultadRuleta dificultad) {
        return switch (dificultad) {
            case FACIL -> SIMPLE;
            case MEDIO -> MEDIA;
            case DIFICIL -> COMPLICADA;
        };
    }
}
