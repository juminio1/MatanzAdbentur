package com.tallerwebi.dominio.servicios;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;

import jakarta.transaction.Transactional;

@Service("servicioSalaDeEspera")
@Transactional
public class ServicioSalaDeEsperaImpl implements ServicioSalaDeEspera {

  private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LONGITUD_CODIGO = 6;
  private final Random random = new SecureRandom();

  private RepositorioUsuario repositorioUsuario;
  private RepositorioPartida repositorioPartida;

  @Autowired
  public ServicioSalaDeEsperaImpl(
      RepositorioUsuario repositorioUsuario,
      RepositorioPartida repositorioPartida) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPartida = repositorioPartida;
  }

  @Override
  public Partida crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException {
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
   // partida.setTablero(new Tablero());
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
  public Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico)
      throws UsuarioNoEncontradoException, PartidaNoEncontradaException {
    throw new UnsupportedOperationException("Unimplemented method 'unirseASalaDeEspera'");
  }
}
