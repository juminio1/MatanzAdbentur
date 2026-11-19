package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntUnaryOperator;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;



public class GeneradorPromptTriviaTest {


    @Test
    public void deberiaGenerarUnPromptConLaDificultadSolicitada(){

      ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");
        
        assertTrue(prompt.contains("MEDIO"));

    }

    @Test 
    public void deberiaGenerarUnPromptConLaCantidadDePreguntasSolicitada(){

       ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");
        
        assertTrue(prompt.contains("5 preguntas"));

    }

    @Test 
    public void deberiaIncluirLasReglasDeLasRespuestasEnElPrompt(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");
        
        assertTrue(prompt.contains("Cada pregunta debe tener cuatro opciones"));
        assertTrue(prompt.contains("una única respuesta correcta"));

    }

    @Test 
    public void deberiaIncluirElFormatoJsonEsperadoEnElPrompt(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");
        
        assertTrue(prompt.contains("enunciado"));
        assertTrue(prompt.contains("opciones"));
        assertTrue(prompt.contains("respuestaCorrecta"));
        assertTrue(prompt.contains("dificultad"));

    }

    @Test
    public void deberiaPedirElTemaYElSubtemaElegidosAlAzar(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt =
            new ServicioGeneradorPromptTriviaImpl(azarQueDevuelve(bound -> 0));

        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");

        assertTrue(prompt.contains("TEMA DE ESTA TANDA: Historia argentina"));
        assertTrue(prompt.contains("SUBTEMA: pueblos originarios"));
        assertFalse(prompt.contains("Deportes argentinos"));
    }

    @Test
    public void elAzarPuedeElegirElUltimoTemaYSuUltimoSubtema(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt =
            new ServicioGeneradorPromptTriviaImpl(azarQueDevuelve(bound -> bound - 1));

        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");

        assertTrue(prompt.contains("TEMA DE ESTA TANDA: La Matanza"));
        assertTrue(prompt.contains("SUBTEMA: lugares conocidos"));
    }

    @Test
    public void todosLosTemasTienenSubtemas(){

        assertTrue(ServicioGeneradorPromptTriviaImpl.TEMAS.size() > 1);
        ServicioGeneradorPromptTriviaImpl.TEMAS.forEach(tema -> assertFalse(tema.subtemas().isEmpty()));
    }

    @Test
    public void deberiaPedirQueEviteLasPreguntasTrilladas(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String prompt = generadorPrompt.generarPromptTrivia(5, "FACIL");

        assertTrue(prompt.contains("EVITÁ LAS PREGUNTAS MÁS TRILLADAS"));
        assertTrue(prompt.contains("el mate"));
        assertTrue(prompt.contains("\"dificultad\": \"FACIL\""));
    }

    private static RandomGenerator azarQueDevuelve(IntUnaryOperator eleccion) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return eleccion.applyAsInt(bound);
            }
        };
    }

}
