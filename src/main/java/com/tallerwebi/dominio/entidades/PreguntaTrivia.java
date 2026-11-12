package com.tallerwebi.dominio.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "pregunta_trivia")
public class PreguntaTrivia {

    private static final int CANTIDAD_OPCIONES = 4;
    private static final int LONGITUD_MAXIMA = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = LONGITUD_MAXIMA)
    private String enunciado;

    @Column(nullable = false, length = LONGITUD_MAXIMA)
    private String opcion1;
    @Column(nullable = false, length = LONGITUD_MAXIMA)
    private String opcion2;
    @Column(nullable = false, length = LONGITUD_MAXIMA)
    private String opcion3;
    @Column(nullable = false, length = LONGITUD_MAXIMA)
    private String opcion4;

    @JsonIgnore
    @Column(name = "respuesta_correcta", nullable = false)
    private int respuestaCorrecta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DificultadTrivia dificultad;

    protected PreguntaTrivia() {
    }

    public PreguntaTrivia(String enunciado, List<String> opciones, int respuestaCorrecta,
                          DificultadTrivia dificultad) {
        if (!textoValido(enunciado) || dificultad == null || opciones == null
                || opciones.size() != CANTIDAD_OPCIONES
                || opciones.stream().anyMatch(opcion -> !textoValido(opcion))
                || new HashSet<>(opciones).size() != CANTIDAD_OPCIONES
                || respuestaCorrecta < 0 || respuestaCorrecta >= CANTIDAD_OPCIONES) {
            throw new IllegalArgumentException("Pregunta de trivia inválida");
        }
        this.enunciado = enunciado;
        this.opcion1 = opciones.get(0);
        this.opcion2 = opciones.get(1);
        this.opcion3 = opciones.get(2);
        this.opcion4 = opciones.get(3);
        this.respuestaCorrecta = respuestaCorrecta;
        this.dificultad = dificultad;
    }

    private static boolean textoValido(String texto) {
        return texto != null && !texto.isBlank() && texto.length() <= LONGITUD_MAXIMA;
    }

    public Long getId() {
        return id;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public List<String> getOpciones() {
        return List.of(opcion1, opcion2, opcion3, opcion4);
    }

    public DificultadTrivia getDificultad() {
        return dificultad;
    }

    public boolean esRespuestaCorrecta(int opcionElegida) {
        return respuestaCorrecta == opcionElegida;
    }
}
