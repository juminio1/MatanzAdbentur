package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.EstadoPartida;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer indiceTurnoActual;
    private String codigoUnico;
    private Instant tiempoInicio;

    @ManyToOne
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    @Enumerated(EnumType.STRING)
    private EstadoPartida estado;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "partida_id")
    private List<Jugador> jugadores;

    @ManyToOne
    @JoinColumn(name = "jugador_turno_id")
    private Jugador jugadorTurnoAleatorio;

    public Partida() {
        this.jugadores = new ArrayList<>();
        this.estado = EstadoPartida.EN_ESPERA;
        this.indiceTurnoActual = 0;
    }

    public Jugador getJugadorTurnoAleatorio() {
        return jugadorTurnoAleatorio;
    }

    public void setJugadorTurnoAleatorio(Jugador jugadorTurnoAleatorio) {
        this.jugadorTurnoAleatorio = jugadorTurnoAleatorio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoUnico() {
        return codigoUnico;
    }

    public void setCodigoUnico(String codigoUnico) {
        this.codigoUnico = codigoUnico;
    }

    public Instant getTiempoInicio() {
        return tiempoInicio;
    }

    public void setTiempoInicio(Instant tiempoInicio) {
        this.tiempoInicio = tiempoInicio;
    }

    public Usuario getCreador() {
        return creador;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartida estado) {
        this.estado = estado;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = jugadores;
    }

    public Integer getIndiceTurnoActual() {
        return indiceTurnoActual;
    }

    public void setIndiceTurnoActual(Integer indiceTurnoActual) {
        this.indiceTurnoActual = indiceTurnoActual;
    }
}