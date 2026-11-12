package com.tallerwebi.presentacion.mapeadores;

import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import java.util.List;

/** Convierte los datos de la trivia en lo que se envía al navegador. */
public final class MapeadorTrivia {

    private MapeadorTrivia() {
    }

    // La respuesta correcta no se copia: el jugador nunca debe poder verla.
    public static PreguntaTriviaDTO desdePregunta(PreguntaGenerada pregunta, Integer ronda, Long venceEn) {
        if (pregunta == null) {
            throw new IllegalArgumentException("No hay una pregunta para mostrar.");
        }
        List<String> opciones = pregunta.getOpciones() == null ? List.of() : List.copyOf(pregunta.getOpciones());
        return new PreguntaTriviaDTO(ronda, pregunta.getEnunciado(), opciones, pregunta.getDificultad(), venceEn);
    }
}
