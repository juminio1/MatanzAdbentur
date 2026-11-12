package com.tallerwebi.presentacion.DTO;

/**
 * Premio que elige el ganador de una trivia difícil: "PLATA" o "ROBAR".
 * posicion es la del casillero de la propiedad y solo se envía al robar.
 */
public record SeleccionPremioDTO(String premio, Integer posicion) {
}
