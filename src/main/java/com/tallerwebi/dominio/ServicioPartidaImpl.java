package com.tallerwebi.dominio;

import com.tallerwebi.infraestructura.RepositorioPartida;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida{

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPartida repositorioPartida;

    @Autowired
    public ServicioPartidaImpl(RepositorioUsuario repositorioUsuario, RepositorioPartida repositorioPartida) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    public Partida crearPartida(Integer id) {
        Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(id);

        if (usuarioEncontrado == null) {
            return null;
        }

        Tablero tablero = new Tablero();

        Partida partida = new Partida();
        partida.setTablero(tablero);
        partida.setTiempoInicio(Instant.now());
        partida.agregarUsuario(usuarioEncontrado);

        this.repositorioPartida.guardarPartida(partida);

        return partida;
    }


}
