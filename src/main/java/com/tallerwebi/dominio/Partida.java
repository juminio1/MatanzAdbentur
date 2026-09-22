package com.tallerwebi.dominio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Partida {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String codigoUnico;
  private Instant tiempoInicio;

  @ManyToOne
  @JoinColumn(name = "creador_id", nullable = false)
  private Usuario creador; //Un creador puede crear varias partidas pero solamente puede tener una activa a la vez

  @Enumerated(EnumType.STRING)
  private EstadoPartida estado;

  @OneToOne(cascade = CascadeType.PERSIST)
  private Tablero tablero;

  @ManyToMany
  private List<Usuario> usuarios;

  public Partida() {
    this.usuarios = new ArrayList<>();
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

  public Tablero getTablero() {
    return tablero;
  }

  public void setTablero(Tablero tablero) {
    this.tablero = tablero;
  }

  public List<Usuario> getUsuarios() {
    return usuarios;
  }

  public void setUsuarios(List<Usuario> usuarios) {
    this.usuarios = usuarios;
  }

  public Boolean agregarUsuario(Usuario usuario) {
    if (this.usuarios.size() < 4 && this.estado == EstadoPartida.EN_ESPERA) {
      this.usuarios.add(usuario);
      if (this.usuarios.size() == 4) {
        this.estado = EstadoPartida.EN_CURSO; // Se llenó el cupo: arranca el juego
      }
      return true;
    }
    return false;
  }

  public void finalizar() {
    this.estado = EstadoPartida.FINALIZADA;
  }
}