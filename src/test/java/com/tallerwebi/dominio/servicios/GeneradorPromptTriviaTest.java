package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GeneradorPromptTriviaTest {

    
    public GeneradorPromptTriviaTest(){

    }

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
        
        assertTrue(prompt.contains("con cuatro opciones"));
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
    public void deberiaIncluirLasTematicasArgentinasEnElPrompt(){

        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String prompt = generadorPrompt.generarPromptTrivia(5, "MEDIO");
        
        assertTrue(prompt.contains("Historia argentina"));
        assertTrue(prompt.contains("Musica y cultura popular"));
        assertTrue(prompt.contains("Deportes argentinos"));

    }


}
