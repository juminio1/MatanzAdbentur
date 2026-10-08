package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioSalaDeEspera")
@Transactional
public class ServicioSalaDeEsperaImpl implements ServicioSalaDeEspera {

    private static final int LIMITE_JUGADORES = 4;

    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final RepositorioUsuario repositorioUsuario;

    @Autowired
    public ServicioSalaDeEsperaImpl(
        RepositorioSalaDeEspera repositorioSalaDeEspera,
        RepositorioUsuario repositorioUsuario
    ) {
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.repositorioUsuario = repositorioUsuario;
    }

    @Override
    public SalaDeEspera crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario creador = repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (creador == null) {
            throw new UsuarioNoEncontradoException();
        }

        SalaDeEspera nuevaSala = new SalaDeEspera();
        nuevaSala.setCodigoUnico(generarCodigoUnico());
        nuevaSala.setCreador(creador);
        nuevaSala.getUsuarios().add(creador);

        repositorioSalaDeEspera.guardar(nuevaSala);
        return nuevaSala;
    }

    @Override
    public SalaDeEspera unirseASalaDeEspera(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException {
        Usuario usuario = repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException();
        }

        SalaDeEspera sala = obtenerSalaPorCodigo(codigoUnico);

        if (sala.getUsuarios().size() >= LIMITE_JUGADORES) {
            throw new SalaDeEsperaLlenaException();
        }

        boolean yaEstaEnSala = sala
            .getUsuarios()
            .stream()
            .anyMatch(u -> u.getId() != null && u.getId().equals(usuario.getId()));

        if (!yaEstaEnSala) {
            sala.getUsuarios().add(usuario);
            repositorioSalaDeEspera.modificar(sala);
        }

        return sala;
    }

    @Override
    public void abandonarSala(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException {
        Usuario usuario = repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException();
        }

        SalaDeEspera sala = obtenerSalaPorCodigo(codigoUnico);

        boolean removido = sala
            .getUsuarios()
            .removeIf(u -> u.getId() != null && u.getId().equals(usuario.getId()));

        if (removido) {
            repositorioSalaDeEspera.modificar(sala);
        }
    }

    @Override
    public SalaDeEspera obtenerSalaPorCodigo(String codigoUnico) throws SalaNoEncontradaException {
        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigoUnico);
        if (sala == null) {
            throw new SalaNoEncontradaException();
        }
        return sala;
    }

    private String generarCodigoUnico() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
    }
}
