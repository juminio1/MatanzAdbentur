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

    @Override
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        
        if (partida != null && partida.getJugadores() != null) {
            int orden = 0;
            
            for (Jugador jugador : partida.getJugadores()) {
                jugador.setOrden(orden);
                jugador.setPierdeTurno(false);
                orden++;
            }
            
            partida.setIndiceTurnoActual(0); 
        }
    }

    @Override
    public Jugador obtenerJugadorActual(String codigoUnico) {
       Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        
        if (partida != null && partida.getJugadores() != null) {
           
            for (Jugador jugador : partida.getJugadores()) {
                if (jugador.getOrden().equals(partida.getIndiceTurnoActual())) {
                    return jugador;
                }
            }
        }
        return null;
    }

    @Override
    public void avanzarTurno(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        
        if (partida != null && partida.getJugadores() != null && !partida.getJugadores().isEmpty()) {
            int cantidadJugadores = partida.getJugadores().size();
            
            int siguienteIndice = (partida.getIndiceTurnoActual() + 1) % cantidadJugadores;
            partida.setIndiceTurnoActual(siguienteIndice);
            
            Jugador jugadorSiguiente = obtenerJugadorActual(codigoUnico);
            
            if (jugadorSiguiente != null && jugadorSiguiente.getPierdeTurno()) {
                jugadorSiguiente.setPierdeTurno(false);
                avanzarTurno(codigoUnico); 
            }
        }
    }
}

