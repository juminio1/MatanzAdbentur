package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;

public class ClienteGeminiTest {
    
    
    private final HttpClient clienteHttpMock; //Es un componente que ejecuta la petición HTTP de cheminai. Es una versión simulada
    @SuppressWarnings("unchecked")
    private final HttpResponse<String> respuestaHttpMock; //Simula la respuesta de cheminai

    public ClienteGeminiTest(){

        this.clienteHttpMock =  mock(HttpClient.class);
        this.respuestaHttpMock =  mock(HttpResponse.class);

    }

    @Test 
    public void deberiaGenerarCincoPreguntasCuandoGeminiRespondeCorrectamente() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 2",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 1,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 3",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 2,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 4",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 3,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 5",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "MEDIO"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("MEDIO", 5); //Se solicitan 5 preguntitas

        assertEquals(5, preguntas.size()); //Verifica 
    }

    @Test 
    public void deberiaGenerarUnaPreguntaCuandoSeSolicitaUnaSola() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("DIFICIL", 1); //Se solicitan 5 preguntitas

        assertEquals(1, preguntas.size()); //Verifica 

    }

    @Test 
    public void deberiaLanzarClienteIAExceptionCuandoLaDificultadNoCoincide() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "MEDIO"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

            assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaDificultadSolicitadaEsVacia() throws Exception{ //El cliente rechaza parámetros inválidos antes de llamar a la API

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("", 1);}); //Se comprueba si lanza la exception

        assertEquals("La dificultad solicitada no es válida", excepcion.getMessage());
        
        verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any() //comprueba que nunca llama al método send() del cliente HTTP. 
    );

    }


    @Test 
    public void deberiaLanzarExcepcionCuandoGeminiDevuelveDificultadVacia() throws Exception{ //El cliente rechaza una pregunta cuya dificultad no es válida

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": ""
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoGeminiDevuelveEnunciadoEstaVacio() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoElEnunciadoEsNulo() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": null,
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoFaltanOpciones() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoUnaOpcionEstaVacia() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", ""],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLasOpcionesSonNulas() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": null,
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }


    @Test 
    public void deberiaLanzarExcepcionCuandoLaRespuestaCorrectaEsInvalida() throws Exception{

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A","B","C","D"],
                      "respuestaCorrecta": 5,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaRespuestaCorrectaEsNula() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": null,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaCantidadDePreguntasNoCoincide() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 2",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 1,
                      "dificultad": "MEDIO"
                    },
                    {
                      "enunciado": "Pregunta 3",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 2,
                      "dificultad": "MEDIO"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("MEDIO", 5);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una cantidad incorrecta de preguntas", excepcion.getMessage());

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaDificultadEsNula() throws Exception{ //El cliente rechaza parámetros inválidos antes de llamar a la API

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas(null, 1);}); //Se comprueba si lanza la exception

        assertEquals("La dificultad solicitada no es válida", excepcion.getMessage());
        
        verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any() //comprueba que nunca llama al método send() del cliente HTTP. 
    );

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaDificultadNoExiste() throws Exception{ //El cliente rechaza parámetros inválidos antes de llamar a la API

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("EXTREMO", 1);}); //Se comprueba si lanza la exception

        assertEquals("La dificultad solicitada no es válida", excepcion.getMessage());
        
        verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any() //comprueba que nunca llama al método send() del cliente HTTP. 
    );

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaCantidadEsCero() throws Exception{ //El cliente rechaza parámetros inválidos antes de llamar a la API

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 0);}); //Se comprueba si lanza la exception

        assertEquals("La cantidad solicitada de preguntas no es válida", excepcion.getMessage());
        
        verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any() //comprueba que nunca llama al método send() del cliente HTTP. 
    );

    }

    @Test 
    public void deberiaLanzarExcepcionCuandoLaCantidadEsNula() throws Exception{ //El cliente rechaza parámetros inválidos antes de llamar a la API

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", null);}); //Se comprueba si lanza la exception

        assertEquals("La cantidad solicitada de preguntas no es válida", excepcion.getMessage());
        
        verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any() //comprueba que nunca llama al método send() del cliente HTTP. 
    );

    }

    @Test
    public void deberiaGenerarPreguntasDeDificultadFacil() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "FACIL"
                    },
                    {
                      "enunciado": "Pregunta 2",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 1,
                      "dificultad": "FACIL"
                    },
                    {
                      "enunciado": "Pregunta 3",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 2,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 3); //Se solicitan 5 preguntitas

       assertEquals(3, preguntas.size());

       for (PreguntaGenerada pregunta : preguntas) {
        assertEquals("FACIL", pregunta.getDificultad()); //Preguntar si esta bien utilizar un for en un test
        }

        verify(clienteHttpMock, times(1)).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()); //Verifica que el cliente haya realizado una soli de http

    }

    @Test 
    public void deberiaConservarLasOpcionesEnElOrdenRecibido() throws Exception{

      ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 0,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 1); 

       assertEquals(List.of("Buenos Aires", "Córdoba", "Santa Fe", "Mendoza"), preguntas.getFirst().getOpciones());

    }

    @Test 
    public void deberiaConservarElIndiceDeLaRespuestaCorrecta() throws Exception{

      ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 2,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 1); 

       assertEquals(2, preguntas.getFirst().getRespuestaCorrecta()); //Se comprueba si el indice de la respuesta es la q esta guardada

       assertEquals("Santa Fe", preguntas.getFirst().getOpciones().get(preguntas.getFirst().getRespuestaCorrecta())); //Verifica que el indice correponda a Santa Fe
    }

    @Test 
    public void deberiaGenerarPreguntasConRespuestaCorrectaEnIndiceCero() throws Exception {

       ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 0,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 1); 

       assertEquals(0, preguntas.getFirst().getRespuestaCorrecta()); //Se comprueba si el indice de la respuesta es la q esta guardada

       assertEquals("Buenos Aires", preguntas.getFirst().getOpciones().get(preguntas.getFirst().getRespuestaCorrecta()));

    }

    @Test 
    public void deberiaGenerarPreguntasConRespuestaCorrectaEnIndiceTres() throws Exception {

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 3,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 1); 

       assertEquals(3, preguntas.getFirst().getRespuestaCorrecta()); //Se comprueba si el indice de la respuesta es la q esta guardada

       assertEquals("Mendoza", preguntas.getFirst().getOpciones().get(preguntas.getFirst().getRespuestaCorrecta()));

    }


    @Test 
   public void deberiaGenerarPreguntasConRespuestaCorrectaEnIndiceUno() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 1,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

        List<PreguntaGenerada> preguntas = clienteGemini.generarPreguntas("FACIL", 1); 

       assertEquals(1, preguntas.getFirst().getRespuestaCorrecta()); //Se comprueba si el indice de la respuesta es la q esta guardada

       assertEquals("Córdoba", preguntas.getFirst().getOpciones().get(preguntas.getFirst().getRespuestaCorrecta()));

   }

   @Test 
   public void deberiaConstruirLaPeticionHttpConElModeloYEncabezadosCorrectos() throws Exception {

      ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 1,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");
        
        clienteGemini.generarPreguntas("FACIL", 1);

        ArgumentCaptor<HttpRequest> capturador = ArgumentCaptor.forClass(HttpRequest.class); //Es un capturador para almacenar elHttpRequest utilizado por ClienteGeminiImpl
        
        verify(clienteHttpMock).send(capturador.capture(),ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

        HttpRequest peticion = capturador.getValue(); //Recupera la petición enviada para inspeccionarla

        assertEquals("POST", peticion.method()); // Verifica el método HTTP

        // Verifica el modelo utilizado
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/" + "gemini-3.5-flash-lite:generateContent", peticion.uri().toString());

        assertEquals("api-key-de-prueba", peticion.headers().firstValue("x-goog-api-key").orElse(null));  // Verifica los encabezados

        assertEquals("application/json", peticion.headers().firstValue("Content-Type").orElse(null));

        assertTrue(peticion.bodyPublisher().isPresent());  // Verifica que exista un cuerpo en la petición

        // Método HTTP, Modelo, URL, API Key, content-type, body HTTP.
   }

   @Test 
   public void deberiaLanzarExcepcionCuandoGeminiDevuelveErrorHttp() throws Exception {

     ObjectMapper objectMapper = new ObjectMapper();

    ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

    when(respuestaHttpMock.statusCode()).thenReturn(500); // Simula que Gemini devuelve un error HTTP 500

    // Simula que el cliente HTTP recibe esa respuesta
    when(clienteHttpMock.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

    ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");

    ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> 
                                                  clienteGemini.generarPreguntas("MEDIO", 5));

    assertEquals("Gemini devolvió HTTP 500", excepcion.getMessage());

   }

   @Test 
   public void deberiaLanzarExcepcionCuandoLaApiKeyEsRechazada() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper();

    ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

    when(respuestaHttpMock.statusCode()).thenReturn(403); // Simula que Gemini devuelve un error HTTP 403

    // Simula que el cliente HTTP recibe esa respuesta
    when(clienteHttpMock.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

    ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");

    ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> 
                                                  clienteGemini.generarPreguntas("MEDIO", 5));

    assertEquals("Gemini devolvió HTTP 403", excepcion.getMessage());

   }

   @Test 
   public void deberiaLanzarExcepcionCuandoGeminiSuperaElTiempoDeEspera() throws Exception{

    ObjectMapper objectMapper = new ObjectMapper();

    ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

    // Simula que el cliente HTTP recibe esa respuesta
    when(clienteHttpMock.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new HttpTimeoutException("Tiempo de espera agotado"));

    ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");

    ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> 
                                                  clienteGemini.generarPreguntas("MEDIO", 5));

    assertEquals("Error al comunicarse o procesar la respuesta de Gemini", excepcion.getMessage());
    assertInstanceOf(HttpTimeoutException.class, excepcion.getCause());
   }

   @Test 
   public void deberiaLanzarExcepcionCuandoFallaLaConexion() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper();

    ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

    // Simula que el cliente HTTP recibe esa respuesta
    when(clienteHttpMock.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow( new IOException("Error de conexión"));

    ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");

    ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> 
                                                  clienteGemini.generarPreguntas("MEDIO", 5));

    assertEquals("Error al comunicarse o procesar la respuesta de Gemini", excepcion.getMessage());

    assertInstanceOf(IOException.class, excepcion.getCause()); //Se comprueba que la causa orishinal no se haya perdido

   }

   @Test 
   public void deberiaLanzarExcepcionCuandoLaPeticionEsInterrumpida() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper();

    ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

    // Simula que el cliente HTTP recibe esa respuesta
    when(clienteHttpMock.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow( new InterruptedException("La petición es interrumpida"));

    ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite");

    ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> 
                                                  clienteGemini.generarPreguntas("MEDIO", 5));

    assertEquals("Se interrumpió la comunicación con Gemini", excepcion.getMessage());

    assertInstanceOf(InterruptedException.class, excepcion.getCause());

   }

   @Test 
   public void deberiaLanzarExcepcionCuandoGeminiDevuelveJsonInvalido() throws Exception {

     ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      a
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200);
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini);

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Error al comunicarse o procesar la respuesta de Gemini", excepcion.getMessage());

   }

   @Test 
   public void deberiaLanzarExcepcionCuandoGeminiNoDevuelveCandidates() throws Exception {

      ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["Buenos Aires",
                                  "Córdoba",
                                  "Santa Fe",
                                  "Mendoza"],
                      "respuestaCorrecta": 1,
                      "dificultad": "FACIL"
                    }
                  ]
                }
                """;

         String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "a",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.
        
        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini no devolvió el contenido esperado", excepcion.getMessage());                              
   
      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiNoDevuelveTexto() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      a
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("casa", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200);
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini);

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini no devolvió el contenido esperado", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiNoDevuelvePreguntas() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "mensaje": "No se generaron preguntas"
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200);
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini);

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una cantidad incorrecta de preguntas", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiDevuelvePreguntasNulas() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": null
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200);
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini);

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una cantidad incorrecta de preguntas", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoLaCantidadEsNegativa() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();
        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", -1);});

            assertEquals("La cantidad solicitada de preguntas no es válida", excepcion.getMessage());
            verify(clienteHttpMock, never()).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiDevuelvePreguntasVacias() throws Exception {

         ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                 {
                   "preguntas": []
                  }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200);
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini);

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
                                                        clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una cantidad incorrecta de preguntas", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiDevuelvePreguntasDeMas() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    },
                    {
                      "enunciado": "Pregunta 2",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 1,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); //Cuando alguien consulte el código de estado de esta respuesta simulada, devolve un okey(200)
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); //Indica qué JSON devolverá el servidor simulado.

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar
        
         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> { //Es la exception q se espera 
            clienteGemini.generarPreguntas("DIFICIL", 1);}); //Se comprueba si lanza la exception

        assertEquals("Gemini devolvió una cantidad incorrecta de preguntas", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoLaRespuestaCorrectaEsNegativa() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": -10,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoLasOpcionesTienenValoresNulos() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A",null,"C","D"],
                      "respuestaCorrecta": 3,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoLasOpcionesEstanVacias() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": [],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoUnaOpcionContieneSoloEspacios() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A"," ","C","D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoElEnunciadoContieneSoloEspacios() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    { "enunciado": " ",
                      "opciones": ["A", "B", "C", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test
      public void deberiaLanzarExcepcionCuandoLasOpcionesEstanDuplicadas() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas": [
                    {
                      "enunciado": "Pregunta 1",
                      "opciones": ["A", "B", "B", "D"],
                      "respuestaCorrecta": 0,
                      "dificultad": "DIFICIL"
                    }
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      } 

      @Test 
      public void deberiaLanzarExcepcionCuandoLaPreguntaEsNula() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                {
                  "preguntas":  [
                    null
                  ]
                }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("DIFICIL", 1);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoGeminiDevuelveEnunciadosDuplicados() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                  {
                    "preguntas": [
                      {
                        "enunciado": "¿Cuál es la capital de Argentina?",
                        "opciones": ["Buenos Aires", "Córdoba", "Rosario", "Mendoza"],
                        "respuestaCorrecta": 0,
                        "dificultad": "FACIL"
                      },
                      {
                        "enunciado": "¿Cuál es la capital de Argentina?",
                        "opciones": ["Buenos Aires", "Salta", "Rosario", "Mendoza"],
                        "respuestaCorrecta": 0,
                        "dificultad": "FACIL"
                      }
                    ]
                  }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("FACIL", 2);}); 

        assertEquals("Gemini devolvió preguntas duplicadas", excepcion.getMessage());

      }

      @Test 
      public void deberiaLanzarExcepcionCuandoLasOpcionesEstanDuplicadasConDiferenciasDeFormato() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper(); 
        ServicioGeneradorPromptTriviaImpl generadorPrompt = new ServicioGeneradorPromptTriviaImpl();

        String preguntasJson = """
                  {
                    "preguntas": [
                      {
                        "enunciado": "¿Cuál es la capital de Argentina?",
                        "opciones": ["Buenos Aires", "buenos aires", "Rosario", "Mendoza"],
                        "respuestaCorrecta": 0,
                        "dificultad": "FACIL"
                      },
                      {
                        "enunciado": "¿Cuál es la capital de Buenos Aires?",
                        "opciones": ["La Plata", "CABA", "La Matanza", "Lujan"],
                        "respuestaCorrecta": 0,
                        "dificultad": "FACIL"
                      }
                    ]
                  }
                """;

        // Simula la estructura real de respuesta de cheminai
        String respuestaGemini = objectMapper.writeValueAsString(
                Map.of(
                        "candidates",
                        List.of(
                                Map.of(
                                        "content",
                                        Map.of(
                                                "parts",
                                                List.of(Map.of("text", preguntasJson)))))));

        when(respuestaHttpMock.statusCode()).thenReturn(200); 
        when(respuestaHttpMock.body()).thenReturn(respuestaGemini); 

        when(clienteHttpMock.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock); //Evita la petición real y devuelve nuestra respuesta simulada

        
        ClienteGeminiImpl clienteGemini = new ClienteGeminiImpl(clienteHttpMock, objectMapper, generadorPrompt, "api-key-de-prueba", "gemini-3.5-flash-lite"); //Objeto really q queremos probar

         ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> {  
            clienteGemini.generarPreguntas("FACIL", 2);}); 

        assertEquals("Gemini devolvió una pregunta inválida", excepcion.getMessage());

      }

}
