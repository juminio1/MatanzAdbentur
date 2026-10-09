package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private RepositorioPartida repositorioPartida;
    private final Integer CANTIDAD_ESTABLECIDA_JUGADORES = 4;
    private final Integer CANTIDAD_INICIAL_DINERO = 150000;
    private final Integer POSICION_INICIAL = 0;
    private final Random random = new Random();

    @Autowired
    public ServicioPartidaImpl(RepositorioPartida repositorioPartida) {
        this.repositorioPartida = repositorioPartida;
        
    }

    @Override
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    @Override
    public void iniciarPartida()
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Partida partidaEncontrada = this.repositorioPartida.buscarPartidaActiva();

        if (partidaEncontrada == null) {
            throw new PartidaNoEncontradaException("Partida no encontrada");
        }

        if (partidaEncontrada.getEstado().equals(EstadoPartida.EN_ESPERA)) {
            validarCantidadDeJugadores(partidaEncontrada);
            this.inicializarAtributosDeCadaJugador(partidaEncontrada);
            
            this.seleccionarAleatoriamenteElPrimerTurnoDelJugador(partidaEncontrada);
            partidaEncontrada.setEstado(EstadoPartida.EN_CURSO);
            this.repositorioPartida.guardarPartida(partidaEncontrada);
        }
    }

    private void validarCantidadDeJugadores(Partida partida)
        throws CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        List<Jugador> jugadores = partida.getJugadores();

        if (jugadores.size() < this.CANTIDAD_ESTABLECIDA_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }

        if (jugadores.size() > this.CANTIDAD_ESTABLECIDA_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(Partida partida){
        List<Jugador> jugadores = partida.getJugadores();
        Integer cantidadJugadores = jugadores.size();
        Integer turnoAleatorio = random.nextInt(cantidadJugadores);
        Jugador jugadorSeleccionado = jugadores.get(turnoAleatorio);

        partida.setJugadorTurnoAleatorio(jugadorSeleccionado);
        
    }

    private void inicializarAtributosDeCadaJugador(Partida partida) {
        List<Jugador> jugadores = partida.getJugadores();

        for (Jugador jugador : jugadores) {
            jugador.setDinero(CANTIDAD_INICIAL_DINERO);
            jugador.setPosicionActual(POSICION_INICIAL);
        }
    }
}
