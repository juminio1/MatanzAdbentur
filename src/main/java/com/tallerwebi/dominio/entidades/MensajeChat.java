package com.tallerwebi.dominio.entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "mensaje_chat", indexes = @Index(name = "idx_chat_partida", columnList = "partida_id,id"))
public class MensajeChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;
    @ManyToOne(optional = false)
    private Jugador autor;
    @Column(nullable = false, length = 500)
    private String texto;
    @Column(nullable = false)
    private Long fecha;
    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida valor) { partida = valor; }
    public Jugador getAutor() { return autor; }
    public void setAutor(Jugador valor) { autor = valor; }
    public String getTexto() { return texto; }
    public void setTexto(String valor) { texto = valor; }
    public Long getFecha() { return fecha; }
    public void setFecha(Long valor) { fecha = valor; }
}
