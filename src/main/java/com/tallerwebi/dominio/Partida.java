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

  @ManyToOne
  @JoinColumn(name = "creador_id", nullable = false)//Un creador puede crear varias partidas pero solamente puede tener una activa a la vez
  private Usuario Creador;
  private Instant tiempoInicio;

  @OneToOne(cascade = CascadeType.PERSIST)
  private Tablero tablero;

  @ManyToMany
  private List<Usuario> usuarios;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Instant getTiempoInicio() {
    return tiempoInicio;
  }

  public Tablero getTablero() {
    return tablero;
  }

  public List<Usuario> getUsuarios() {
    return usuarios;
  }

  public void setUsuarios(List<Usuario> usuarios) {
    this.usuarios = usuarios;
  }

  public Boolean getActiva() {
    return activa;
  }

  public void setActiva(Boolean activa) {
    this.activa = activa;
  }

  private Boolean activa;

  public Partida() {
    this.usuarios = new ArrayList<>();
    this.activa = true;
  }

  public Boolean agregarUsuario(Usuario usuario) {
    Integer limiteTamanio = 4;

    if (this.usuarios.size() < limiteTamanio) {
      this.usuarios.add(usuario);
      return true;
    }
    return false;
  }

  public void setTablero(Tablero tablero) {
    this.tablero = tablero;
  }

  public void setTiempoInicio(Instant tiempoInicio) {
    this.tiempoInicio = tiempoInicio;
  }

  public void finalizar() {
    this.activa = false;
  }

  public String getCodigoUnico() {
    return codigoUnico;
  }

  public void setCodigoUnico(String codigoUnico) {
    this.codigoUnico = codigoUnico;
  }

  public Usuario getCreador() {
    return Creador;
  }

  public void setCreador(Usuario creador) {
    Creador = creador;
  }
}
