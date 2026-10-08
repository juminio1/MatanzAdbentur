package com.tallerwebi.dominio.servicios;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.excepcion.UsuarioYaEstaEnPartidaException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;
import com.tallerwebi.presentacion.DTO.SalaActualizadaDTO;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;

import jakarta.transaction.Transactional;

@Service("servicioSalaDeEspera")
@Transactional
public class ServicioSalaDeEsperaImpl implements ServicioSalaDeEspera {

  private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LONGITUD_CODIGO = 6;
  private final Random random = new SecureRandom();

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPartida repositorioPartida;
  private final NotificadorSala notificadorSala;

  @Autowired
  public ServicioSalaDeEsperaImpl(
      RepositorioUsuario repositorioUsuario,
      RepositorioPartida repositorioPartida, NotificadorSala notificadorSala) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPartida = repositorioPartida;
    this.notificadorSala = notificadorSala;
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
public Partida unirseASalaDeEspera(Long idUsuario, String codigoUnico, SalaDeEspera sala)
    throws UsuarioNoEncontradoException, PartidaNoEncontradaException,
           SalaDeEsperaLlenaException, PartidaIniciadaException,
           UsuarioYaEstaEnPartidaException {

    Usuario usuarioEncontrado = obtenerUsuarioValido(idUsuario);
    Partida partidaEncontrada = obtenerPartidaValida(codigoUnico);

    validarEstadoPartida(partidaEncontrada, usuarioEncontrado);

    if (!partidaEncontrada.agregarUsuario(usuarioEncontrado)) {
        throw new SalaDeEsperaLlenaException();
    }

    this.repositorioPartida.guardarPartida(partidaEncontrada);
    SalaActualizadaDTO salaActualizada = obtenerEstadoSala(sala);

    this.notificarSalaActualizada(salaActualizada);

    return partidaEncontrada;
}

private Usuario obtenerUsuarioValido(Long idUsuario) throws UsuarioNoEncontradoException {
    Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
    if (usuario == null) {
        throw new UsuarioNoEncontradoException();
    }
    return usuario;
}

private Partida obtenerPartidaValida(String codigoUnico) throws PartidaNoEncontradaException {
    Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    if (partida == null) {
        throw new PartidaNoEncontradaException();
    }
    return partida;
}

private void validarEstadoPartida(Partida partida, Usuario usuario)
    throws PartidaIniciadaException, UsuarioYaEstaEnPartidaException {

    if (partida.getEstado() == com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO) {
        throw new PartidaIniciadaException();
    }
    if (partida.getUsuarios().contains(usuario)) {
        throw new UsuarioYaEstaEnPartidaException();
    }
}

@Override
public SalaActualizadaDTO obtenerEstadoSala(SalaDeEspera salaDeEspera) {
  
for (Usuario usuario : salaDeEspera.getUsuarios()) { // Se busca que los usuarios existan
    if (usuario == null) { 
        throw new UsuarioNoEncontradoException(); 
    }
}

List<String> usernames = salaDeEspera.getUsuarios().stream().map(Usuario::getUsername).toList(); //Obtengo los usernames

return new SalaActualizadaDTO(salaDeEspera.getCodigoGenerado(),usernames); //Armo el DTO
}

public void notificarSalaActualizada( SalaActualizadaDTO sala) {

    this.notificadorSala.notificarSalaActualizada(sala);

}
}