package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.presentacion.DTO.PosicionFichaDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona el inicio de partidas, los jugadores y el avance de sus turnos. */
@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final Integer CANTIDAD_MAXIMA_DE_JUGADORES = 4;
    private static final Integer CANTIDAD_MINIMA_DE_JUGADORES = 2;
    private static final Integer CANTIDAD_INICIAL_DINERO = 2000;
    private static final Integer LIMITE_BANCARROTA = 250;
    private static final Integer POSICION_INICIAL = 0;
    private static final Integer DURACION_TURNO_SEGUNDOS = 30;
    private final Set<String> partidasPausadasPorEvento = ConcurrentHashMap.newKeySet();

    private final RepositorioPartida repositorioPartida;
    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final Random random = new Random();
    private final ServicioDado servicioDado;
    private final ServicioEvento servicioEvento;
    private final PublicadorEventos<EventoTurnoCambiado> publicadorTurno;
    private final GestorTemporizadorPartida gestorTemporizador;

     @Autowired
    public ServicioPartidaImpl(
        RepositorioPartida repositorioPartida,
        RepositorioSalaDeEspera repositorioSalaDeEspera,
        ServicioDado servicioDado,
        GestorTemporizadorPartida gestorTemporizador,
        @Autowired(required = false) PublicadorEventos<EventoTurnoCambiado> publicadorTurno, ServicioEvento servicioEvento
    ) {
        this.repositorioPartida = repositorioPartida;
        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.servicioDado = servicioDado;
        this.gestorTemporizador = gestorTemporizador;
        this.publicadorTurno = publicadorTurno;
        this.servicioEvento = servicioEvento;
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

        Long jugadoresActivos = partida.getJugadores().stream()
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
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }

        asignarOrdenInicialJugadores(partida);
        partida.getJugadores().forEach(jugador -> jugador.setEnBancarrota(false));
        partida.setIndiceTurnoActual(obtenerIndiceTurnoActual(partida));
        repositorioPartida.guardarPartida(partida);
    }

    @Override
    @Transactional(readOnly = true)
    public Jugador obtenerJugadorActual(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return null;
        }

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        Integer indiceActual = obtenerIndiceTurnoActual(partida);
        Jugador jugadorActual = buscarJugadorPorTurno(partida.getJugadores(), indiceActual);
        return esJugadorActivo(jugadorActual) ? jugadorActual : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSegundosRestantesTurno(String codigoUnico) {
        return gestorTemporizador.obtenerSegundosRestantes(codigoUnico);
    }

    @Override
    public void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico) {

        Partida partida = obtenerPartida(codigoUnico);

        if (partida == null) {
            return;
        }

        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(
            codigoUnico,
            DURACION_TURNO_SEGUNDOS,
            () -> avanzarTurnoDesdeTemporizador(codigoUnico)
        );

        if (iniciado) {
            notificarTurnoActual(partida, false);
        }
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
    public void avanzarTurno(String codigoUnico) throws ActivacionEventoInvalidoException {

        gestorTemporizador.cancelarTemporizador(codigoUnico);

        Partida partida = obtenerPartida(codigoUnico);

        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }

        Integer indiceActual = obtenerIndiceTurnoActual(partida);
        Integer rondaAnterior = partida.getRondaActual();

        incrementarRondas(partida);

        verificarEventoGlobalPorCambioDeRonda(partida, rondaAnterior);

        Jugador candidato = resolverSiguienteJugador(partida, indiceActual);

        if (candidato == null) {
            repositorioPartida.guardarPartida(partida);
            gestorTemporizador.cancelarTemporizador(codigoUnico);
            return;
        }

        partida.setIndiceTurnoActual(candidato.getOrden());

        repositorioPartida.guardarPartida(partida);

        notificarTurnoActual(partida, true);
    }

    private void verificarEventoGlobalPorCambioDeRonda(Partida partida, Integer rondaAnterior) throws ActivacionEventoInvalidoException {

        if (java.util.Objects.equals(rondaAnterior, partida.getRondaActual())) {
            return;
        }

        try {
            servicioEvento.verificarActivacionEventoGlobal(partida);

        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException | EventoGlobalActivoException excepcion) {

            throw new ActivacionEventoInvalidoException("No se pudo activar el evento global");
        }
    }

    private Integer obtenerIndiceTurnoActual(Partida partida) {
        return partida.getIndiceTurnoActual() == null ? 0 : partida.getIndiceTurnoActual();
    }

    private Jugador resolverSiguienteJugador(Partida partida, Integer indiceActual) {
        Jugador candidato = buscarSiguienteJugadorActivo(partida, indiceActual, false);
        if (candidato != null) {
            return candidato;
        }
        // Una segunda vuelta permite continuar si todos los activos perdían el turno.
        candidato = buscarSiguienteJugadorActivo(partida, indiceActual, true);
        if (candidato != null) {
            candidato.setPierdeTurno(false);
        }
        return candidato;
    }

    private void avanzarTurnoDesdeTemporizador(String codigoUnico) {

        try {
            avanzarTurno(codigoUnico);

        } catch (ActivacionEventoInvalidoException excepcion) {

         throw new IllegalStateException("No se pudo avanzar el turno por un error al activar el evento global", excepcion);
        }
    }

    private Jugador buscarSiguienteJugadorActivo(Partida partida, Integer indiceActual,
                                                 Boolean incluirJugadoresPenalizados) {
        Integer cantidadJugadores = partida.getJugadores().size();
        for (Integer desplazamiento = 1; desplazamiento <= cantidadJugadores; desplazamiento++) {
            Integer siguienteIndice = (indiceActual + desplazamiento) % cantidadJugadores;
            Jugador candidato = buscarJugadorPorTurno(partida.getJugadores(), siguienteIndice);
            if (!esJugadorActivo(candidato)) {
                continue;
            }
            if (Boolean.TRUE.equals(candidato.getPierdeTurno()) && !incluirJugadoresPenalizados) {
                candidato.setPierdeTurno(false);
            } else {
                return candidato;
            }
        }
        return null;
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

        gestorTemporizador.cancelarTemporizador(codigo);
        notificarTurnoActual(partida, false);
    }

    private Partida prepararYCrearPartida(String codigo, SalaDeEspera sala)
        throws PartidaIniciadaException {
        Partida partida = obtenerPartida(codigo);
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
    public List<PosicionFichaDTO> obtenerPosicionesFichas(String codigo) {
        Partida partida = obtenerPartida(codigo);
        if (esPartidaConJugadoresInvalida(partida)) {
            return List.of();
        }
        return partida.getJugadores().stream().map(this::mapearAJugadorPosicionDto).toList();
    }

    private PosicionFichaDTO mapearAJugadorPosicionDto(Jugador jugador) {
        String nombreFicha = obtenerNombreFichaSegura(jugador);
        Integer posicion =
            jugador.getPosicionActual() != null ? jugador.getPosicionActual() : POSICION_INICIAL;
        return new PosicionFichaDTO(jugador.getId(), jugador.getApodo(), nombreFicha, posicion);
    }

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador) {
        Jugador jugadorActual = obtenerJugadorActual(partida.getCodigoUnico());
        if (jugadorActual == null) {
            return;
        }
        if (iniciarTemporizador && partida.getCodigoUnico() != null) {
            gestorTemporizador.programarTemporizador(partida.getCodigoUnico(), DURACION_TURNO_SEGUNDOS, () -> avanzarTurnoDesdeTemporizador(partida.getCodigoUnico()));
        }
        if (publicadorTurno != null) {
            EventoTurnoCambiado evento = new EventoTurnoCambiado(
                partida.getCodigoUnico(),
                jugadorActual.getId(),
                jugadorActual.getApodo(),
                jugadorActual.getOrden(),
                obtenerSegundosRestantesTurno(partida.getCodigoUnico()),
                partida.getRondaActual()
            );
            publicadorTurno.notificar(evento);
        }
    }

    private Boolean esPartidaConJugadoresInvalida(Partida partida) {
        return partida == null || esListaDeJugadoresVacia(partida.getJugadores());
    }

    private Boolean esListaDeJugadoresVacia(List<Jugador> jugadores) {
        return jugadores == null || jugadores.isEmpty();
    }

    private Boolean esJugadorActivo(Jugador jugador) {
        return jugador != null && !jugador.getEnBancarrota();
    }

    private String obtenerNombreFichaSegura(Jugador jugador) {
        return jugador.getFicha() == null ? null : jugador.getFicha().name();
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
