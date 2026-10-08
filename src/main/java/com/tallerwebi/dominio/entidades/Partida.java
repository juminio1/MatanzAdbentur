package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private final Integer tamanioMaximo = 4;
    private String codigoUnico;
    private Instant tiempoInicio;

    @Transient
    private Map<Usuario, Ficha> fichasSeleccionadas = new HashMap<>();

    @ManyToOne
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    @Enumerated(EnumType.STRING)
    private EstadoPartida estado;

    @ManyToMany
    private List<Usuario> usuarios = new ArrayList<>();

    public Partida() {
        this.estado = EstadoPartida.EN_ESPERA;
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

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public Map<Usuario, Ficha> getFichasSeleccionadas() {
        return fichasSeleccionadas;
    }

    public void setFichasSeleccionadas(Map<Usuario, Ficha> fichasSeleccionadas) {
        this.fichasSeleccionadas = fichasSeleccionadas;
    }

    public Integer getTamanioMaximo() {
        return tamanioMaximo;
    }
}
