package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.EstadoCarcel;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.dominio.resultados.PosicionFicha;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTiempoAgotadoEnCarcel;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;

import java.util.List;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

//  Coordino las operaciones de la partida.
@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final Integer POSICION_INICIAL = 0;
    private static final Integer DURACION_TURNO_SEGUNDOS = 30;

    private final RepositorioPartida repositorioPartida;
    private final ServicioInicioPartida servicioInicioPartida;
    private final ServicioTurno servicioTurno;
    private final PublicadorEventos<EventoTurnoCambiado> publicadorTurno;
    private final GestorTemporizadorPartida gestorTemporizador;
    private final PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono;
    // Se usan cuando a un jugador preso se le acaba el tiempo (pueden ser null en los tests).
    private final ServicioGestionQuiebra quiebras;
    private final PublicadorEventos<EventoTiempoAgotadoEnCarcel> publicadorCarcel;
    private final ServicioFinalizacionPartida finalizacion;
    private final TransactionTemplate lecturaTurno;
    @Autowired
    private com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion participacion;
    @Autowired(required = false)
    private com.tallerwebi.dominio.servicios.interfaces.ServicioEvento eventosGlobales;

    public ServicioPartidaImpl(
            RepositorioPartida repositorioPartida,
            ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador,
            ServicioTurno servicioTurno,
            PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono
    ) {
        this(repositorioPartida, servicioInicioPartida, gestorTemporizador, servicioTurno,
                publicadorTurno, publicadorAbandono, null, null, null, null);
    }

    @Autowired
    public ServicioPartidaImpl(
            RepositorioPartida repositorioPartida,
            ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador,
            ServicioTurno servicioTurno,
            @Autowired(required = false) PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono,
            @Lazy ServicioGestionQuiebra quiebras,
            @Autowired(required = false) PublicadorEventos<EventoTiempoAgotadoEnCarcel> publicadorCarcel,
            ServicioFinalizacionPartida finalizacion,
            PlatformTransactionManager transactionManager
    ) {
        this.repositorioPartida = repositorioPartida;
        this.servicioInicioPartida = servicioInicioPartida;
        this.servicioTurno = servicioTurno;
        this.gestorTemporizador = gestorTemporizador;
        this.publicadorTurno = publicadorTurno;
        this.publicadorAbandono = publicadorAbandono;
        this.quiebras = quiebras;
        this.publicadorCarcel = publicadorCarcel;
        this.finalizacion = finalizacion;
        this.lecturaTurno = new TransactionTemplate(transactionManager);
        this.lecturaTurno.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lecturaTurno.setReadOnly(true);
    }

    //Devuelve partidas que todavía no finalizaron (EN_ESPERA o EN_CURSO).
    @Override
    @Transactional(readOnly = true)
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    //Devuelve partidas de cualquier estado, incluso FINALIZADA.
    @Override
    @Transactional(readOnly = true)
    public Partida obtenerPartidaParaVisualizacion(String codigoPartida) {
        return repositorioPartida.buscarPartidaPorCodigoUnico(codigoPartida);
    }

    @Override
    public void actualizarJugador(Jugador jugador) {
        repositorioPartida.actualizarJugador(jugador);
    }

    @Override
    public void bloquearPartida(Partida partida) {
        repositorioPartida.bloquearPartida(partida);
    }

    @Override
    public void incrementarRondas(Partida partida) {
        servicioTurno.incrementarRondas(partida);
    }

    @Override
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }

        servicioTurno.asignarOrdenInicialJugadores(partida);
        partida.getJugadores().forEach(jugador -> jugador.setEnBancarrota(false));
        partida.setIndiceTurnoActual(servicioTurno.obtenerIndiceTurnoActual(partida));
        repositorioPartida.guardarPartida(partida);
    }

    @Override
    public Jugador obtenerJugadorActual(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return null;
        }

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        return servicioTurno.obtenerJugadorActual(partida);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSegundosRestantesTurno(String codigoUnico) {
        return gestorTemporizador.obtenerSegundosRestantes(codigoUnico);
    }

    @Override
    public void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida == null) return;
        repositorioPartida.bloquearPartida(partida);
        if (partida.getEventoGlobal().estaActivo()) return;
        participacion.validarPartidaEnCurso(partida);
        Jugador pendiente = servicioTurno.obtenerJugadorActual(partida);
        if (pendiente == null || Boolean.TRUE.equals(pendiente.getPierdeTurno())) {
            reanudarDespuesDeEvento(codigoUnico);
            return;
        }
        MarcaTurno marca = MarcaTurno.de(partida);
        Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(
                codigoUnico,
                DURACION_TURNO_SEGUNDOS,
                () -> avanzarTurnoSiSigueVigente(codigoUnico, marca)
        );
        if (iniciado) notificarTurnoActual(partida, false);
    }

    @Override
    public Jugador obtenerJugadorPorId(Long idJugador) {
        return repositorioPartida.buscarJugadorPorId(idJugador);
    }

    @Override
    public void prepararTiempoTurno(String codigoUnico, Integer segundosPresentacion) {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida == null) return;
        if (partida.getEventoGlobal().estaActivo()) return;
        notificarTurnoActual(partida, true, DURACION_TURNO_SEGUNDOS + segundosPresentacion);
    }

    @Override
    public void avanzarTurno(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return;
        }
        repositorioPartida.bloquearPartida(partida);
        participacion.validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo()) return;
        DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigoUnico));
        Integer indiceActual = servicioTurno.obtenerIndiceTurnoActual(partida);
        incrementarRondas(partida);
        if (finalizacion.finalizarSiCorresponde(partida)) {
            return;
        }

        Jugador candidato = servicioTurno.resolverSiguienteJugador(partida, indiceActual);
        if (finalizacion.finalizarSiCorresponde(partida)) {
            return;
        }
        if (candidato == null) {
            repositorioPartida.guardarPartida(partida);
            DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigoUnico));
            return;
        }

        candidato.setTiroEnTurno(false);
        partida.setIndiceTurnoActual(candidato.getOrden());
        repositorioPartida.guardarPartida(partida);
        if (eventosGlobales != null && eventosGlobales.activarSiCorresponde(partida)) return;
        notificarTurnoActual(partida, true);
    }

    @Override
    public void reanudarDespuesDeEvento(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida == null) return;
        repositorioPartida.bloquearPartida(partida);
        if (partida.getEventoGlobal().estaActivo() || finalizacion.finalizarSiCorresponde(partida)) return;
        Jugador jugadorPendiente = servicioTurno.obtenerJugadorActual(partida);
        if (jugadorPendiente == null || Boolean.TRUE.equals(jugadorPendiente.getPierdeTurno())) {
            // El turno elegido antes de pausar no se puede volver a habilitar si recibió
            // la penalización del Sarmiento. Consumimos exactamente esa oportunidad.
            Integer indicePendiente = servicioTurno.obtenerIndiceTurnoActual(partida);
            incrementarRondas(partida);
            if (jugadorPendiente != null) jugadorPendiente.setPierdeTurno(false);
            Jugador siguiente = servicioTurno.resolverSiguienteJugador(partida, indicePendiente);
            if (siguiente == null || finalizacion.finalizarSiCorresponde(partida)) return;
            siguiente.setTiroEnTurno(false);
            partida.setIndiceTurnoActual(siguiente.getOrden());
        }
        repositorioPartida.guardarPartida(partida);
        notificarTurnoActual(partida, true);
    }

    @Override
    public void iniciarPartida(String codigo, Long idUsuario)
            throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException,
            CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException,
            SalaNoEncontradaException, FichaNoDisponibleException {
        Partida partida = servicioInicioPartida.iniciarPartida(codigo, idUsuario);
        DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigo));
        notificarTurnoActual(partida, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosicionFicha> obtenerPosicionesFichas(String codigo) {
        Partida partida = obtenerPartida(codigo);
        if (esPartidaConJugadoresInvalida(partida)) {
            return List.of();
        }
        return partida.getJugadores().stream().map(this::mapearAJugadorPosicionDto).toList();
    }

    @Override
    public void abandonarPartida(String codigoUnico, Long idJugador) throws PartidaNoEncontradaException {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida == null) {
            throw new PartidaNoEncontradaException();
        }
        participacion.validarPartidaEnCurso(partida);
        repositorioPartida.bloquearPartida(partida);
        Jugador jugador = obtenerJugadorPorId(idJugador);
        if (jugador == null) {
            throw new IllegalArgumentException("El jugador especificado no existe.");
        }

        finalizacion.conservarResultadoEconomico(partida, jugador, EstadoParticipante.ABANDONADO);
        jugador.setAbandono(true);
        actualizarJugador(jugador);

        // Disparamos el evento con la seguridad de que todo existe
        publicadorAbandono.notificar(new EventoJugadorAbandonado(partida, jugador));
    }

    private PosicionFicha mapearAJugadorPosicionDto(Jugador jugador) {
        String nombreFicha = obtenerNombreFichaSegura(jugador);
        Integer posicion = jugador.getPosicionActual() != null ? jugador.getPosicionActual() : POSICION_INICIAL;
        return new PosicionFicha(jugador.getId(), jugador.getApodo(), nombreFicha, posicion);
    }

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador) {
        notificarTurnoActual(partida, iniciarTemporizador, DURACION_TURNO_SEGUNDOS);
    }

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador, Integer segundosTotales) {
        Jugador jugadorActual = obtenerJugadorActual(partida.getCodigoUnico());
        if (jugadorActual == null) {
            return;
        }
        MarcaTurno marca = MarcaTurno.de(partida);
        String codigo = partida.getCodigoUnico();
        DespuesDeConfirmar.ejecutar(() -> lecturaTurno.executeWithoutResult(estado ->
                notificarTurnoSiSigueVigente(codigo, marca, iniciarTemporizador, segundosTotales)));
    }

    private void notificarTurnoSiSigueVigente(String codigo, MarcaTurno marca,
                                             Boolean iniciarTemporizador, Integer segundosTotales) {
        Partida partida = obtenerPartida(codigo);
        if (!turnoSigueVigente(partida, marca)) {
            return;
        }
        Jugador jugadorActual = servicioTurno.obtenerJugadorActual(partida);
        if (jugadorActual == null) {
            return;
        }
        if (iniciarTemporizador && codigo != null) {
            gestorTemporizador.programarTemporizador(
                    partida.getCodigoUnico(),
                    segundosTotales,
                    () -> avanzarTurnoSiSigueVigente(codigo, marca)
            );
        }
        if (publicadorTurno != null) {
            EventoTurnoCambiado evento = new EventoTurnoCambiado(
                    partida.getCodigoUnico(),
                    jugadorActual.getId(),
                    jugadorActual.getApodo(),
                    jugadorActual.getOrden(),
                    obtenerSegundosRestantesTurno(partida.getCodigoUnico()),
                    partida.getRondaActual(),
                    partida.getTurnosFinalizadosEnRonda()
            );
            publicadorTurno.notificar(evento);
        }
    }

    private boolean turnoSigueVigente(Partida partida, MarcaTurno marca) {
        return partida != null && partida.getEstado() == EstadoPartida.EN_CURSO
                && !partida.getEventoGlobal().estaActivo()
                && marca.equals(MarcaTurno.de(partida));
    }

    /*
     * Lo usa el temporizador al vencer. Si el turno que se programó ya terminó
     * (por ejemplo, el jugador lo cerró justo antes), no avanza de nuevo.
     */
    private void avanzarTurnoSiSigueVigente(String codigoUnico, MarcaTurno marcaProgramada) {
        Partida partida = obtenerPartida(codigoUnico);
        repositorioPartida.bloquearPartida(partida);
        if (!turnoSigueVigente(partida, marcaProgramada)) {
            return;
        }
        registrarTiempoAgotadoEnCarcel(codigoUnico);

        // Si cobrar la fianza terminó en quiebra, el turno ya lo avanzó el observador de quiebra.
        Partida actualizada = obtenerPartida(codigoUnico);
        if (!turnoSigueVigente(actualizada, marcaProgramada)) {
            return;
        }
        avanzarTurno(codigoUnico);
    }

    /*
     * Si al jugador del turno se le acabó el tiempo estando preso y sin tirar, ese turno
     * cuenta como intento fallido. Al último intento se le cobra la fianza y queda libre
     * para su próximo turno (igual que cuando tira y no saca dobles).
     */
    private void registrarTiempoAgotadoEnCarcel(String codigoUnico) {
        Jugador jugador = obtenerJugadorActual(codigoUnico);
        if (jugador == null
                || !Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado())
                || Boolean.TRUE.equals(jugador.getTiroEnTurno())) {
            return;
        }

        boolean usoTodosLosIntentos = jugador.getEstadoCarcel().registrarIntentoFallido();
        // Con una deuda pendiente no se puede cobrar otra; la fianza se cobra cuando vuelva a tirar.
        boolean puedeCobrarFianza = quiebras != null
                && quiebras.obtenerDeudaPendiente(codigoUnico, jugador.getId()) == null;
        if (usoTodosLosIntentos && puedeCobrarFianza) {
            quiebras.procesarMulta(codigoUnico, jugador, EstadoCarcel.COSTO_FIANZA, "FIANZA_CARCEL");
            jugador.getEstadoCarcel().liberar();
        }
        actualizarJugador(jugador);

        if (publicadorCarcel != null) {
            EventoTiempoAgotadoEnCarcel evento = new EventoTiempoAgotadoEnCarcel(
                    codigoUnico,
                    jugador.getId(),
                    jugador.getEstadoCarcel().getEncarcelado(),
                    jugador.getEstadoCarcel().getIntentosSalirCarcel(),
                    jugador.getDinero());
            DespuesDeConfirmar.ejecutar(() -> publicadorCarcel.notificar(evento));
        }
    }

    // Identifica un turno concreto: cambia cada vez que se avanza el turno.
    private record MarcaTurno(Integer indiceTurno, Integer ronda, Integer turnosFinalizadosEnRonda) {
        static MarcaTurno de(Partida partida) {
            return new MarcaTurno(partida.getIndiceTurnoActual(), partida.getRondaActual(),
                    partida.getTurnosFinalizadosEnRonda());
        }
    }

    private Boolean esPartidaConJugadoresInvalida(Partida partida) {
        return partida == null || esListaDeJugadoresVacia(partida.getJugadores());
    }

    private Boolean esListaDeJugadoresVacia(List<Jugador> jugadores) {
        return jugadores == null || jugadores.isEmpty();
    }

    private String obtenerNombreFichaSegura(Jugador jugador) {
        return jugador.getFicha() == null ? null : jugador.getFicha().name();
    }

}
