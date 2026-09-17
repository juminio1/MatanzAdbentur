package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontrado;
import com.tallerwebi.infraestructura.RepositorioPartida;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

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
  public Partida crearPartida(Integer id) throws UsuarioNoEncontrado {
    Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(id);

    if (usuarioEncontrado == null) {
      throw new UsuarioNoEncontrado();
    }

    String codigoUnico;

    do {
      codigoUnico = generarCodigoUnico();
    } while (this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico) != null);

    Tablero tablero = new Tablero();

    Partida partida = new Partida();
    partida.setCodigoUnico(codigoUnico);
    partida.setTablero(tablero);
    partida.agregarUsuario(usuarioEncontrado);

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
