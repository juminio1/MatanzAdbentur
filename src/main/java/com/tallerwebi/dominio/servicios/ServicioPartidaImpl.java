package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.infraestructura.RepositorioPartida;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private RepositorioPartida repositorioPartida;

    @Autowired
    public ServicioPartidaImpl(RepositorioPartida repositorioPartida) {
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    @Override
    public void restarDinero(Jugador jugador, Integer monto) {
        jugador.setDinero(jugador.getDinero() - monto);
    }

    @Override
    public void sumarDinero(Jugador jugador, Integer monto) {
        jugador.setDinero(jugador.getDinero() + monto);
    }
}

