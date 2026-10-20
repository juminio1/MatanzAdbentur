package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.InicioPartidaNoAutorizadoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.PartidaSinJugadoresException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioPartida;
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

    private static final Integer CANTIDAD_MAXIMA_DE_JUGADORES = 4;
    private static final Integer CANTIDAD_MINIMA_DE_JUGADORES = 2;
    private static final Integer CANTIDAD_INICIAL_DINERO = 2000;
    private static final Integer LIMITE_BANCARROTA = 250;
    private static final Integer POSICION_INICIAL = 0;
    private static final Integer DURACION_TURNO_SEGUNDOS = 30;

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
        if (jugador.getDinero() <= LIMITE_BANCARROTA) {
            jugador.setEnBancarrota(true);
        }
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
        Integer ronda = partida.getRondaActual() == null ? 1 : partida.getRondaActual();
        Integer turnosFinalizados =
            partida.getTurnosFinalizadosEnRonda() == null
                ? 0
                : partida.getTurnosFinalizadosEnRonda();

        long jugadoresActivos = partida
            .getJugadores()
            .stream()
            .filter(jugador -> !jugador.getEnBancarrota())
            .count();
        if (jugadoresActivos == 0) return;

        turnosFinalizados++;

        if (turnosFinalizados >= jugadoresActivos) {
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
    public void inicializarTurnos(String codigoUnico) throws PartidaIniciadaException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        esPartidaConJugadoresInvalida(partida);

        Integer orden = 0;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.setPierdeTurno(false);
            jugador.setEnBancarrota(false);
            orden++;
        }
        partida.setIndiceTurnoActual(
            partida.getIndiceTurnoActual() == null ? 0 : partida.getIndiceTurnoActual()
        );
        repositorioPartida.guardarPartida(partida);
    }

    @Override
    @Transactional(readOnly = true)
    public Jugador obtenerJugadorActual(String codigoUnico) throws PartidaIniciadaException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        esPartidaConJugadoresInvalida(partida);

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        Integer indiceActual =
            partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;
        Jugador jugadorActual = buscarJugadorPorTurno(partida.getJugadores(), indiceActual);
        return jugadorActual != null && !jugadorActual.getEnBancarrota() ? jugadorActual : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSegundosRestantesTurno(String codigoUnico) {
        return gestorTemporizador.obtenerSegundosRestantes(codigoUnico);
    }

    @Override
    public void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico)
        throws PartidaNoEncontradaException, PartidaIniciadaException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        if (partida == null) throw new PartidaNoEncontradaException();

        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(
            codigoUnico,
            DURACION_TURNO_SEGUNDOS,
            () -> {
                try {
                    avanzarTurno(codigoUnico);
                } catch (
                    PartidaIniciadaException
                    | PartidaSinJugadoresException
                    | EstadoPartidaInvalidoException e
                ) {
                    throw new IllegalStateException(
                        "No se pudo avanzar el turno de la partida.",
                        e
                    );
                }
            }
        );

        if (iniciado) notificarTurnoActual(partida, false);
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
    public void avanzarTurno(String codigoUnico)
        throws PartidaIniciadaException, PartidaSinJugadoresException, EstadoPartidaInvalidoException {
        gestorTemporizador.cancelarTemporizador(codigoUnico);

        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);

        if (esPartidaConJugadoresInvalida(partida)) {
            throw new PartidaSinJugadoresException("No existe jugadores en la partida");
        }

        Integer indiceActual =
            partida.getIndiceTurnoActual() != null ? partida.getIndiceTurnoActual() : 0;
        incrementarRondas(partida);

        Jugador candidato = buscarSiguienteJugadorActivo(partida, indiceActual, false);
        if (candidato == null) {
            // Si todos los jugadores activos estaban penalizados, termina el efecto
            // de perder turno para que la partida nunca quede bloqueada.
            candidato = buscarSiguienteJugadorActivo(partida, indiceActual, true);
            if (candidato != null) candidato.setPierdeTurno(false);
        }
        if (candidato == null) {
            repositorioPartida.guardarPartida(partida);
            gestorTemporizador.cancelarTemporizador(codigoUnico);
            throw new EstadoPartidaInvalidoException(
                "No se puede avanzar el turno: no hay jugadores activos disponibles."
            );
        }

        partida.setIndiceTurnoActual(candidato.getOrden());
        repositorioPartida.guardarPartida(partida);
        notificarTurnoActual(partida, true);
    }

    private Jugador buscarSiguienteJugadorActivo(
        Partida partida,
        Integer indiceActual,
        Boolean incluirJugadoresPenalizados
    ) {
        Integer cantidadJugadores = partida.getJugadores().size();
        for (Integer desplazamiento = 1; desplazamiento <= cantidadJugadores; desplazamiento++) {
            Integer siguienteIndice = (indiceActual + desplazamiento) % cantidadJugadores;
            Jugador candidato = buscarJugadorPorTurno(partida.getJugadores(), siguienteIndice);
            if (candidato != null && !candidato.getEnBancarrota()) {
                if (
                    Boolean.TRUE.equals(candidato.getPierdeTurno()) && !incluirJugadoresPenalizados
                ) {
                    //No entiendo nada
                    candidato.setPierdeTurno(false);
                } else {
                    return candidato;
                }
            }
        }
        return null;
    }

    @Override
    public void iniciarPartida(String codigo, Long idUsuario)
        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException, InicioPartidaNoAutorizadoException, EstadoPartidaInvalidoException {
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

        gestorTemporizador.cancelarTemporizador(codigo);
        notificarTurnoActual(partida, false);
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
        throws SalaNoEncontradaException, InicioPartidaNoAutorizadoException {
        if (sala == null) {
            throw new SalaNoEncontradaException("La sala no existe");
        }
        if (!sala.getCreador().getId().equals(idUsuario)) {
            throw new InicioPartidaNoAutorizadoException(
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
        Integer cantidadDeJugadores = partida.getJugadores().size();

        if (cantidadDeJugadores < CANTIDAD_MINIMA_DE_JUGADORES) {
            throw new CantidadJugadoresInsuficienteException("Cantidad de jugadores insuficientes");
        }
        if (cantidadDeJugadores > CANTIDAD_MAXIMA_DE_JUGADORES) {
            throw new CantidadMaximaJugadoresSuperadaException("Máxima de jugadores superados");
        }
    }

    private void seleccionarAleatoriamenteElPrimerTurnoDelJugador(Partida partida)
        throws CantidadJugadoresInsuficienteException {
        if (partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
            throw new CantidadJugadoresInsuficienteException(
                "La cantidad de jugadores es insuficiente"
            );
        }
        Jugador seleccionado = partida
            .getJugadores()
            .get(random.nextInt(partida.getJugadores().size()));
        partida.setJugadorTurnoAleatorio(seleccionado);
        partida.setIndiceTurnoActual(seleccionado.getOrden());
    }

    private void inicializarAtributosDeCadaJugador(Partida partida)
        throws EstadoPartidaInvalidoException {
        if (partida.getJugadores() == null) {
            throw new EstadoPartidaInvalidoException(
                "No se pueden inicializar los atributos: la lista de jugadores de la partida no está inicializada."
            );
        }
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setDinero(CANTIDAD_INICIAL_DINERO);
            jugador.setPosicionActual(POSICION_INICIAL);
            jugador.setEnBancarrota(false);
        }
    }

    private void asignarOrdenInicialJugadores(Partida partida) {
        Integer orden = 0;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.setPierdeTurno(false);
            orden++;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosicionFichaDTO> obtenerPosicionesFichas(String codigo)
        throws PartidaIniciadaException {
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

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador)
        throws PartidaIniciadaException {
        Jugador jugadorActual = obtenerJugadorActual(partida.getCodigoUnico());
        if (jugadorActual != null && iniciarTemporizador && partida.getCodigoUnico() != null) {
            gestorTemporizador.programarTemporizador(
                partida.getCodigoUnico(),
                DURACION_TURNO_SEGUNDOS,
                () -> {
                    try {
                        avanzarTurno(partida.getCodigoUnico());
                    } catch (
                        PartidaIniciadaException
                        | PartidaSinJugadoresException
                        | EstadoPartidaInvalidoException e
                    ) {
                        throw new IllegalStateException(
                            "No se pudo avanzar el turno de la partida.",
                            e
                        );
                    }
                }
            );
        }
        if (jugadorActual != null && notificadorPartida != null) {
            TurnoDTO turnoDTO = new TurnoDTO(
                partida.getCodigoUnico(),
                jugadorActual.getId(),
                jugadorActual.getApodo(),
                jugadorActual.getOrden(),
                "Es el turno de " + jugadorActual.getApodo(),
                obtenerSegundosRestantesTurno(partida.getCodigoUnico())
            );
            turnoDTO.setRondaActual(partida.getRondaActual());
            notificadorPartida.notificarCambioDeTurno(partida.getCodigoUnico(), turnoDTO);
        }
    }

    private Boolean esPartidaConJugadoresInvalida(Partida partida) {
        return (
            partida == null || partida.getJugadores() == null || partida.getJugadores().isEmpty()
        );
    }

    private String obtenerNombreFichaSegura(Jugador jugador) {
        if (jugador.getFicha() != null) {
            return jugador.getFicha().name();
        }
        return null;
    }

    private void validarFichasSeleccionadas(SalaDeEspera sala) throws FichaNoDisponibleException {
        for (Usuario usuario : sala.getUsuarios()) {
            if (usuario.getFicha() == null) {
                throw new FichaNoDisponibleException(
                    "No se puede iniciar: Todos los jugadores deben seleccionar una ficha."
                );
            }
        }
    }
}
