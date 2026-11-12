package com.tallerwebi.presentacion.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class TriviaDTOTest {

    private final ObjectMapper lector = new ObjectMapper();

    @Test
    public void deberiaLeerLaRespuestaEnviadaPorElFrontend() throws Exception {
        RespuestaTriviaDTO respuesta = lector.readValue("{\"id\":2,\"opcion\":3}", RespuestaTriviaDTO.class);

        assertEquals(2, respuesta.id());
        assertEquals(3, respuesta.opcion());
    }

    @Test
    public void deberiaLeerElPremioElegidoConLaPropiedadARobar() throws Exception {
        SeleccionPremioDTO seleccion = lector.readValue("{\"premio\":\"ROBAR\",\"posicion\":9}", SeleccionPremioDTO.class);

        assertEquals("ROBAR", seleccion.premio());
        assertEquals(9, seleccion.posicion());
    }

    @Test
    public void deberiaPermitirElegirElPremioEnDineroSinPropiedad() throws Exception {
        SeleccionPremioDTO seleccion = lector.readValue("{\"premio\":\"PLATA\"}", SeleccionPremioDTO.class);

        assertEquals("PLATA", seleccion.premio());
        assertNull(seleccion.posicion());
    }

    @Test
    public void elResultadoDeLaRespuestaSoloIndicaSiFueCorrecta() throws Exception {
        String json = lector.writeValueAsString(new ResultadoRespuestaTriviaDTO(true));

        assertEquals("{\"correcta\":true}", json);
    }
}
