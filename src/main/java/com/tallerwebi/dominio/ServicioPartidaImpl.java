package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
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
  public ServicioPartidaImpl(RepositorioUsuario repositorioUsuario, RepositorioPartida repositorioPartida) {
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
}