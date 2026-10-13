package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
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
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final int CANTIDAD_MAXIMA_DE_JUGADORES = 4;
    private static final int CANTIDAD_MINIMA_DE_JUGADORES = 2;
    private static final int CANTIDAD_INICIAL_DINERO = 150000;
    private static final int POSICION_INICIAL = 0;

    private final RepositorioPartida repositorioPartida;
    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final Random random = new Random();
    private final ServicioDado servicioDado;

    @Autowired
    public ServicioPartidaImpl(
        RepositorioPartida repositorioPartida,
        RepositorioSalaDeEspera repositorioSalaDeEspera,
        ServicioDado servicioDado
    ) {
        this.repositorioPartida = repositorioPartida;
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.servicioDado = servicioDado;
    }

    @Override
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    @Override
    public void moverJugador(Jugador jugador, Integer cantidadAvance, Integer totalCasilleros) {
        Integer posicionActual = jugador.getPosicionActual();
        Integer nuevaPosicion = (posicionActual + cantidadAvance) % totalCasilleros;//se calcula la nueva posicion. si supera el total la division se reinicia
        jugador.setPosicionActual(nuevaPosicion);
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
    public void incrementarRondas(Partida partida) {
        if (partida.getRondaActual() == null) {
            partida.setRondaActual(1);
        }

        if (partida.getTurnosFinalizadosEnRonda() == null) {
            partida.setTurnosFinalizadosEnRonda(0);
        }

        Integer turnosFinalizado = partida.getTurnosFinalizadosEnRonda() + 1;

        partida.setTurnosFinalizadosEnRonda(turnosFinalizado);

        if (turnosFinalizado == partida.getJugadores().size()) {
            partida.setRondaActual(partida.getRondaActual() + 1);
            partida.setTurnosFinalizadosEnRonda(0);
        }
    }

    @Override
    public ResultadoTiradaDTO tirarDados() {
        return servicioDado.tirarDados();
    }

    @Override
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        if (
            partida != null && partida.getJugadores() != null && !partida.getJugadores().isEmpty()
        ) {
            int orden = 0;
            for (Jugador jugador : partida.getJugadores()) {
                jugador.setOrden(orden);
                jugador.setPierdeTurno(false);
                orden++;
            }
            if (partida.getIndiceTurnoActual() == null) {
                partida.setIndiceTurnoActual(0);
            }
            repositorioPartida.guardarPartida(partida);
        }
    }

    @Override
    public Jugador obtenerJugadorActual(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (partida == null || partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
            return null;
        }

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        Integer indiceActual =
            partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;
        return buscarJugadorPorTurno(partida.getJugadores(), indiceActual);
    }

    private Jugador buscarJugadorPorTurno(List<Jugador> jugadores, Integer indice) {
        for (Jugador jugador : jugadores) {
            if (jugador.getOrden() != null && jugador.getOrden().equals(indice)) {
                return jugador;
            }
        }
        return jugadores.get(0);
    }

    @Override
    public void avanzarTurno(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        if (
            partida != null && partida.getJugadores() != null && !partida.getJugadores().isEmpty()
        ) {
            int cantidadJugadores = partida.getJugadores().size();
            int indiceActual =
                partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;

            incrementarRondas(partida);

            int siguienteIndice = (indiceActual + 1) % cantidadJugadores;
            partida.setIndiceTurnoActual(siguienteIndice);
            repositorioPartida.guardarPartida(partida);

            Jugador jugadorSiguiente = obtenerJugadorActual(codigoUnico);

            if (
                jugadorSiguiente != null && Boolean.TRUE.equals(jugadorSiguiente.getPierdeTurno())
            ) {
                jugadorSiguiente.setPierdeTurno(false);
                repositorioPartida.guardarPartida(partida);
                avanzarTurno(codigoUnico);
            }
        }
    }

    @Override
    public void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigo);
        validarSalaYPermisos(sala, idUsuario);

        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigo);
        validarQueNoEsteEnCurso(partida);

        if (partida == null) {
            partida = crearPartidaDesdeSala(sala);
        } else if (partida.getJugadores().isEmpty()) {
            agregarJugadoresDesdeSala(partida, sala);
        }

        validarCantidadDeJugadores(partida);
        inicializarAtributosDeCadaJugador(partida);
        asignarOrdenInicialJugadores(partida);

        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setTiempoInicio(Instant.now());

        seleccionarAleatoriamenteElPrimerTurnoDelJugador(partida);
        repositorioPartida.guardarPartida(partida);
        
    }

    private void validarSalaYPermisos(SalaDeEspera sala, Long idUsuario)
        throws SalaNoEncontradaException {
        if (sala == null) {
            throw new SalaNoEncontradaException("La sala no existe");
        }
        if (!sala.getCreador().getId().equals(idUsuario)) {
            throw new IllegalArgumentException(
                "Solo el creador de la sala puede iniciar la partida"
            );
        }
    }

    private void validarQueNoEsteEnCurso(Partida partida) throws PartidaIniciadaException {
        if (partida != null && EstadoPartida.EN_CURSO.equals(partida.getEstado())) {
            throw new PartidaIniciadaException();
        }
    }

    private Partida crearPartidaDesdeSala(SalaDeEspera sala) throws FichaNoDisponibleException {
        Partida partida = new Partida();

        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setCreador(sala.getCreador());
        partida.setEstado(EstadoPartida.EN_ESPERA);

        agregarJugadoresDesdeSala(partida, sala);

        return partida;
    }

    private void agregarJugadoresDesdeSala(Partida partida, SalaDeEspera sala)
        throws FichaNoDisponibleException {
        if (sala.getUsuarios() != null) {
            for (Usuario usuario : sala.getUsuarios()) {
                Jugador jugador = crearJugador(usuario);
                partida.getJugadores().add(jugador);
            }
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
            default -> throw new FichaNoDisponibleException("Ficha no disponible para la partida");
        };
    }

    private void validarCantidadDeJugadores(Partida partida)
        throws CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        List<Jugador> jugadores = partida.getJugadores();

        if (jugadores.size() < CANTIDAD_MINIMA_DE_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }

        if (jugadores.size() > CANTIDAD_MAXIMA_DE_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(Partida partida) {
        List<Jugador> jugadores = partida.getJugadores();

        if (jugadores != null && !jugadores.isEmpty()) {
            Integer turnoAleatorio = random.nextInt(jugadores.size());

            Jugador jugadorSeleccionado = jugadores.get(turnoAleatorio);

            partida.setJugadorTurnoAleatorio(jugadorSeleccionado);
            partida.setIndiceTurnoActual(jugadorSeleccionado.getOrden());
        }
    }

    private void inicializarAtributosDeCadaJugador(Partida partida) {
        if (partida.getJugadores() != null) {
            for (Jugador jugador : partida.getJugadores()) {
                jugador.setDinero(CANTIDAD_INICIAL_DINERO);
                jugador.setPosicionActual(POSICION_INICIAL);
            }
        }
    }

    private void asignarOrdenInicialJugadores(Partida partida) {
        int orden = 0;

        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.setPierdeTurno(false);
            orden++;
        }
    }
}
