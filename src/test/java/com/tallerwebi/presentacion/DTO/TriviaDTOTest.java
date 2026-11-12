package com.tallerwebi.presentacion.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class TriviaDTOTest {

    private final ObjectMapper lector = new ObjectMapper();

    @Test
    public void deberiaLeerLaRespuestaEnviadaPorElFrontend() throws Exception {
        RespuestaTriviaDTO respuesta = lector.readValue("{\"ronda\":2,\"opcionElegida\":3}", RespuestaTriviaDTO.class);

        assertEquals(2, respuesta.ronda());
        assertEquals(3, respuesta.opcionElegida());
    }

    @Test
    public void deberiaLeerElPremioElegidoConLaPropiedadARobar() throws Exception {
        SeleccionPremioDTO seleccion = lector.readValue("{\"premio\":\"ROBAR_PROPIEDAD\",\"posicionPropiedad\":9}", SeleccionPremioDTO.class);

        assertEquals("ROBAR_PROPIEDAD", seleccion.premio());
        assertEquals(9, seleccion.posicionPropiedad());
    }

    @Test
    public void deberiaPermitirElegirUnPremioSinPropiedad() throws Exception {
        SeleccionPremioDTO seleccion = lector.readValue("{\"premio\":\"DINERO\"}", SeleccionPremioDTO.class);

        assertEquals("DINERO", seleccion.premio());
        assertNull(seleccion.posicionPropiedad());
    }
}
