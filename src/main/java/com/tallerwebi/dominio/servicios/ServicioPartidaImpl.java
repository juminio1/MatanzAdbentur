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

import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;



import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;

import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;

import com.tallerwebi.dominio.excepcion.EventoInexistenteException;


import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;




import com.tallerwebi.dominio.publicadores.PublicadorEventos;


import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;

import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;

import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;

import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;


import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;


import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;

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

    private RepositorioDeudaPendiente repositorioDeudaPendiente;

    @Autowired

    public void configurarRepositorioDeudaPendiente(RepositorioDeudaPendiente repositorioDeudaPendiente) {

        this.repositorioDeudaPendiente = repositorioDeudaPendiente;

    }

    private final ServicioInicioPartida servicioInicioPartida;

    private final ServicioTurno servicioTurno;

    private final Map<String, Thread> cambiosDeTurnoEnCurso = new ConcurrentHashMap<>();

    private final ServicioEvento servicioEvento;

    private final PublicadorEventos<EventoTurnoCambiado> publicadorTurno;

    private final GestorTemporizadorPartida gestorTemporizador;

    private final PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono;
    // Se usan cuando a un jugador preso se le acaba el tiempo (pueden ser null en los tests).
    private final ServicioGestionQuiebra quiebras;
    private final PublicadorEventos<EventoTiempoAgotadoEnCarcel> publicadorCarcel;
    private final ServicioFinalizacionPartida finalizacion;
    private final TransactionTemplate lecturaTurno;
    @Autowired
    private com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion participacion = new ServicioParticipacionImpl();

    public ServicioPartidaImpl(
            RepositorioPartida repositorioPartida,
            ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador,
            ServicioTurno servicioTurno,
            PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono
    ) {
        this(repositorioPartida, servicioInicioPartida, gestorTemporizador, servicioTurno,
                publicadorTurno, publicadorAbandono, null, null, null, null, null);
    }

    public ServicioPartidaImpl(
            RepositorioPartida repositorioPartida,
            ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador,
            ServicioTurno servicioTurno,
            PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono,
            ServicioEvento servicioEvento
    ) {
        this(repositorioPartida, servicioInicioPartida, gestorTemporizador, servicioTurno,
                publicadorTurno, publicadorAbandono, servicioEvento, null, null, null, null);
    }

    public ServicioPartidaImpl(RepositorioPartida repositorioPartida, ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador, ServicioTurno servicioTurno,
            PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono, ServicioEvento servicioEvento,
            ServicioGestionQuiebra quiebras, PublicadorEventos<EventoTiempoAgotadoEnCarcel> publicadorCarcel) {
        this(repositorioPartida, servicioInicioPartida, gestorTemporizador, servicioTurno, publicadorTurno,
                publicadorAbandono, servicioEvento, quiebras, publicadorCarcel, null, null);
    }

    public ServicioPartidaImpl(RepositorioPartida repositorioPartida, ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador, ServicioTurno servicioTurno,
            PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono, ServicioGestionQuiebra quiebras,
            PublicadorEventos<EventoTiempoAgotadoEnCarcel> publicadorCarcel,
            ServicioFinalizacionPartida finalizacion, PlatformTransactionManager transactionManager) {
        this(repositorioPartida, servicioInicioPartida, gestorTemporizador, servicioTurno, publicadorTurno,
                publicadorAbandono, null, quiebras, publicadorCarcel, finalizacion, transactionManager);
    }

    @Autowired

    public ServicioPartidaImpl(
            RepositorioPartida repositorioPartida,
            ServicioInicioPartida servicioInicioPartida,
            GestorTemporizadorPartida gestorTemporizador,
            ServicioTurno servicioTurno,
            @Autowired(required = false) PublicadorEventos<EventoTurnoCambiado> publicadorTurno,
            PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono,
            ServicioEvento servicioEvento,
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

        this.servicioEvento = servicioEvento;

        this.quiebras = quiebras;
        this.publicadorCarcel = publicadorCarcel;
        this.finalizacion = finalizacion;
        this.lecturaTurno = new TransactionTemplate();
        lecturaTurno.setTransactionManager(transactionManager);
        if (transactionManager != null) {
            lecturaTurno.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            lecturaTurno.setReadOnly(true);
        }
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

    @Transactional(propagation = Propagation.REQUIRES_NEW) //Permite iniciar una una transcacción nueva cuando el obvservador solicite reanudar la partida 

    public void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico) {

        Partida partida = obtenerPartida(codigoUnico);

        if (esPartidaConJugadoresInvalida(partida)) return;
        participacion.validarPartidaEnCurso(partida);
        if (!verificarSiHayEventoGlobalActivo(codigoUnico)

                && !tieneDeudasPendientes(partida)) {

            omitirTurnoPenalizadoTrasEvento(partida);

            if (verificarSiHayEventoGlobalActivo(codigoUnico)) return;
            MarcaTurno marca = MarcaTurno.de(partida);
            Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(codigoUnico, DURACION_TURNO_SEGUNDOS, () -> avanzarTurnoSiSigueVigente(codigoUnico, marca));

            if (Boolean.TRUE.equals(iniciado)) {

                notificarTurnoActual(partida, false);

            }

        }

    }

    // Si la consecuencia alcanzó al jugador que quedaba seleccionado, no debe recibir el reloj normal.

    private void omitirTurnoPenalizadoTrasEvento(Partida partida) {

        Jugador actual = servicioTurno.obtenerJugadorActual(partida);

        // Si abandonó durante la pausa, su turno tampoco puede recibir el reloj de reanudación.
        if (actual == null || Boolean.TRUE.equals(actual.getPierdeTurno())) {

            Integer rondaAnterior = Objects.requireNonNullElse(partida.getRondaActual(), 1);
            if (actual != null) actual.setPierdeTurno(false);
            // main recalcula la ronda también si el seleccionado abandonó durante la pausa.
            incrementarRondas(partida);

            Jugador siguiente = servicioTurno.resolverSiguienteJugador(partida, servicioTurno.obtenerIndiceTurnoActual(partida));

            if (siguiente != null) {

                siguiente.setTiroEnTurno(false);

                partida.setIndiceTurnoActual(siguiente.getOrden());

            }

            repositorioPartida.guardarPartida(partida);
            if (!rondaAnterior.equals(partida.getRondaActual())) verificarEventoGlobalAlCambiarRonda(partida);
        }

    }

    @Override

    public Jugador obtenerJugadorPorId(Long idJugador) {

        return repositorioPartida.buscarJugadorPorId(idJugador);

    }

    @Override

    public void prepararTiempoTurno(String codigoUnico, Integer segundosPresentacion) {

        Partida partida = obtenerPartida(codigoUnico);

        if (!esPartidaConJugadoresInvalida(partida) && !verificarSiHayEventoGlobalActivo(codigoUnico)
                && !tieneDeudasPendientes(partida)) {
            notificarTurnoActual(partida, true, DURACION_TURNO_SEGUNDOS + segundosPresentacion);
        }

    }

    @Override

    public void avanzarTurno(String codigoUnico) {

        ejecutarCambioDeTurno(codigoUnico, () -> avanzarTurnoActual(codigoUnico));

    }

    // La reserva dura hasta finalizar la transacción, para no admitir otro cierre con una vista anterior.
    private void ejecutarCambioDeTurno(String codigoUnico, Runnable accion) {
        if (codigoUnico == null) {
            accion.run();
            return;
        }
        Thread actual = Thread.currentThread();
        Thread enCurso = cambiosDeTurnoEnCurso.putIfAbsent(codigoUnico, actual);
        if (enCurso != null) {
            // Conserva el avance interno de los observadores que participan de la misma operación.
            if (enCurso.equals(actual)) accion.run();
            return;
        }
        DespuesDeConfirmar.ejecutarConConfirmacion(accion, () -> { },
                () -> cambiosDeTurnoEnCurso.remove(codigoUnico, actual));
    }

    private void verificarEventoGlobalSiCambioRonda(Partida partida, Integer rondaAnterior) {
        if (!rondaAnterior.equals(partida.getRondaActual())) verificarEventoGlobalAlCambiarRonda(partida);
    }

    private boolean finalizarSiCorresponde(Partida partida) {
        return partida.getEstado() == EstadoPartida.FINALIZADA
                || finalizacion != null && finalizacion.finalizarSiCorresponde(partida);
    }

    private void avanzarTurnoActual(String codigoUnico) {

        Partida partida = obtenerPartida(codigoUnico);

        if (!esPartidaConJugadoresInvalida(partida) && !verificarSiHayEventoGlobalActivo(codigoUnico) && !tieneDeudasPendientes(partida)) {

            participacion.validarPartidaEnCurso(partida);
            DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigoUnico));

            Integer indiceActual = servicioTurno.obtenerIndiceTurnoActual(partida);

            Integer rondaAnterior = Objects.requireNonNullElse(partida.getRondaActual(), 1);

            incrementarRondas(partida);

            if (finalizarSiCorresponde(partida)) return;
            verificarEventoGlobalSiCambioRonda(partida, rondaAnterior);
            // ROBO puede finalizar la partida durante la activación: no se busca otro turno en ese caso.
            if (finalizarSiCorresponde(partida)) return;
            Integer rondaAntesDeResolver = partida.getRondaActual();
            Jugador candidato = servicioTurno.resolverSiguienteJugador(partida, indiceActual);

            if (candidato == null) {
                repositorioPartida.guardarPartida(partida);
                DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigoUnico));
                return;
            }
            candidato.setTiroEnTurno(false);
            partida.setIndiceTurnoActual(candidato.getOrden());

            repositorioPartida.guardarPartida(partida);
            // Las oportunidades perdidas también pueden completar una ronda según main.
            verificarEventoGlobalSiCambioRonda(partida, rondaAntesDeResolver);

            if (!verificarSiHayEventoGlobalActivo(codigoUnico)) {

                notificarTurnoActual(partida, true);

            }

        }

    }

    @Override

    public void iniciarPartida(String codigo, Long idUsuario)

        throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {

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

    public void abandonarPartida(String codigoUnico, Long idJugador)

        throws PartidaNoEncontradaException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {

        Partida partida = obtenerPartida(codigoUnico);

        if (partida == null) {

            throw new PartidaNoEncontradaException();

        }
        participacion.validarPartidaEnCurso(partida);
        Jugador jugador = obtenerJugadorPorId(idJugador);

        if (jugador == null) {

            throw new IllegalArgumentException("El jugador especificado no existe.");

        }

        if (finalizacion != null) finalizacion.conservarResultadoEconomico(partida, jugador, EstadoParticipante.ABANDONADO);
        jugador.setAbandono(true);

        actualizarJugador(jugador);

        // Disparamos el evento con la seguridad de que todo existe

        publicadorAbandono.notificar(new EventoJugadorAbandonado(partida, jugador));

    }

    @Override

    public Boolean verificarSiHayEventoGlobalActivo(String codigoUnico) {

        return servicioEvento != null && Boolean.TRUE.equals(servicioEvento.verificarSiHayEventoGlobalActivo(codigoUnico));

    }

    private PosicionFicha mapearAJugadorPosicionDto(Jugador jugador) {

        String nombreFicha = obtenerNombreFichaSegura(jugador);

        Integer posicion =

            jugador.getPosicionActual() != null ? jugador.getPosicionActual() : POSICION_INICIAL;

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
        DespuesDeConfirmar.ejecutar(() -> {
            if (lecturaTurno.getTransactionManager() == null) notificarTurnoSiSigueVigente(codigo, marca, iniciarTemporizador, segundosTotales);
            else lecturaTurno.executeWithoutResult(estado ->
                    notificarTurnoSiSigueVigente(codigo, marca, iniciarTemporizador, segundosTotales));
        });
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
            publicarTurnoCambiado(evento);
        }
    }

    private void publicarTurnoCambiado(EventoTurnoCambiado evento) {
        try {
            publicadorTurno.notificar(evento);
        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException exception) {
            throw new IllegalStateException("Error al notificar el cambio de turno", exception);
        }
    }

    private Boolean tieneDeudasPendientes(Partida partida) {

        return repositorioDeudaPendiente != null && partida.getJugadores().stream()

                .filter(ValidadorEventoGlobal::estaActivo)

                .anyMatch(jugador -> repositorioDeudaPendiente.buscarPorPartidaYJugador(

                        partida.getCodigoUnico(), jugador.getId()) != null);

    }

    private boolean turnoSigueVigente(Partida partida, MarcaTurno marca) {
        return partida != null && partida.getEstado() == EstadoPartida.EN_CURSO
                && !verificarSiHayEventoGlobalActivo(partida.getCodigoUnico())
                && !tieneDeudasPendientes(partida) && marca.equals(MarcaTurno.de(partida));
    }

    /*
     * Lo usa el temporizador al vencer. Si el turno que se programó ya terminó
     * (por ejemplo, el jugador lo cerró justo antes), no avanza de nuevo.
     */
    private void avanzarTurnoSiSigueVigente(String codigoUnico, MarcaTurno marcaProgramada) {

        ejecutarCambioDeTurno(codigoUnico, () -> avanzarTurnoProgramado(codigoUnico, marcaProgramada));

    }

    private void avanzarTurnoProgramado(String codigoUnico, MarcaTurno marcaProgramada) {

        Partida partida = obtenerPartida(codigoUnico);
        if (!turnoSigueVigente(partida, marcaProgramada)) {
            return;

        }
        try {
            registrarTiempoAgotadoEnCarcel(codigoUnico);
        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException
                | EventoGlobalActivoException | EventoInexistenteException exception) {
            throw new IllegalStateException("Error al procesar el vencimiento del turno en cárcel", exception);
        }

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
    private void registrarTiempoAgotadoEnCarcel(String codigoUnico)
            throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException,
            EventoGlobalActivoException, EventoInexistenteException {
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
            DespuesDeConfirmar.ejecutar(() -> {
                try {
                    publicadorCarcel.notificar(evento);
                } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException
                        | EventoGlobalActivoException | EventoInexistenteException exception) {
                    throw new IllegalStateException("Error al notificar el tiempo agotado en cárcel", exception);
                }
            });
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

    private void verificarEventoGlobalAlCambiarRonda(Partida partida) {

        try {

            if (servicioEvento != null) servicioEvento.verificarActivacionEventoGlobal(partida);

        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException excption) {

            throw new IllegalStateException("Error al verificar el evento global de la partida", excption); //esta exception es para no ocultar los horrores, y se propaga como error de ejecución y la transacción puede revertirse. 

        }

    }

}
