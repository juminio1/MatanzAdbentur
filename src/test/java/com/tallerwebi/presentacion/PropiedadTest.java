package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import org.junit.jupiter.api.Test;

import static com.tallerwebi.dominio.enums.GrupoPropiedades.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PropiedadTest {
    @Test
    public void deberiaCrearUnaPropiedadConSusDatos() {
        Propiedad propiedad = new Propiedad(
                1L,
                "Plaza San Justo",
                500,
                100,
                null,
                SAN_JUSTO
        );

        assertEquals(1L, propiedad.getId());
        assertEquals("Plaza San Justo", propiedad.getNombre());
        assertEquals(500, propiedad.getPrecioCompra());
        assertEquals(100, propiedad.getPrecioAlquiler());
        assertNull(propiedad.getPropietario());
        assertEquals(SAN_JUSTO, propiedad.getGrupo());
        assertEquals(0, propiedad.getNivelMejora());
    }

    @Test
    public void deberiaModificarLosDatosDeLaPropiedadConLosSetters() {
        Propiedad propiedad = new Propiedad();

        Jugador jugador = new Jugador();

        propiedad.setId(2L);
        propiedad.setNombre("UNLAM");
        propiedad.setPrecioCompra(600);
        propiedad.setPrecioAlquiler(120);
        propiedad.setPropietario(jugador);
        propiedad.setNivelMejora(2);

        assertEquals(2L, propiedad.getId());
        assertEquals("UNLAM", propiedad.getNombre());
        assertEquals(600, propiedad.getPrecioCompra());
        assertEquals(120, propiedad.getPrecioAlquiler());
        assertEquals(jugador, propiedad.getPropietario());
        assertEquals(2, propiedad.getNivelMejora());
    }
}
