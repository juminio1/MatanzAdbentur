package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CantidadinsuficienteDeJugadoresException;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoCreadorException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
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

  @ManyToOne
  @JoinColumn(name = "creador_id", nullable = false)
  private Usuario creador; // Un creador puede crear varias partidas pero solamente puede tener una activa

  // a la vez

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
    Integer tamanioMaximo = 4;
    if (this.usuarios.size() < tamanioMaximo && this.estado == EstadoPartida.EN_ESPERA) {
      this.usuarios.add(usuario);
      return true;
    }
    return false;
  }

  public void finalizar() {
    this.estado = EstadoPartida.FINALIZADA;
  }

  public void iniciar(Usuario usuario)
    throws UsuarioNoCreadorException, CantidadinsuficienteDeJugadoresException {
    Integer minimoJugadores = 2;
    Integer maximoJugadores = 4;

    if (this.usuarios.size() < minimoJugadores || this.usuarios.size() > maximoJugadores) {
      throw new CantidadinsuficienteDeJugadoresException(
        "No se puede iniciar la partida con menos de 2 jugadores o más de 4 jugadores."
      );
    }

    if (this.creador == null || !this.creador.equals(usuario)) {
      throw new UsuarioNoCreadorException(
        "Sólo el creador de la partida puede iniciar la partida."
      );
    }

    this.estado = EstadoPartida.EN_CURSO;
    this.tiempoInicio = Instant.now();
  }

  @ElementCollection
  @CollectionTable(name = "partida_fichas", joinColumns = @JoinColumn(name = "partida_id"))
  @MapKeyJoinColumn(name = "usuario_id")
  @Enumerated(EnumType.STRING)
  @Column(name = "ficha")
  private Map<Usuario, Ficha> fichasPorUsuario = new HashMap<>();

  public Map<Usuario, Ficha> getFichasPorUsuario() {
    return fichasPorUsuario;
  }

  public void seleccionarFicha(Usuario usuario, Ficha ficha)
    throws FichaOcupadaException, UsuarioNoEncontradoException {
    if (!this.usuarios.contains(usuario)) {
      throw new UsuarioNoEncontradoException();
    }

    if (
      this.fichasPorUsuario.containsValue(ficha) &&
      !ficha.equals(this.fichasPorUsuario.get(usuario))
    ) {
      throw new FichaOcupadaException();
    }

    this.fichasPorUsuario.put(usuario, ficha);
  }
}
