package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida{

    private final RepositorioUsuario repositorioUsuario;

    @Autowired
    public ServicioPartidaImpl(RepositorioUsuario repositorioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
    }

    @Override
    public Partida crearPartida(Integer id) {
        Usuario usuarioEncontrado = this.repositorioUsuario.buscarUsuarioPorId(id);



        return null;
    }
}
