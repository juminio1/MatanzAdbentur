package com.tallerwebi.dominio;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Partida {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Instant tiempoInicio;

  @OneToOne(cascade = CascadeType.PERSIST)
  private Tablero tablero;

  @ManyToMany
  private List<Usuario> usuarios;

  private Boolean activa;
  private String codigoUnico;

  public Partida() {
    this.usuarios = new ArrayList<>();
    this.activa = true;
    this.tiempoInicio = Instant.now();
  }

  public Boolean agregarUsuario(Usuario usuario) {
    if (partidaTieneEspacio()) {
      this.usuarios.add(usuario);
      return true;
    }
    return false;
  }

  private boolean partidaTieneEspacio() {
    Integer maximoUsuarios = 4;
    return this.usuarios.size() < maximoUsuarios;
  }

  public void setTablero(Tablero tablero) {
    this.tablero = tablero;
  }

  public void setTiempoInicio(Instant tiempoInicio) {
    this.tiempoInicio = tiempoInicio;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Boolean getActiva() {
    return activa;
  }

  public void setActiva(Boolean activa) {
    this.activa = activa;
  }

  public void setUsuarios(List<Usuario> usuarios) {
    this.usuarios = usuarios;
  }

  public String getCodigoUnico() {
    return codigoUnico;
  }

  public void setCodigoUnico(String codigoUnico) {
    this.codigoUnico = codigoUnico;
  }

  public List<Usuario> getUsuarios() {
    return usuarios;
  }

  public Tablero getTablero() {
    return tablero;
  }

  public Instant getTiempoInicio() {
    return tiempoInicio;
  }
}
