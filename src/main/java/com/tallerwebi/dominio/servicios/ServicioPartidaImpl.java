package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final int CANTIDAD_MAXIMA_DE_JUGADORES = 4;
    private static final int CANTIDAD_MINIMA_DE_JUGADORES = 2;
    private static final int CANTIDAD_INICIAL_DINERO = 150000;
    private static final int POSICION_INICIAL = 0;
    private static final int DURACION_TURNO_SEGUNDOS = 30;

    private final RepositorioPartida repositorioPartida;
    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final Random random = new Random();
    private final ServicioDado servicioDado;
    private final NotificadorPartida notificadorPartida;
    private final GestorTemporizadorPartida gestorTemporizador;

    @Autowired
    public ServicioPartidaImpl(
        RepositorioPartida repositorioPartida,
        RepositorioSalaDeEspera repositorioSalaDeEspera,
        ServicioDado servicioDado,
        GestorTemporizadorPartida gestorTemporizador,
        @Autowired(required = false) NotificadorPartida notificadorPartida
    ) {
        this.repositorioPartida = repositorioPartida;
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.servicioDado = servicioDado;
        this.gestorTemporizador = gestorTemporizador;
        this.notificadorPartida = notificadorPartida;
    }

    @Override
    @Transactional(readOnly = true)
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    @Override
    public void moverJugador(Jugador jugador, Integer cantidadAvance, Integer totalCasilleros) {
        jugador.setPosicionActual((jugador.getPosicionActual() + cantidadAvance) % totalCasilleros);
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
    public void actualizarJugador(Jugador jugador) {
        repositorioPartida.actualizarJugador(jugador);
    }

    @Override
    public void incrementarRondas(Partida partida) {
        int ronda = partida.getRondaActual() == null ? 1 : partida.getRondaActual();
        int turnosFinalizados =
            partida.getTurnosFinalizadosEnRonda() == null
                ? 0
                : partida.getTurnosFinalizadosEnRonda();

        turnosFinalizados++;

        if (turnosFinalizados >= partida.getJugadores().size()) {
            ronda++;
            turnosFinalizados = 0;
        }

        partida.setRondaActual(ronda);
        partida.setTurnosFinalizadosEnRonda(turnosFinalizados);
    }

    @Override
    public ResultadoTiradaDTO tirarDados() {
        return servicioDado.tirarDados();
    }

    @Override
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }

        int orden = 0;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.setPierdeTurno(false);
            orden++;
        }
        partida.setIndiceTurnoActual(
            partida.getIndiceTurnoActual() == null ? 0 : partida.getIndiceTurnoActual()
        );
        repositorioPartida.guardarPartida(partida);
    }

    @Override
    @Transactional(readOnly = true)
    public Jugador obtenerJugadorActual(String codigoUnico) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return null;
        }

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        Integer indiceActual =
            partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;
        return buscarJugadorPorTurno(partida.getJugadores(), indiceActual);
    }

    @Override
    public Jugador obtenerJugadorPorId(Long idJugador) {
        return repositorioPartida.buscarJugadorPorId(idJugador);
    }

    private Jugador buscarJugadorPorTurno(List<Jugador> jugadores, Integer indice) {
        for (Jugador jugador : jugadores) {
            if (indice.equals(jugador.getOrden())) {
                return jugador;
            }
        }
        return jugadores.get(0);
    }

    @Override
    public void avanzarTurno(String codigoUnico) {
        gestorTemporizador.cancelarTemporizador(codigoUnico);

        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }

        int indiceActual =
            partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;
        incrementarRondas(partida);

        partida.setIndiceTurnoActual((indiceActual + 1) % partida.getJugadores().size());
        repositorioPartida.guardarPartida(partida);

        Jugador jugadorSiguiente = obtenerJugadorActual(codigoUnico);
        if (jugadorSiguiente != null && Boolean.TRUE.equals(jugadorSiguiente.getPierdeTurno())) {
            jugadorSiguiente.setPierdeTurno(false);
            repositorioPartida.guardarPartida(partida);
            avanzarTurno(codigoUnico);
            return;
        }

        notificarTurnoActual(partida);
    }

    @Override
    public void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigo);
        validarSalaYPermisos(sala, idUsuario);
        validarFichasSeleccionadas(sala);

        Partida partida = prepararYCrearPartida(codigo, sala);

        validarCantidadDeJugadores(partida);
        inicializarAtributosDeCadaJugador(partida);
        asignarOrdenInicialJugadores(partida);

        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setTiempoInicio(Instant.now());

        seleccionarAleatoriamenteElPrimerTurnoDelJugador(partida);
        repositorioPartida.guardarPartida(partida);

        notificarTurnoActual(partida);
    }

    private Partida prepararYCrearPartida(String codigo, SalaDeEspera sala)
        throws PartidaIniciadaException, FichaNoDisponibleException {
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
            jugador.setFicha(usuario.getFicha());
        }

        return jugador;
    }

    private void validarCantidadDeJugadores(Partida partida)
        throws CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException {
        int total = partida.getJugadores().size();
        if (total < CANTIDAD_MINIMA_DE_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }
        if (total > CANTIDAD_MAXIMA_DE_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(Partida partida) {
        if (partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
            return;
        }
        Jugador seleccionado = partida
            .getJugadores()
            .get(random.nextInt(partida.getJugadores().size()));
        partida.setJugadorTurnoAleatorio(seleccionado);
        partida.setIndiceTurnoActual(seleccionado.getOrden());
    }

    private void inicializarAtributosDeCadaJugador(Partida partida) {
        if (partida.getJugadores() == null) {
            return;
        }
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setDinero(CANTIDAD_INICIAL_DINERO);
            jugador.setPosicionActual(POSICION_INICIAL);
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

    @Override
    @Transactional(readOnly = true)
    public List<PosicionFichaDTO> obtenerPosicionesFichas(String codigo) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigo);
        if (esPartidaConJugadoresInvalida(partida)) {
            return List.of();
        }
        return partida.getJugadores().stream().map(this::mapearAJugadorPosicionDTO).toList();
    }

    private PosicionFichaDTO mapearAJugadorPosicionDTO(Jugador jugador) {
        String nombreFicha = obtenerNombreFichaSegura(jugador);
        Integer posicion =
            jugador.getPosicionActual() != null ? jugador.getPosicionActual() : POSICION_INICIAL;
        return new PosicionFichaDTO(jugador.getId(), jugador.getApodo(), nombreFicha, posicion);
    }

    private void notificarTurnoActual(Partida partida) {
        Jugador jugadorActual = obtenerJugadorActual(partida.getCodigoUnico());
        if (jugadorActual != null && notificadorPartida != null) {
            TurnoDTO turnoDTO = new TurnoDTO(
                partida.getCodigoUnico(),
                jugadorActual.getId(),
                jugadorActual.getApodo(),
                jugadorActual.getOrden(),
                "Es el turno de " + jugadorActual.getApodo(),
                DURACION_TURNO_SEGUNDOS
            );
            turnoDTO.setRondaActual(partida.getRondaActual());
            notificadorPartida.notificarCambioDeTurno(partida.getCodigoUnico(), turnoDTO);
        }

        if (partida != null && partida.getCodigoUnico() != null) {
            gestorTemporizador.programarTemporizador(
                partida.getCodigoUnico(),
                DURACION_TURNO_SEGUNDOS,
                () -> avanzarTurno(partida.getCodigoUnico())
            );
        }
    }

    private boolean esPartidaConJugadoresInvalida(Partida partida) {
        return (
            partida == null || partida.getJugadores() == null || partida.getJugadores().isEmpty()
        );
    }

    private String obtenerNombreFichaSegura(Jugador jugador) {
        if (jugador.getFicha() != null) {
            return jugador.getFicha().name();
        };
        return null;
    }


    private void validarFichasSeleccionadas(SalaDeEspera sala) throws FichaNoDisponibleException {
        for (Usuario usuario : sala.getUsuarios()) {
            if (usuario.getFicha() == null) {
                throw new FichaNoDisponibleException("No se puede iniciar: Todos los jugadores deben seleccionar una ficha.");
            }
        }
    }
}
