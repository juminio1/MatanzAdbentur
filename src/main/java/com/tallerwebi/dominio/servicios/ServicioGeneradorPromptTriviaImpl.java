package com.tallerwebi.dominio.servicios;

import org.springframework.stereotype.Service;

import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

@Service
public class ServicioGeneradorPromptTriviaImpl implements ServicioGeneradorPromptTrivia { //Esta clase se va a encargar de darle un toque de contexto a la cheminai para generar las preguntas

  @Override
  public String generarPromptTrivia(Integer cantidad, String dificultad) {

    String mensaje = """
            Sos un generador de preguntas para MatanzAdbentur,
            un juego de mesa inspirado en Monopoly y ambientado
            en La Matanza, Buenos Aires, Argentina.

            Generá exactamente %d preguntas de trivia sobre Argentina
            de dificultad %s.

            TEMATICAS:
            - Historia argentina.
            - Geografia argentina.
            - Deportes argentinos.
            - Musica y cultura popular.
            - Costumbres y tradiciones argentinas.
            - Lugares y curiosidades de La Matanza.

            NIVELES DE DIFICULTAD:
            - FACIL: preguntas sobre conocimientos populares.
            - MEDIO: preguntas de cultura general intermedia.
            - DIFICIL: preguntas especificas que requieren mayor conocimiento.

            REGLAS:
            1. Generá exactamente la cantidad de preguntas solicitada.
            2. Cada pregunta debe tener cuatro opciones.
            3. Debe existir una única respuesta correcta.
            4. Las otras tres opciones deben ser incorrectas.
            5. No generes preguntas ambiguas.
            6. Verificá que ninguna otra opción pueda considerarse correcta.
            7. Evitá preguntas sobre hechos que cambian con el tiempo.
            8. No repitas preguntas dentro de la misma respuesta.
            9. Distribuí las respuestas correctas entre distintas posiciones.
            10. Utilizá información verificable.
            11. Si una pregunta incumple estas reglas, reemplazala.
            12. Devolvé exclusivamente JSON válido, sin explicaciones.

            FORMATO JSON:
            {
              "preguntas": [
                {
                  "enunciado": "Texto de la pregunta",
                  "opciones": [
                    "Opcion A",
                    "Opcion B",
                    "Opcion C",
                    "Opcion D"
                  ],
                  "respuestaCorrecta": 0,
                  "dificultad": "FACIL"
                }
              ]
            }

            IMPORTANTE:
            - respuestaCorrecta debe ser un número entre 0 y 3.
            - El índice debe señalar la única opción correcta.
            - Todas las preguntas deben tener la dificultad solicitada.
            - No agregues texto fuera del JSON.
            """.formatted(cantidad, dificultad); //Se utiliza para reemplazar los marcadores %d y %s por los parametros

    return mensaje;
}
}
