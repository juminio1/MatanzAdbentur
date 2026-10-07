package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
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

    private String codigoUnico;
    private Instant tiempoInicio;

    @Transient
    private Map<Usuario, Ficha> fichasSeleccionadas;

    @ManyToOne
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador; // Un creador puede crear varias partidas pero solamente puede tener una activa

    // a la vez

    @Enumerated(EnumType.STRING)
    private EstadoPartida estado;

    //@OneToOne(cascade = CascadeType.PERSIST)
    //private Tablero tablero;

    @ManyToMany
    private List<Usuario> usuarios;

    public Partida() {
        this.usuarios = new ArrayList<>();
        this.fichasSeleccionadas = new HashMap<>();
        this.estado = EstadoPartida.EN_ESPERA; // Nace en sala de espera / lobby
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

    public Boolean agregarUsuario(Usuario usuario) {
        Integer tamanioMaximo = 4;
        if (this.usuarios.size() < tamanioMaximo && this.estado == EstadoPartida.EN_ESPERA) {
            this.usuarios.add(usuario);
            return true;
        }
        return false;
    }

    public void seleccionarFicha(Usuario usuario, Ficha ficha) throws FichaOcupadaException {
        if (!this.usuarios.contains(usuario)) {
            throw new IllegalArgumentException("El usuario no pertenece a la partida.");
        }

        if (this.fichasSeleccionadas.containsValue(ficha)) {
            throw new FichaOcupadaException(
                "La ficha " + ficha + " ya está ocupada por otro jugador."
            );
        }
        this.fichasSeleccionadas.put(usuario, ficha);
    }

    public void abandonarSala(Usuario usuario) {
        if (this.usuarios.contains(usuario)) {
            this.usuarios.remove(usuario);
            this.fichasSeleccionadas.remove(usuario); // Libera la ficha automáticamente
        }
    }

    public Ficha getFichaSeleccionada(Usuario usuario) {
        return this.fichasSeleccionadas.get(usuario);
    }
}
