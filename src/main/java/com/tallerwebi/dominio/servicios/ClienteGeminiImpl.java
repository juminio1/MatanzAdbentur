
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
    private static final Integer CANTIDAD_MAXIMA_OPCIONES = 4;
   private static final Integer INDICE_MAXIMO_RESPUESTA_CORRECTA = 3;
    private static final Integer SEGUNDOS = 15;
    private static final Integer HTTP_OK = 200;

    private static final String DIFICULTAD_FACIL = "FACIL";
    private static final String DIFICULTAD_MEDIO = "MEDIO";
    private static final String DIFICULTAD_DIFICIL = "DIFICIL";
    private static final String MENSAJE_PREGUNTA_INVALIDA = "Gemini devolvió una pregunta inválida";

   
    
    
    public ClienteGeminiImpl(HttpClient clienteHttp, ObjectMapper objectMapper,
            ServicioGeneradorPromptTrivia generadorPrompt, String apiKey, String modelo) {

        this.clienteHttp = clienteHttp;
        this.objectMapper = objectMapper;
        this.generadorPrompt = generadorPrompt;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    public List<PreguntaGenerada> generarPreguntas(String dificultad, Integer cantidad) {

        this.validarAntesDeLaSolicitud(dificultad, cantidad);

        try {

            HttpResponse<String> respuesta = this.construirSolicitudHTTP(dificultad, cantidad);

            this.validarRespuestaHTTP(respuesta);

            JsonNode texto = this.extraerTextoGenerado(respuesta);

            JsonNode preguntasJson = this.extraerPreguntasJson(texto, cantidad);

            List<PreguntaGenerada> preguntas = this.convertirPreguntas(preguntasJson, dificultad);

            this.validarEnunciadosDuplicados(preguntas);

            return preguntas;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClienteIAException("Se interrumpió la comunicación con Gemini", e);

        } catch (IOException e) {
            throw new ClienteIAException("Error al comunicarse o procesar la respuesta de Gemini", e);
        }
    }


    private void validarAntesDeLaSolicitud(String dificultad, Integer cantidad) {

        if (dificultad == null || dificultad.isBlank()
                || (!DIFICULTAD_FACIL.equals(dificultad)
                && !DIFICULTAD_MEDIO.equals(dificultad)
                && !DIFICULTAD_DIFICIL.equals(dificultad))) {

            throw new ClienteIAException("La dificultad solicitada no es válida");  //Se comprueba antes de enviar la solicitud http
        } 
        

        if (cantidad == null || cantidad <= CANTIDAD_MINIMA) {
            throw new ClienteIAException("La cantidad solicitada de preguntas no es válida");
        }
    }


    private HttpResponse<String> construirSolicitudHTTP(String dificultad, Integer cantidad)
            throws IOException, InterruptedException {

        String prompt = generadorPrompt.generarPromptTrivia(cantidad, dificultad);

        Map<String, Object> cuerpo = Map.of( //Es el cuerpo de la solicitud JSON
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"));

        String cuerpoJson = objectMapper.writeValueAsString(cuerpo);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelo + ":generateContent"; //Se prepara la petición a la cheminai

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(SEGUNDOS))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(solicitud, HttpResponse.BodyHandlers.ofString()); //Envía la petición y recibe una respuesta

        return respuesta;
    }


    private void validarRespuestaHTTP(HttpResponse<String> respuesta) {

        if (respuesta.statusCode() != HTTP_OK) {
            throw new ClienteIAException("Gemini devolvió HTTP " + respuesta.statusCode());
        }
    }


    private JsonNode extraerTextoGenerado(HttpResponse<String> respuesta) throws IOException {

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

        return texto;
    }


    private JsonNode extraerPreguntasJson(JsonNode texto, Integer cantidad) throws IOException {

        JsonNode preguntasJson = objectMapper.readTree(texto.asText()).path("preguntas"); //Se convierte el json generado en un objeto pregunta de java

        if (!preguntasJson.isArray() || preguntasJson.size() != cantidad) {
            throw new ClienteIAException("Gemini devolvió una cantidad incorrecta de preguntas");
        }

        return preguntasJson;
    }


    private List<PreguntaGenerada> convertirPreguntas(JsonNode preguntasJson, String dificultad)
            throws IOException {

        List<PreguntaGenerada> preguntas = new ArrayList<>();

        for (JsonNode preguntaJson : preguntasJson) {

            if (preguntaJson.isNull()) {
                throw new ClienteIAException("Gemini devolvió una pregunta inválida");
            }

            PreguntaGenerada pregunta = objectMapper.treeToValue(
                    preguntaJson, PreguntaGenerada.class);

            this.validarPregunta(pregunta, dificultad);

            preguntas.add(pregunta);
        }

        return preguntas;
    }


   private void validarPregunta(PreguntaGenerada pregunta, String dificultad) {

        if (pregunta == null) {
            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }

        this.validarEnunciado(pregunta.getEnunciado());

        this.validarOpcionesDePregunta(pregunta.getOpciones());

        this.validarRespuestaCorrecta(pregunta.getRespuestaCorrecta());

        if (!dificultad.equals(pregunta.getDificultad())) {
            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }
    }

    private void validarEnunciado(String enunciado) {

        if (enunciado == null || enunciado.isBlank()) {
            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }
    }

    private void validarOpcionesDePregunta(List<String> opciones) {

        if (opciones == null || opciones.size() != CANTIDAD_MAXIMA_OPCIONES) {
            throw new ClienteIAException("Gemini devolvió una pregunta inválida");
        }

        this.validarOpciones(opciones);
    }

    private void validarRespuestaCorrecta(Integer respuestaCorrecta) {

        if (respuestaCorrecta == null
                || respuestaCorrecta < CANTIDAD_MINIMA
                || respuestaCorrecta > INDICE_MAXIMO_RESPUESTA_CORRECTA) {

            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }
    }

    private void validarOpciones(List<String> opciones) {

        if (opciones.stream().anyMatch(
                opcion -> opcion == null || opcion.isBlank())) {

            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }

        long cantidadOpcionesDistintas = opciones.stream()
                .map(opcion -> opcion.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        if (cantidadOpcionesDistintas != opciones.size()) {
            throw new ClienteIAException(MENSAJE_PREGUNTA_INVALIDA);
        }
    }


    private void validarEnunciadosDuplicados(List<PreguntaGenerada> preguntas) {

        long cantidadEnunciadosDistintos = preguntas.stream()
                .map(PreguntaGenerada::getEnunciado)
                .map(enunciado -> enunciado.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        if (cantidadEnunciadosDistintos != preguntas.size()) {
            throw new ClienteIAException("Gemini devolvió preguntas duplicadas");
        }
    }

}
