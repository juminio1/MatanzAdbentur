package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "partida_pregunta_trivia_usada", uniqueConstraints =
    @UniqueConstraint(columnNames = {"partida_id", "huella_enunciado"}))
public class PreguntaTriviaUsada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(name = "huella_enunciado", nullable = false)
    private UUID huellaEnunciado;

    protected PreguntaTriviaUsada() {
    }

    public PreguntaTriviaUsada(Partida partida, UUID huellaEnunciado) {
        this.partida = partida;
        this.huellaEnunciado = huellaEnunciado;
    }
}
