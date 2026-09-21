package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPartida repositorioPartida;

  @Autowired
  public ServicioPartidaImpl(RepositorioUsuario repositorioUsuario, RepositorioPartida repositorioPartida) {

    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPartida = repositorioPartida;
  }

  @Override
  public Partida crearPartida(Long id) throws UsuarioNoEncontradoException {
    Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(id);

    if (usuarioEncontrado == null) {
      throw new UsuarioNoEncontradoException();
    }

    String codigoUnico;

    do {
      codigoUnico = generarCodigoUnico();
    } while (this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico) != null);

    Tablero tablero = new Tablero();

    Partida partida = new Partida();
    partida.setTablero(tablero);
    // partida.setTiempoInicio(Instant.now());
    // partida.agregarUsuario(usuarioEncontrado);

    this.repositorioPartida.guardarPartida(partida);

    return partida;
  }

  private String generarCodigoUnico() {
    String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder codigo = new StringBuilder();

    Integer longitudCodigo = 6;
    for (int i = 0; i < longitudCodigo; i++) {
      int posicion = (int) (Math.random() * caracteres.length());
      codigo.append(caracteres.charAt(posicion));
    }

    return codigo.toString();
  }
}
