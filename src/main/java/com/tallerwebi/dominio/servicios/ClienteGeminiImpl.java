package com.tallerwebi.dominio.servicios;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

public class ClienteGeminiImpl { //Esta clase se encarga de realizar la petición HTTP e interpreta el JSON

    private final HttpClient clienteHttp;
    private final ObjectMapper objectMapper;
    private final ServicioGeneradorPromptTrivia generadorPrompt;
    private final String apiKey;
    private final String modelo;

    private static final Integer CANTIDAD_MINIMA = 0;

    public ClienteGeminiImpl(HttpClient clienteHttp, ObjectMapper objectMapper,
            ServicioGeneradorPromptTrivia  generadorPrompt, String apiKey, String modelo) {
      
        this.clienteHttp = clienteHttp;
        this.objectMapper = objectMapper;
        this.generadorPrompt = generadorPrompt;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    
public List<PreguntaGenerada> generarPreguntas(String dificultad, Integer cantidad) {

        if (dificultad == null || dificultad.isBlank() || (!dificultad.equals("FACIL")
                && !dificultad.equals("MEDIO") && !dificultad.equals("DIFICIL"))) {

        throw new ClienteIAException("La dificultad solicitada no es válida"); //Se comprueba antes de enviar la solicitud http 
        }

        if (cantidad == null || cantidad <= CANTIDAD_MINIMA) {
         throw new ClienteIAException("La cantidad solicitada de preguntas no es válida");
        }

    try {
        
        String prompt = generadorPrompt.generarPromptTrivia(cantidad, dificultad);

        Map<String, Object> cuerpo = Map.of( //Es el cuerpo de la solicitud JSON
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"));

        String cuerpoJson = objectMapper.writeValueAsString(cuerpo);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":generateContent"; //Se prepara la petición a la cheminai

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(solicitud, HttpResponse.BodyHandlers.ofString()); //Envía la petición y recibe una respuesta

        if (respuesta.statusCode() != 200) {
            throw new ClienteIAException("Gemini devolvió HTTP " + respuesta.statusCode());
        }

        JsonNode respuestaJson = objectMapper.readTree(respuesta.body()); //Se extrae el texto generado por cheminai

        JsonNode texto = respuestaJson.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (!texto.isTextual()) {
            throw new ClienteIAException("Gemini no devolvió el contenido esperado");
        }

        JsonNode preguntasJson = objectMapper.readTree(texto.asText()).path("preguntas"); //Se convierte el json generado en un objeto pregunta de java

        if (!preguntasJson.isArray() || preguntasJson.size() != cantidad ) {
            throw new ClienteIAException("Gemini devolvió una cantidad incorrecta de preguntas");
        }

        List<PreguntaGenerada> preguntas = new ArrayList<>();

       for (JsonNode preguntaJson : preguntasJson) {

        if (preguntaJson.isNull()) {
            throw new ClienteIAException("Gemini devolvió una pregunta inválida");
        }

        PreguntaGenerada pregunta = objectMapper.treeToValue(
                preguntaJson, PreguntaGenerada.class);

        if (pregunta == null) {
            throw new ClienteIAException( "Gemini devolvió una pregunta inválida");
        }

            if (pregunta.getEnunciado() == null //Validaciones de la pregunta
                    || pregunta.getEnunciado().isBlank()
                    || pregunta.getOpciones() == null
                    || pregunta.getOpciones().size() != 4
                    || pregunta.getOpciones().stream().map(opcion -> opcion.trim().toLowerCase(Locale.ROOT)).distinct().count() != 4
                    || !dificultad.equals(pregunta.getDificultad())
                    || pregunta.getOpciones().stream().anyMatch(
                            opcion -> opcion == null || opcion.isBlank())
                    || pregunta.getRespuestaCorrecta() == null
                    || pregunta.getRespuestaCorrecta() < 0
                    || pregunta.getRespuestaCorrecta() > 3) {
                
                throw new ClienteIAException("Gemini devolvió una pregunta inválida");
                }
                preguntas.add(pregunta);
            }

            long cantidadEnunciadosDistintos = preguntas.stream().map(PreguntaGenerada::getEnunciado).map(enunciado -> enunciado.trim().toLowerCase(java.util.Locale.ROOT)).distinct().count();

                if (cantidadEnunciadosDistintos != preguntas.size()) {
                 throw new ClienteIAException("Gemini devolvió preguntas duplicadas");
                }

        return preguntas;

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ClienteIAException("Se interrumpió la comunicación con Gemini", e);

    } catch (IOException e) {
        throw new ClienteIAException("Error al comunicarse o procesar la respuesta de Gemini", e);
    }
}


}
