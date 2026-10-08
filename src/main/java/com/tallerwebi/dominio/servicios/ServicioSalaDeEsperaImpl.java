package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioSalaDeEspera")
@Transactional
public class ServicioSalaDeEsperaImpl implements ServicioSalaDeEspera {

    private static final String MSG_USUARIO_NO_EXISTE = "El usuario no existe.";
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_CODIGO = 6;
    private final Random random = new SecureRandom();

    private RepositorioUsuario repositorioUsuario;
    private RepositorioPartida repositorioPartida;

    @Autowired
    public ServicioSalaDeEsperaImpl(
        RepositorioUsuario repositorioUsuario,
        RepositorioPartida repositorioPartida
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    public Partida crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);

        if (usuarioEncontrado == null) {
            throw new UsuarioNoEncontradoException(MSG_USUARIO_NO_EXISTE);
        }

        String codigoUnico;
        do {
            codigoUnico = generarCodigoUnico();
        } while (this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico) != null);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigoUnico);
        partida.setCreador(usuarioEncontrado);
        partida.setTiempoInicio(Instant.now());

        // Agregamos al creador a la lista de usuarios
        partida.getUsuarios().add(usuarioEncontrado);

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
        Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException(MSG_USUARIO_NO_EXISTE);
        }

        Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (partida == null) {
            throw new PartidaNoEncontradaException(
                "No se encontró una partida activa con el código: " + codigoUnico
            );
        }

        // Validaciones de negocio en el servicio
        if (
            partida.getUsuarios().size() >= partida.getTamanioMaximo() ||
            partida.getEstado() != EstadoPartida.EN_ESPERA
        ) {
            throw new IllegalStateException("La partida está llena o ya ha comenzado.");
        }

        if (!partida.getUsuarios().contains(usuario)) {
            partida.getUsuarios().add(usuario);
            this.repositorioPartida.guardarPartida(partida);
        }

        return partida;
    }

    @Override
    public void seleccionarFicha(String codigoUnico, Long idUsuario, Ficha ficha)
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException, FichaOcupadaException {
        Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (partida == null) {
            throw new PartidaNoEncontradaException(
                "No se encontró una partida activa con el código: " + codigoUnico
            );
        }

        Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException(MSG_USUARIO_NO_EXISTE);
        }

        // Validaciones de fichas en el servicio
        if (!partida.getUsuarios().contains(usuario)) {
            throw new UsuarioNoEncontradoException("El usuario no pertenece a la partida.");
        }

        if (partida.getFichasSeleccionadas().containsValue(ficha)) {
            throw new FichaOcupadaException(
                "La ficha " + ficha + " ya está ocupada por otro jugador."
            );
        }

        partida.getFichasSeleccionadas().put(usuario, ficha);
        this.repositorioPartida.guardarPartida(partida);
    }

    @Override
    public void abandonarSala(String codigoUnico, Long idUsuario)
        throws UsuarioNoEncontradoException, PartidaNoEncontradaException {
        Partida partida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (partida == null) {
            throw new PartidaNoEncontradaException(
                "No se encontró una partida activa con el código: " + codigoUnico
            );
        }

        Usuario usuario = this.repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException(MSG_USUARIO_NO_EXISTE);
        }

        // Lógica de abandono en el servicio
        if (partida.getUsuarios().contains(usuario)) {
            partida.getUsuarios().remove(usuario);
            partida.getFichasSeleccionadas().remove(usuario);
        }

        this.repositorioPartida.guardarPartida(partida);
    }
}
