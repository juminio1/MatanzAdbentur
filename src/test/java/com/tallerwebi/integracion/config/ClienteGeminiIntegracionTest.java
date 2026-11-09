
package com.tallerwebi.integracion.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.ClienteGeminiImpl;
import com.tallerwebi.dominio.servicios.ServicioGeneradorPromptTriviaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;

public class ClienteGeminiIntegracionTest {

    @Test //ESTA PRUEBA GASTA TOKENS
    @Disabled("Prueba manual: realiza una solicitud real a Gemini") //Esto evita que se ejecute automaticamente
    public void deberiaGenerarCincoPreguntasRealesConGemini() {

        String apiKey = System.getenv("GEMINI_API_KEY"); //Obtiene la api key desde la variable de entorno (La q se configuro desde la terminal)

        assertNotNull(apiKey);
        assertFalse(apiKey.isBlank());

        String modelo = System.getenv().getOrDefault("GEMINI_MODELO", "gemini-3.5-flash-lite");
        
        //Utiliza la clase real sin mock
        ClienteIA clienteIA = new ClienteGeminiImpl(HttpClient.newHttpClient(), new ObjectMapper(), new ServicioGeneradorPromptTriviaImpl(), apiKey, modelo);

        //Generamos preguntitas
        List<PreguntaGenerada> preguntas = clienteIA.generarPreguntas("FACIL", 5);

        assertEquals(5, preguntas.size());

        for (PreguntaGenerada pregunta : preguntas) {

            assertNotNull(pregunta.getEnunciado());
            assertFalse(pregunta.getEnunciado().isBlank());

            assertEquals(4, pregunta.getOpciones().size());

            assertEquals("FACIL", pregunta.getDificultad());

            assertTrue(pregunta.getRespuestaCorrecta() >= 0);
            assertTrue(pregunta.getRespuestaCorrecta() <= 3);
        }
    }
}
