package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valido la sala y preparo el inicio de la partida. */
@Service("servicioInicioPartida")
@Transactional
public class ServicioInicioPartidaImpl implements ServicioInicioPartida {
    private static final Integer CANTIDAD_MAXIMA_DE_JUGADORES = 4;
    private static final Integer CANTIDAD_MINIMA_DE_JUGADORES = 2;
    private static final Integer CANTIDAD_INICIAL_DINERO = 2000;
    private static final Integer POSICION_INICIAL = 0;

    private final RepositorioPartida repositorioPartida;
    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final ServicioTurno servicioTurno;
    private final Random random = new Random();

    public ServicioInicioPartidaImpl(RepositorioPartida repositorioPartida,
            RepositorioSalaDeEspera repositorioSalaDeEspera, ServicioTurno servicioTurno) {
        this.repositorioPartida = repositorioPartida;
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.servicioTurno = servicioTurno;
    }

    @Override
    public Partida iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigo);
        validarSalaYPermisos(sala, idUsuario);
        validarFichasSeleccionadas(sala);

        Partida partida = prepararYCrearPartida(codigo, sala);

        validarCantidadDeJugadores(partida);
        inicializarAtributosDeCadaJugador(partida);
        servicioTurno.asignarOrdenInicialJugadores(partida);

        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setTiempoInicio(Instant.now());

        seleccionarAleatoriamenteElPrimerTurnoDelJugador(partida);
        repositorioPartida.guardarPartida(partida);

        return partida;
    }

    private Partida prepararYCrearPartida(String codigo, SalaDeEspera sala)
        throws PartidaIniciadaException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigo);
        if (partida != null && EstadoPartida.EN_CURSO.equals(partida.getEstado())) {
            throw new PartidaIniciadaException();
        }

        if (partida == null) {
            return crearPartidaDesdeSala(sala);
        }

        if (partida.getJugadores().isEmpty()) {
            agregarJugadoresDesdeSala(partida, sala);
        }
        return partida;
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

    private Partida crearPartidaDesdeSala(SalaDeEspera sala) {
        Partida partida = new Partida();
        partida.setCodigoUnico(sala.getCodigoUnico());
        partida.setCreador(sala.getCreador());
        partida.setEstado(EstadoPartida.EN_ESPERA);
        agregarJugadoresDesdeSala(partida, sala);
        return partida;
    }

    private void agregarJugadoresDesdeSala(Partida partida, SalaDeEspera sala) {
        if (sala.getUsuarios() == null) {
            return;
        }
        for (Usuario usuario : sala.getUsuarios()) {
            partida.getJugadores().add(crearJugador(usuario));
        }
    }

    private Jugador crearJugador(Usuario usuario) {
        Jugador jugador = new Jugador();
        jugador.setApodo(usuario.getUsername());
        jugador.setFicha(usuario.getFicha());
        return jugador;
    }

    private void validarCantidadDeJugadores(Partida partida)
        throws CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        Integer total = partida.getJugadores().size();
        if (total < CANTIDAD_MINIMA_DE_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }
        if (total > CANTIDAD_MAXIMA_DE_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(Partida partida) {
        if (esListaDeJugadoresVacia(partida.getJugadores())) {
            return;
        }
        Jugador seleccionado = partida
            .getJugadores()
            .get(random.nextInt(partida.getJugadores().size()));
        partida.setJugadorTurnoAleatorio(seleccionado);
        partida.setIndiceTurnoActual(seleccionado.getOrden());
    }

    private void inicializarAtributosDeCadaJugador(Partida partida) {
        if (esListaDeJugadoresVacia(partida.getJugadores())) {
            return;
        }
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setDinero(CANTIDAD_INICIAL_DINERO);
            jugador.setPosicionActual(POSICION_INICIAL);
            jugador.setEnBancarrota(false);
        }
    }

    private void validarFichasSeleccionadas(SalaDeEspera sala) throws FichaNoDisponibleException {
        for (Usuario usuario : sala.getUsuarios()) {
            if (usuario.getFicha() == null) {
                throw new FichaNoDisponibleException("No se puede iniciar: Todos los jugadores deben seleccionar una ficha.");
            }
        }
    }

    private Boolean esListaDeJugadoresVacia(List<Jugador> jugadores) {
        return jugadores == null || jugadores.isEmpty();
    }
}
