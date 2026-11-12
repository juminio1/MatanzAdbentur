package com.tallerwebi.presentacion.DTO;

/**
 * Premio que elige el ganador de una trivia difícil.
 * posicionPropiedad solo se envía cuando el premio es robar una propiedad.
 */
public record SeleccionPremioDTO(String premio, Integer posicionPropiedad) {
}
