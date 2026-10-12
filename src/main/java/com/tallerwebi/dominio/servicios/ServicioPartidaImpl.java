package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.Ficha;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final int CANTIDAD_ESTABLECIDA_JUGADORES = 4;
    private static final int CANTIDAD_INICIAL_DINERO = 150000;
    private static final int POSICION_INICIAL = 0;

    private final RepositorioPartida repositorioPartida;
    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final Random random = new Random();

    @Autowired
    public ServicioPartidaImpl(
        RepositorioPartida repositorioPartida,
        RepositorioSalaDeEspera repositorioSalaDeEspera
    ) {
        this.repositorioPartida = repositorioPartida;
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
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
    public void iniciarPartida(String codigo)
        throws PartidaNoEncontradaException,
        CantidadJugadoresInsuficienteException,
        CantidadMaximaJugadoresSuperadaException,
        PartidaIniciadaException,
        SalaNoEncontradaException, FichaNoDisponibleException {

        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigo);

        if (sala == null) {
            throw new SalaNoEncontradaException("La sala no existe");
        }

        Partida partida = repositorioPartida
            .buscarPartidaActivaPorCodigoUnico(codigo);

        if (partida != null &&
            EstadoPartida.EN_CURSO.equals(partida.getEstado())) {
            throw new PartidaIniciadaException();
        }

        if (partida == null) {
            partida = crearPartidaDesdeSala(sala);
        } else if (partida.getJugadores().isEmpty()) {
            agregarJugadoresDesdeSala(partida, sala);
        }

        validarCantidadDeJugadores(partida);

        inicializarAtributosDeCadaJugador(partida);

        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setTiempoInicio(Instant.now());

        repositorioPartida.guardarPartida(partida);

        seleccionarAleatoriamenteElPrimerTurnoDelJugador(partida);
    }

    private Partida crearPartidaDesdeSala(SalaDeEspera sala) throws FichaNoDisponibleException {

        Partida partida = new Partida();

        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setCreador(sala.getCreador());
        partida.setEstado(EstadoPartida.EN_ESPERA);

        agregarJugadoresDesdeSala(partida, sala);

        return partida;
    }

    private void agregarJugadoresDesdeSala(
        Partida partida,
        SalaDeEspera sala
    ) throws FichaNoDisponibleException {
        for (Usuario usuario : sala.getUsuarios()) {
            Jugador jugador = crearJugador(usuario);
            partida.getJugadores().add(jugador);
        }
    }

    private Jugador crearJugador(Usuario usuario) throws FichaNoDisponibleException {

        Jugador jugador = new Jugador();

        jugador.setApodo(usuario.getUsername());

        if (usuario.getFicha() != null) {
            jugador.setFicha(convertirFicha(usuario.getFicha()));
        }

        return jugador;
    }

    private ColorFicha convertirFicha(Ficha ficha) throws FichaNoDisponibleException {
        return switch (ficha) {
            case ROJA -> ColorFicha.ROJO;
            case AZUL -> ColorFicha.AZUL;
            case VERDE -> ColorFicha.VERDE;
            case AMARILLA -> ColorFicha.AMARILLO;
            default -> throw new FichaNoDisponibleException(
                "Ficha no disponible para la partida"
            );
        };
    }

    private void validarCantidadDeJugadores(Partida partida)
        throws CantidadJugadoresInsuficienteException,
        CantidadMaximaJugadoresSuperadaException {

        List<Jugador> jugadores = partida.getJugadores();

        if (jugadores.size() < CANTIDAD_ESTABLECIDA_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }

        if (jugadores.size() > CANTIDAD_ESTABLECIDA_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(
        Partida partida
    ) {
        List<Jugador> jugadores = partida.getJugadores();

        int turnoAleatorio = random.nextInt(jugadores.size());

        Jugador jugadorSeleccionado = jugadores.get(turnoAleatorio);

        partida.setJugadorTurnoAleatorio(jugadorSeleccionado);
    }

    private void inicializarAtributosDeCadaJugador(Partida partida) {

        for (Jugador jugador : partida.getJugadores()) {
            jugador.setDinero(CANTIDAD_INICIAL_DINERO);
            jugador.setPosicionActual(POSICION_INICIAL);
        }
    }
}