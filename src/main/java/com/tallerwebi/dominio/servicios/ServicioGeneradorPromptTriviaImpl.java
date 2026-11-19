package com.tallerwebi.dominio.servicios;

import java.util.List;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;

import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

@Service
public class ServicioGeneradorPromptTriviaImpl implements ServicioGeneradorPromptTrivia { //Esta clase construye el prompt con cantidad, dificultad y contexto argentino.

  // En cada pedido se elige al azar un tema y un subtema. Si el prompt es siempre igual,
  // Gemini tiende a devolver las mismas preguntas reescritas con otras palabras.
  static final List<Tema> TEMAS = List.of(
      new Tema("Historia argentina", List.of("pueblos originarios", "época colonial",
          "independencia y próceres", "siglo XIX", "siglo XX", "presidentes históricos")),
      new Tema("Geografía argentina", List.of("provincias y sus capitales", "ríos y lagos",
          "montañas y glaciares", "parques nacionales", "climas y regiones")),
      new Tema("Deportes argentinos", List.of("fútbol de clubes", "Selección Argentina de fútbol", "básquet",
          "tenis", "automovilismo", "boxeo", "hockey y Las Leonas", "rugby y Los Pumas")),
      new Tema("Música argentina", List.of("tango", "folklore", "rock nacional", "cumbia", "cuarteto")),
      new Tema("Cultura popular argentina", List.of("cine argentino", "televisión",
          "historieta (Mafalda, El Eternauta y otras)", "literatura", "humor")),
      new Tema("Costumbres y tradiciones argentinas", List.of("comidas típicas", "bebidas",
          "fiestas populares", "lunfardo y modismos", "juegos tradicionales")),
      new Tema("La Matanza", List.of("localidades y barrios", "rutas y avenidas",
          "historia del partido", "lugares conocidos")));

  private final RandomGenerator azar;

  public ServicioGeneradorPromptTriviaImpl() {
    this(RandomGenerator.getDefault());
  }

  ServicioGeneradorPromptTriviaImpl(RandomGenerator azar) {
    this.azar = azar;
  }

  @Override
  public String generarPromptTrivia(Integer cantidad, String dificultad) {

    Tema tema = TEMAS.get(azar.nextInt(TEMAS.size()));
    String subtema = tema.subtemas().get(azar.nextInt(tema.subtemas().size()));

    String mensaje = """
            Sos un generador de preguntas para MatanzAdbentur, un juego de mesa
            inspirado en Monopoly y ambientado en La Matanza, Buenos Aires, Argentina.

            Generá exactamente %1$d preguntas de trivia de dificultad %2$s.

            TEMA DE ESTA TANDA: %3$s
            SUBTEMA: %4$s
            Todas las preguntas tienen que ser sobre ese tema y ese subtema.
            Si el subtema da para pocas preguntas, podés abrirte a otros aspectos
            del mismo tema, pero no a otros temas.

            EVITÁ LAS PREGUNTAS MÁS TRILLADAS. No preguntes:
            - cuál es la infusión más consumida (el mate),
            - cuál es la cabecera de La Matanza,
            - en qué año se declaró la independencia o se hizo la Revolución de Mayo,
            - cuál es la capital del país o de la provincia de Buenos Aires,
            - quién ganó el Mundial 2022 o en qué deporte se destacó Maradona.
            Buscá datos menos obvios dentro del subtema: personajes secundarios,
            fechas puntuales, lugares, obras, récords, objetos, palabras, costumbres.

            VARIEDAD:
            - Variá el tipo de pregunta: quién, qué, cuándo, dónde, cuál, cuántos.
            - Que no haya dos preguntas sobre el mismo personaje, lugar o hecho.

            NIVELES DE DIFICULTAD:
            - FACIL: algo que sabe la mayoría de la gente en Argentina.
            - MEDIO: algo que sabe alguien con cultura general o interés en el tema.
            - DIFICIL: un dato específico que conoce quien sabe bastante del tema.

            REGLAS:
            1. Cada pregunta debe tener cuatro opciones y una única respuesta correcta.
            2. Las tres opciones incorrectas tienen que ser creíbles pero claramente incorrectas.
            3. Nada de preguntas ambiguas ni de opiniones.
            4. Nada de hechos que cambian con el tiempo (récords vigentes, cargos actuales, poblaciones).
            5. Usá solo información verificable. Si no estás seguro de un dato, cambiá de pregunta.
            6. Distribuí la respuesta correcta entre las posiciones 0, 1, 2 y 3.
            7. Devolvé solo JSON válido, sin texto antes ni después.

            FORMATO JSON:
            {
              "preguntas": [
                {
                  "enunciado": "Texto de la pregunta",
                  "opciones": ["Opción A", "Opción B", "Opción C", "Opción D"],
                  "respuestaCorrecta": 0,
                  "dificultad": "%2$s"
                }
              ]
            }
            """.formatted(cantidad, dificultad, tema.nombre(), subtema); //Reemplaza los marcadores por los parámetros

    return mensaje;
  }

  record Tema(String nombre, List<String> subtemas) {
  }
}
