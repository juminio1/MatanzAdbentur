package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CantidadinsuficienteDeJugadoresException;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoCreadorException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;

import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

  private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LONGITUD_CODIGO = 6;
  private final Random random = new SecureRandom();

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPartida repositorioPartida;

  @Autowired
  public ServicioPartidaImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPartida repositorioPartida
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPartida = repositorioPartida;
  }

  @Override
  public Partida crearPartida(Long idUsuario) throws UsuarioNoEncontradoException {
    Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(idUsuario); 

    if (usuarioEncontrado == null) {
      throw new UsuarioNoEncontradoException();
    }

    String codigoUnico;
    do {
      codigoUnico = generarCodigoUnico();
    } while (this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico) != null);

    Partida partida = new Partida();
    partida.setCodigoUnico(codigoUnico);
    partida.setCreador(usuarioEncontrado);
    partida.setTablero(new Tablero());
    partida.setTiempoInicio(Instant.now());
    partida.agregarUsuario(usuarioEncontrado);

    this.repositorioPartida.guardarPartida(partida);

    return partida;
  }

  private String generarCodigoUnico() {
    StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);
    for (int i = 0; i < LONGITUD_CODIGO; i++) {
      int posicion = this.random.nextInt(CARACTERES.length());
      codigo.append(CARACTERES.charAt(posicion));
    }
    return codigo.toString();
  }

  @Override
  public Partida unirseAPartida(Long idUsuario, String codigoUnico)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException {
    throw new UnsupportedOperationException("Unimplemented method 'unirseAPartida'");
  }

  @Override
  public void iniciarPartida(Long idUsuario, String codigoUnico)
    throws UsuarioNoCreadorException, CantidadinsuficienteDeJugadoresException, PartidaNoEncontradaException {
    Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
    if (usuario == null) {
      throw new UsuarioNoEncontradoException();
    }

    Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    if (partida == null) {
      throw new PartidaNoEncontradaException("No se encontró una partida con el código solicitado");
    }

    partida.iniciar(usuario);
    this.repositorioPartida.guardarPartida(partida);
  }

  @Override
  public void seleccionarFicha(Long idUsuario, String codigoUnico, Ficha ficha)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException, FichaOcupadaException {
    Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
    if (usuario == null) {
      throw new UsuarioNoEncontradoException();
    }

    Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    if (partida == null) {
      throw new PartidaNoEncontradaException("Partida no encontrada");
    }

    partida.seleccionarFicha(usuario, ficha);
    this.repositorioPartida.guardarPartida(partida);
  }
}
