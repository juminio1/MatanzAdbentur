package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import org.springframework.context.annotation.Lazy;
import java.util.stream.Collectors;
import com.tallerwebi.dominio.eventos.EventoGlobalFinalizado;
import com.tallerwebi.dominio.eventos.EventoGlobalActualizado;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.excepcion.TiradasFaltantesException;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service("servicioEvento")
@Transactional
public class ServicioEventoImpl implements ServicioEvento {

    private final RepositorioEvento repositorioEvento;
    private final ServicioDado servicioDado;
    private final ApplicationEventPublisher publicadorEventos;
    private final Random random;
    private final GestorTemporizadorPartida gestorTemporizador;
    private final RepositorioPartida repositorioPartida;
    private ServicioGestionQuiebra servicioGestionQuiebra;

    // La dependencia es perezosa para evitar un ciclo con los observadores de quiebra y partida.
    @Autowired
    public void configurarServicioGestionQuiebra(@Lazy ServicioGestionQuiebra servicioGestionQuiebra) {
        this.servicioGestionQuiebra = servicioGestionQuiebra;
    }

    private static final Integer RONDAS_ENTRE_EVENTOS = 2; 
    private static final Integer CANTIDAD_CASILLEROS = 30;
    private static final Integer CANTIDAD_MAXIMA_JUGADORES_ELEGIDOS = 1;
    private static final Integer DURACION_EVENTO_GLOBAL_SEGUNDOS = 30;
    private static final Set<TipoEventoGlobal> EVENTOS_CON_TIRADA = Set.of(TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE, TipoEventoGlobal.TRAMITES_AFIP);
    private static final String PREFIJO_TEMPORIZADOR_EVENTO = "evento-global:";
    private static final String FASE_INICIADO = "INICIADO";
    private static final String FASE_TIRADA = "TIRADA";
    private static final String FASE_DESEMPATE = "DESEMPATE";
    private static final String FASE_EMPATE = "EMPATE";
    private static final Integer DURACION_PAUSA_EMPATE_SEGUNDOS = 8;
    private static final String PREFIJO_TEMPORIZADOR_EMPATE = "evento-global-empate:";
    private static final String FASE_FINALIZADO = "FINALIZADO";
    private static final Integer DURACION_CONFIRMACION_SEGUNDOS = 60;
    private static final String PREFIJO_TEMPORIZADOR_RESULTADO = "evento-global-resultado:";

    private final Map<String, Integer> etapaTiradasPorPartida = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Integer>> tiradasPorPartida = new ConcurrentHashMap<>();
    private final Map<String, EventoGlobal> eventosActivosPorPartida = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> participantesDesempatePorPartida = new ConcurrentHashMap<>();
    private final Set<String> partidasEnPausaPorEmpate = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> ultimaRondaConEventoPorPartida = new ConcurrentHashMap<>();
    private final Map<String, EstadoEventoGlobal> resultadosEnPresentacionPorPartida = new ConcurrentHashMap<>();
    private final RelojEventoGlobal relojEventoGlobal = new RelojEventoGlobal();
    private final RelojEventoGlobal relojPausaEmpate = new RelojEventoGlobal();
    private final RelojEventoGlobal relojResultadoGlobal = new RelojEventoGlobal();
    private final Map<String, Set<Long>> confirmacionesPorPartida = new ConcurrentHashMap<>();
    private final Map<String, Long> jugadoresMovimientoPendiente = new ConcurrentHashMap<>();
    private final Map<String, EventoGlobal> eventosMovimientoPendiente = new ConcurrentHashMap<>();

    @Autowired
    public ServicioEventoImpl(RepositorioEvento repositorioEvento, ServicioDado servicioDado, ApplicationEventPublisher publicadorEventos, GestorTemporizadorPartida gestorTemporizador, RepositorioPartida repositorioPartida) {

        this.repositorioEvento = repositorioEvento;
        this.servicioDado = servicioDado;
        this.publicadorEventos = publicadorEventos;
        this.gestorTemporizador = gestorTemporizador;
        this.repositorioPartida = repositorioPartida;
        this.random = new Random();
    }

    @Override
    public Evento obtenerEventoAleatorio() {
        return repositorioEvento.obtenerEventoAleatorio();
    }

    @Override
    public Evento obtenerEventosGlobalesAleatorio() {
        return this.repositorioEvento.obtenerEventoGlobalAleatorio();
    }

    @Override
    public void aplicarEvento(Jugador jugador, Evento evento) throws EventoInexistenteException {
        if (ValidadorEventoGlobal.esEventoInvalido(evento)) {
            throw new EventoInexistenteException("El evento no existe");
        }

        // Switch expression exhaustiva: mantiene el comportamiento y evita el conflicto de reglas de PMD.
        Runnable accion = switch (evento.getTipoEfecto()) {
            case GANAR_PLATA -> () -> jugador.setDinero(jugador.getDinero() + evento.getValor());
            case PERDER_PLATA -> () -> jugador.setDinero(jugador.getDinero() - evento.getValor());
            case PERDER_TURNO -> () -> jugador.setPierdeTurno(true);
            case MOVER_CASILLEROS -> () -> moverCasilleros(jugador, evento);
            case IR_A_CASILLERO -> () -> jugador.setPosicionActual(evento.getValor());
            case VOLVER_INICIO -> () -> jugador.setPosicionActual(0);
        };

        accion.run();
    }

    @Override
    public void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        ValidadorEventoGlobal.validarEventoGlobal(eventoGlobal);
        ValidadorEventoGlobal.validarPartida(partida);
        ValidadorEventoGlobal.validarJugadores(partida);

        if (EVENTOS_CON_TIRADA.contains(eventoGlobal.getTipoEventoGlobal())) {
            prepararTiradasEventoGlobal(partida, eventoGlobal);
        } else {
            ejecutarEventoInmediato(partida, eventoGlobal);
        }
    }

    // Reserva la pausa antes de aplicar la consecuencia: una quiebra no debe adelantar el turno en medio del evento.
    private void ejecutarEventoInmediato(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalActivoException, JugadorNoEncontradoException, EventoInexistenteException {
        String codigoPartida = partida.getCodigoUnico();
        EventoGlobal eventoAnterior = eventosActivosPorPartida.putIfAbsent(codigoPartida, eventoGlobal);

        ValidadorEventoGlobal.validarAusenciaDeEventoAnterior(eventoAnterior);

        try {
            AplicadorConsecuenciaEventoGlobal.aplicarAlAzar(partida, eventoGlobal, random, repositorioPartida,
                    servicioGestionQuiebra, this::aplicarEvento,
                    (jugador, detalle) -> publicarEstadoInmediato(partida, eventoGlobal, jugador.getId(), detalle));
        } finally {
            eventosActivosPorPartida.remove(codigoPartida, eventoGlobal);
        }
    }

    @Override
    public synchronized void verificarActivacionEventoGlobal(Partida partida) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        ValidadorEventoGlobal.validarPartidaParaActivacion(partida);

        Integer rondaActual = partida.getRondaActual();
        String codigoPartida = partida.getCodigoUnico();

      if (ValidadorEventoGlobal.correspondeActivarEnRonda(rondaActual, RONDAS_ENTRE_EVENTOS)
              && !rondaActual.equals(ultimaRondaConEventoPorPartida.get(codigoPartida))) {

            ValidadorEventoGlobal.validarQueNoExistaEventoActivo(codigoPartida, eventosActivosPorPartida);

            EventoGlobal eventoGlobal = obtenerEventoGlobalValido();

            iniciarEventoGlobal(partida, eventoGlobal);

            ultimaRondaConEventoPorPartida.put(codigoPartida, rondaActual);
        }
    }

    @Override
    public synchronized ResultadoTirada realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {

        ValidadorEventoGlobal.validarDatosEventoGlobal(partida, jugador, eventoGlobal); //valida los datos del evento que sean validos

        ValidadorEventoGlobal.validarJugadorEnPartida(partida, jugador); //valida que los jugadores pertenezcan a la partida

        ValidadorEventoGlobal.validarEventoActivo(eventosActivosPorPartida.get(partida.getCodigoUnico()), eventoGlobal); //valida que el evento exista y este activo

        ValidadorEventoGlobal.validarJugadorHabilitado(partida, jugador, participantesDesempatePorPartida); //valida que el jugador este habilitado para realizar la tirada de dados

        ValidadorEventoGlobal.validarQueNoSeaPausaEmpate(partida.getCodigoUnico(), partidasEnPausaPorEmpate);

        Map<Long, Integer> tiradas = ValidadorEventoGlobal.obtenerTiradasDePartida(partida, tiradasPorPartida);

        ValidadorEventoGlobal.validarQueNoHayaTirado(tiradas, jugador); //valida que el jugador no haya tirado

        ResultadoTirada resultado = servicioDado.tirarDados();

        ValidadorEventoGlobal.registrarTiradaEventoGlobal(tiradas, jugador, resultado.sumarDados());
        publicarEstado(partida, eventoGlobal, FASE_TIRADA, null);

        this.resolverEventoGlobal(partida);

        return resultado;
    }

    @Override
    public synchronized EstadoEventoGlobal consultarEstadoEventoGlobal(String codigoPartida) {
        completarTiradasVencidasSiCorresponde(codigoPartida);
        completarPausaEmpateVencidaSiCorresponde(codigoPartida);
        resolverSiAbandonaronParticipantes(codigoPartida);
        reanudarSiVencioPlazo(codigoPartida);
        EstadoEventoGlobal resultado = resultadosEnPresentacionPorPartida.get(codigoPartida);
        return resultado != null ? construirEstadoResultadoActual(resultado) : consultarEstadoEventoActivo(codigoPartida);
    }

    private void resolverSiAbandonaronParticipantes(String codigoPartida) {
        EventoGlobal evento = eventosActivosPorPartida.get(codigoPartida);
        if (evento != null && !partidasEnPausaPorEmpate.contains(codigoPartida)) {
            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
            Map<Long, Integer> tiradas = tiradasPorPartida.get(codigoPartida);
            if (ValidadorEventoGlobal.puedeResolverTrasAbandonos(partida, tiradas, participantesDesempatePorPartida)) {
                try {
                    resolverEventoGlobal(partida);
                } catch (DatosEnventoInvalidoExceptions | JugadorNoEncontradoException | EventoInexistenteException exception) {
                    throw new IllegalStateException("No se pudo resolver el evento tras los abandonos", exception);
                }
            }
        }
    }

    // Si el temporizador falló, la consulta de estado también puede completar las tiradas vencidas.
    private void completarTiradasVencidasSiCorresponde(String codigoPartida) {
        Integer etapa = etapaTiradasPorPartida.get(codigoPartida);
        if (ValidadorEventoGlobal.puedeCompletarTiradasVencidas(etapa,
                partidasEnPausaPorEmpate.contains(codigoPartida),
                () -> relojEventoGlobal.vencioOProgramacionAgotada(codigoPartida,
                        gestorTemporizador.obtenerSegundosRestantes(obtenerClaveTemporizadorEvento(codigoPartida))))) {
            ejecutarVencimientoEventoGlobal(codigoPartida, etapa);
        }
    }

    private void completarPausaEmpateVencidaSiCorresponde(String codigoPartida) {
        if (ValidadorEventoGlobal.puedeFinalizarPausaEmpate(partidasEnPausaPorEmpate.contains(codigoPartida),
                () -> relojPausaEmpate.vencioOProgramacionAgotada(codigoPartida,
                        gestorTemporizador.obtenerSegundosRestantes(obtenerClaveTemporizadorEmpate(codigoPartida))))) {
            iniciarDesempate(codigoPartida, etapaTiradasPorPartida.get(codigoPartida));
        }
    }

    private EstadoEventoGlobal consultarEstadoEventoActivo(String codigoPartida) {
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);
        EstadoEventoGlobal estado = null;
        if (eventoActivo != null) {
            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
            if (partida != null) {
                Integer etapa = etapaTiradasPorPartida.get(codigoPartida);
                String fase = partidasEnPausaPorEmpate.contains(codigoPartida) ? FASE_EMPATE
                        : etapa != null && etapa > 0 ? FASE_DESEMPATE : FASE_INICIADO;
                estado = construirEstadoEvento(partida, eventoActivo, fase, null);
            }
        }
        return estado;
    }

    private EstadoEventoGlobal construirEstadoResultadoActual(EstadoEventoGlobal resultado) {
        Integer segundosRestantes = relojResultadoGlobal.obtenerSegundosRestantes(resultado.codigoPartida());
        
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(resultado.codigoPartida());
        
        List<Long> activos = partida == null ? List.of() : partida.getJugadores().stream().filter(ValidadorEventoGlobal::estaActivo).map(Jugador::getId).toList();
        
        Map<Long, Integer> tiradas = resultado.tiradas().entrySet().stream().filter(entrada -> activos.contains(entrada.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        Boolean elegidoSigueActivo = activos.contains(resultado.idJugadorElegido());
        
        String detalle = elegidoSigueActivo || resultado.idJugadorElegido() == null
                ? resultado.detalle() : "El jugador seleccionado abandonó la partida; no recibirá la consecuencia.";
        
            return new EstadoEventoGlobal(resultado.codigoPartida(), resultado.fase(), resultado.tipo(), resultado.descripcion(),
            resultado.etapa(), segundosRestantes == null ? DURACION_CONFIRMACION_SEGUNDOS : segundosRestantes,
            activos, tiradas, elegidoSigueActivo ? resultado.idJugadorElegido() : null, detalle,
            resultado.posiciones().entrySet().stream().filter(entrada -> activos.contains(entrada.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            resultado.saldos().entrySet().stream().filter(entrada -> activos.contains(entrada.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            
            obtenerConfirmaciones(resultado.codigoPartida()));
    }

    @Override
    public synchronized ResultadoTirada tirarDadosEventoGlobal(String codigoPartida, Long idJugador) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        ValidadorEventoGlobal.validarPartida(partida);
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);
        if (eventoActivo == null) {
            throw new DatosEnventoInvalidoExceptions("No hay un evento global pendiente de tiradas");
        }
        Jugador jugador = ValidadorEventoGlobal.obtenerJugadorEnPartida(partida, idJugador);
        return realizarTiradaEventoGlobal(partida, jugador, eventoActivo);
    }

    @Override
    public Boolean verificarSiHayEventoGlobalActivo(String codigoUnico) {
        return ValidadorEventoGlobal.existeEventoGlobalActivo(codigoUnico, eventosActivosPorPartida, resultadosEnPresentacionPorPartida);
    }

    @Override
    public synchronized void realizarTiradasAutomaticasPendientes(Partida partida) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {

        ValidadorEventoGlobal.validarPartida(partida);

        String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);

        if (eventoActivo != null) {

            Map<Long, Integer> tiradas = ValidadorEventoGlobal.obtenerTiradasDePartida(partida, tiradasPorPartida);

            List<Jugador> participantes = ValidadorEventoGlobal.obtenerParticipantesHabilitados(partida, participantesDesempatePorPartida);

            List<Jugador> jugadoresPendientes = participantes.stream().filter(jugador -> !tiradas.containsKey(jugador.getId())).toList();
            for (Jugador jugador : jugadoresPendientes) {
                ResultadoTirada resultado = servicioDado.tirarDados();
                ValidadorEventoGlobal.registrarTiradaEventoGlobal(tiradas, jugador, resultado.sumarDados());
            }
            publicarEstado(partida, eventoActivo, FASE_TIRADA, null);
            if (!participantes.isEmpty()) {
                resolverEventoGlobal(partida);
            }
        }
    }
    

    private EventoGlobal obtenerEventoGlobalValido() throws EventoGlobalInvalidoException {
        Evento eventoAleatorio = obtenerEventosGlobalesAleatorio();

        if (!(eventoAleatorio instanceof EventoGlobal)) {
            //jeje creo q ta mal
            throw new EventoGlobalInvalidoException("No se encontró un evento global válido");
        }

        return (EventoGlobal) eventoAleatorio;
    }

    private void resolverEventoGlobal(Partida partida) throws DatosEnventoInvalidoExceptions, JugadorNoEncontradoException, EventoInexistenteException {

        String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);

        ValidadorEventoGlobal.validarEventoActivoEnResolucion(eventoActivo);

        Map<Long, Integer> tiradas = ValidadorEventoGlobal.obtenerTiradasDePartida(partida, tiradasPorPartida);

        List<Jugador> participantes = ValidadorEventoGlobal.obtenerParticipantesHabilitados(partida, participantesDesempatePorPartida);

        ValidadorEventoGlobal.validarParticipantesEnResolucion(participantes);

        Boolean faltanTiradas = participantes.stream().anyMatch(jugador -> !tiradas.containsKey(jugador.getId()));

        if (!faltanTiradas) {

            List<Jugador> jugadoresElegidos = AplicadorConsecuenciaEventoGlobal.obtenerJugadoresConResultadoElegido(participantes, tiradas, eventoActivo.getTipoEventoGlobal());

            ValidadorEventoGlobal.validarJugadoresElegidos(jugadoresElegidos);

            if (jugadoresElegidos.size() > CANTIDAD_MAXIMA_JUGADORES_ELEGIDOS) {
                prepararDesempate(partida, jugadoresElegidos);
            } else {
                Jugador elegido = jugadoresElegidos.get(0);
                String detalle = resolverConsecuenciaOReservarMovimiento(partida, elegido, eventoActivo);
                publicarEstado(partida, eventoActivo, FASE_FINALIZADO, elegido.getId(), detalle);

                finalizarEventoGlobal(partida);
            }
        }
    }

    private void moverCasilleros(Jugador jugador, Evento evento) {
        Integer nuevaPosicion = Math.floorMod(jugador.getPosicionActual() + evento.getValor(),CANTIDAD_CASILLEROS);

        jugador.setPosicionActual(nuevaPosicion);
    }

    private void prepararTiradasEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalActivoException {
         String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoAnterior = eventosActivosPorPartida.putIfAbsent(codigoPartida, eventoGlobal);

        ValidadorEventoGlobal.validarAusenciaDeEventoAnterior(eventoAnterior);

        tiradasPorPartida.put(codigoPartida, new ConcurrentHashMap<>());

        etapaTiradasPorPartida.put(codigoPartida, 0);

        programarTiempoEventoGlobal(codigoPartida, 0);

        publicarEstado(partida, eventoGlobal, FASE_INICIADO, null);
    }

    private String obtenerClaveTemporizadorEvento(String codigoPartida) { //Diferencia del termporizador de la partida con el temporizador del tirar dados por el evento
        return PREFIJO_TEMPORIZADOR_EVENTO + codigoPartida;
    }

    private void programarTiempoEventoGlobal(String codigoPartida, Integer etapa) {

        DespuesDeConfirmar.ejecutar(() -> {
            relojEventoGlobal.iniciar(codigoPartida, DURACION_EVENTO_GLOBAL_SEGUNDOS);
            gestorTemporizador.programarTemporizador(obtenerClaveTemporizadorEvento(codigoPartida), DURACION_EVENTO_GLOBAL_SEGUNDOS, () -> ejecutarVencimientoEventoGlobal(codigoPartida, etapa));
        });
    }

    private synchronized void ejecutarVencimientoEventoGlobal(String codigoPartida, Integer etapa) {

        if (ValidadorEventoGlobal.puedeEjecutarVencimiento(
                eventosActivosPorPartida.containsKey(codigoPartida),
                Objects.equals(etapa, etapaTiradasPorPartida.get(codigoPartida)),
                partidasEnPausaPorEmpate.contains(codigoPartida))) {

            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);

            if (partida == null) {
                throw new IllegalStateException( "No se encontró la partida del evento global");
            }

            realizarTiradasPendientesAlVencer(partida);
        }
    }
    
    private void realizarTiradasPendientesAlVencer(Partida partida) { //Este método captura todas las excepciones y las transforma en una IllegalStateException
        try {
            realizarTiradasAutomaticasPendientes(partida);

        } catch (PartidaNoEncontradaException | DatosEnventoInvalidoExceptions | TiradaDadoInvalidaException | JugadorNoEncontradoException | TiradasFaltantesException | EventoInexistenteException exception) {

            throw new IllegalStateException("Error al realizar las tiradas automáticas",exception);
        }
    }

    private void prepararDesempate(Partida partida, List<Jugador> jugadoresEmpatados) {
        String codigoPartida = partida.getCodigoUnico();
        Set<Long> jugadoresHabilitados = ConcurrentHashMap.newKeySet();
        for (Jugador jugador : jugadoresEmpatados) {
            jugadoresHabilitados.add(jugador.getId());
        }
        participantesDesempatePorPartida.put(codigoPartida, jugadoresHabilitados);
        partidasEnPausaPorEmpate.add(codigoPartida);
        Integer nuevaEtapa = etapaTiradasPorPartida.merge(codigoPartida, 1, Integer::sum);
        publicarEstado(partida, eventosActivosPorPartida.get(codigoPartida), FASE_EMPATE, null);

        DespuesDeConfirmar.ejecutar(() -> {
            gestorTemporizador.cancelarTemporizador(obtenerClaveTemporizadorEvento(codigoPartida));
            relojPausaEmpate.iniciar(codigoPartida, DURACION_PAUSA_EMPATE_SEGUNDOS);
            gestorTemporizador.programarTemporizador(obtenerClaveTemporizadorEmpate(codigoPartida),
                    DURACION_PAUSA_EMPATE_SEGUNDOS, () -> iniciarDesempate(codigoPartida, nuevaEtapa));
        });
    }

    private String obtenerClaveTemporizadorEmpate(String codigoPartida) {
        return PREFIJO_TEMPORIZADOR_EMPATE + codigoPartida;
    }

    private synchronized void iniciarDesempate(String codigoPartida, Integer etapa) {
        if (ValidadorEventoGlobal.puedeIniciarDesempate(
                partidasEnPausaPorEmpate.contains(codigoPartida),
                etapa, etapaTiradasPorPartida.get(codigoPartida))) {
            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
            EventoGlobal evento = eventosActivosPorPartida.get(codigoPartida);
            if (partida != null && evento != null) {
                tiradasPorPartida.get(codigoPartida).clear();
                partidasEnPausaPorEmpate.remove(codigoPartida);
                relojPausaEmpate.limpiar(codigoPartida);
                programarTiempoEventoGlobal(codigoPartida, etapa);
                publicarEstado(partida, evento, FASE_DESEMPATE, null);
            }
        }
    }

    private String resolverConsecuenciaOReservarMovimiento(Partida partida, Jugador elegido, EventoGlobal evento) throws EventoInexistenteException {
        if (AplicadorConsecuenciaEventoGlobal.modificaPosicion(evento)) {
            jugadoresMovimientoPendiente.put(partida.getCodigoUnico(), elegido.getId());
            eventosMovimientoPendiente.put(partida.getCodigoUnico(), evento);
            return AplicadorConsecuenciaEventoGlobal.previsualizarMovimiento(partida, elegido, evento);
        }
        return aplicarConsecuenciaEventoGlobal(partida, elegido, evento);
    }

    private String aplicarConsecuenciaEventoGlobal(Partida partida, Jugador jugadorElegido, EventoGlobal eventoGlobal) throws EventoInexistenteException {
        return AplicadorConsecuenciaEventoGlobal.aplicar(partida, jugadorElegido, eventoGlobal, repositorioPartida,
                servicioGestionQuiebra, this::aplicarEvento);
    }

    private EstadoEventoGlobal construirEstadoEvento(Partida partida, EventoGlobal eventoGlobal, String fase, Long elegido) {
        return construirEstadoEvento(partida, eventoGlobal, fase, elegido, null);
    }

    private EstadoEventoGlobal construirEstadoEvento(Partida partida, EventoGlobal eventoGlobal, String fase, Long elegido, String detalle) {
        String codigoPartida = partida.getCodigoUnico();
        Integer etapa = etapaTiradasPorPartida.getOrDefault(codigoPartida, 0);
        Integer segundos = FASE_EMPATE.equals(fase)
                ? gestorTemporizador.obtenerSegundosRestantes(obtenerClaveTemporizadorEmpate(codigoPartida))
                : gestorTemporizador.obtenerSegundosRestantes(obtenerClaveTemporizadorEvento(codigoPartida));
        segundos = relojEventoGlobal.obtenerSegundosConRespaldo(codigoPartida, FASE_EMPATE.equals(fase),
                segundos, FASE_EMPATE.equals(fase) ? DURACION_PAUSA_EMPATE_SEGUNDOS : DURACION_EVENTO_GLOBAL_SEGUNDOS);
        List<Long> habilitados = ValidadorEventoGlobal.obtenerParticipantesHabilitados(partida, participantesDesempatePorPartida).stream().map(Jugador::getId).filter(Objects::nonNull).toList();
        Map<Long, Integer> tiradas = tiradasPorPartida.getOrDefault(codigoPartida, Map.of()).entrySet().stream()
                .filter(entrada -> habilitados.contains(entrada.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Long, Integer> posiciones = partida.getJugadores().stream().filter(ValidadorEventoGlobal::estaActivo)
                .filter(ValidadorEventoGlobal::tienePosicionRegistrable).collect(Collectors.toMap(Jugador::getId, Jugador::getPosicionActual));
        Map<Long, Integer> saldos = partida.getJugadores().stream().filter(ValidadorEventoGlobal::estaActivo)
                .filter(ValidadorEventoGlobal::tieneSaldoRegistrable).collect(Collectors.toMap(Jugador::getId, Jugador::getDinero));
        return new EstadoEventoGlobal(codigoPartida, fase, eventoGlobal.getTipoEventoGlobal(), eventoGlobal.getDescripcion(), etapa,
                FASE_FINALIZADO.equals(fase) ? DURACION_CONFIRMACION_SEGUNDOS : segundos,
                List.copyOf(habilitados), Map.copyOf(tiradas), elegido,
                detalle, posiciones, saldos);
    }

    private void publicarEstado(Partida partida, EventoGlobal eventoGlobal, String fase, Long jugadorElegido) {
        publicarEstado(partida, eventoGlobal, fase, jugadorElegido, null);
    }

    private void publicarEstado(Partida partida, EventoGlobal eventoGlobal, String fase, Long jugadorElegido, String detalle) {
        EstadoEventoGlobal snapshot = construirEstadoEvento(partida, eventoGlobal, fase, jugadorElegido, detalle);
        if (Set.of(FASE_INICIADO, FASE_DESEMPATE, FASE_EMPATE).contains(fase)) {
            snapshot = new EstadoEventoGlobal(snapshot.codigoPartida(), snapshot.fase(), snapshot.tipo(), snapshot.descripcion(),
                    snapshot.etapa(), FASE_EMPATE.equals(fase) ? DURACION_PAUSA_EMPATE_SEGUNDOS : DURACION_EVENTO_GLOBAL_SEGUNDOS,
                    snapshot.participantes(), snapshot.tiradas(), snapshot.idJugadorElegido(),
                    snapshot.detalle(), snapshot.posiciones(), snapshot.saldos());
        }
        publicarSnapshot(snapshot);
    }

    private void publicarEstadoInmediato(Partida partida, EventoGlobal eventoGlobal, Long idJugadorElegido, String detalle) {
        EstadoEventoGlobal snapshot = construirEstadoEvento(partida, eventoGlobal, FASE_FINALIZADO, idJugadorElegido, detalle);
        publicarSnapshot(snapshot);
    }

    private void publicarSnapshot(EstadoEventoGlobal snapshot) {
        if (FASE_FINALIZADO.equals(snapshot.fase())) {
            resultadosEnPresentacionPorPartida.put(snapshot.codigoPartida(), snapshot);
            confirmacionesPorPartida.put(snapshot.codigoPartida(), ConcurrentHashMap.newKeySet());
            DespuesDeConfirmar.ejecutar(() -> programarTiempoDeConfirmacion(snapshot.codigoPartida()));
        }
        DespuesDeConfirmar.ejecutar(() -> publicadorEventos.publishEvent(new EventoGlobalActualizado(snapshot)));
    }

    private void programarTiempoDeConfirmacion(String codigoPartida) {
        relojResultadoGlobal.iniciar(codigoPartida, DURACION_CONFIRMACION_SEGUNDOS);
        gestorTemporizador.programarTemporizador(PREFIJO_TEMPORIZADOR_RESULTADO + codigoPartida,
                DURACION_CONFIRMACION_SEGUNDOS, () -> reanudarSiVencioPlazo(codigoPartida));
    }

    private synchronized void reanudarSiVencioPlazo(String codigoPartida) {
        if (resultadosEnPresentacionPorPartida.containsKey(codigoPartida)) {
            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
            if (ValidadorEventoGlobal.puedeReanudarResultado(partida, codigoPartida, servicioGestionQuiebra,
                    relojResultadoGlobal.vencio(codigoPartida),
                    confirmacionesPorPartida.getOrDefault(codigoPartida, Set.of()))) {
                reanudarPartidaTrasEvento(codigoPartida);
            }
        }
    }

    private List<Long> obtenerConfirmaciones(String codigoPartida) {
        return List.copyOf(confirmacionesPorPartida.getOrDefault(codigoPartida, Set.of()));
    }

    @Override
    public synchronized void confirmarContinuacion(String codigoPartida, Long idJugador) {
        ValidadorEventoGlobal.validarResultadoPendiente(codigoPartida, resultadosEnPresentacionPorPartida);
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        ValidadorEventoGlobal.validarConfirmacion(partida, idJugador);
        confirmacionesPorPartida.computeIfAbsent(codigoPartida, codigo -> ConcurrentHashMap.newKeySet()).add(idJugador);
        Set<Long> confirmados = confirmacionesPorPartida.get(codigoPartida);
        if (ValidadorEventoGlobal.puedeReanudarResultado(partida, codigoPartida, servicioGestionQuiebra,
                false, confirmados)) {
            reanudarPartidaTrasEvento(codigoPartida);
        } else {
            EstadoEventoGlobal estado = construirEstadoResultadoActual(resultadosEnPresentacionPorPartida.get(codigoPartida));
            DespuesDeConfirmar.ejecutar(() -> publicadorEventos.publishEvent(new EventoGlobalActualizado(estado)));
        }
    }

    @Override
    public synchronized void reanudarPartidaTrasEvento(String codigoPartida) {
        ValidadorEventoGlobal.validarResultadoPendiente(codigoPartida, resultadosEnPresentacionPorPartida);
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        ValidadorEventoGlobal.validarReanudacion(partida, codigoPartida, servicioGestionQuiebra);
        aplicarMovimientoReservado(codigoPartida, partida);
        resultadosEnPresentacionPorPartida.remove(codigoPartida);
        confirmacionesPorPartida.remove(codigoPartida);
        relojResultadoGlobal.limpiar(codigoPartida);
        DespuesDeConfirmar.ejecutar(() -> {
            gestorTemporizador.cancelarTemporizador(PREFIJO_TEMPORIZADOR_RESULTADO + codigoPartida);
            publicadorEventos.publishEvent(new EventoGlobalFinalizado(codigoPartida));
        });
    }

    private void aplicarMovimientoReservado(String codigoPartida, Partida partida) {
        Long idJugador = jugadoresMovimientoPendiente.remove(codigoPartida);
        EventoGlobal evento = eventosMovimientoPendiente.remove(codigoPartida);
        if (ValidadorEventoGlobal.hayMovimientoPendiente(idJugador, evento)) {
            partida.getJugadores().stream()
                    .filter(jugador -> idJugador.equals(jugador.getId()))
                    .filter(ValidadorEventoGlobal::estaActivo)
                    .findFirst().ifPresent(jugador -> aplicarMovimientoConfirmado(partida, jugador, evento));
        }
    }

    private void aplicarMovimientoConfirmado(Partida partida, Jugador jugador, EventoGlobal evento) {
        try {
            aplicarConsecuenciaEventoGlobal(partida, jugador, evento);
        } catch (EventoInexistenteException exception) {
            throw new IllegalStateException("No se pudo aplicar el movimiento confirmado", exception);
        }
    }

    private void limpiarEstadoEventoGlobal(String codigoPartida) {

        relojEventoGlobal.limpiar(codigoPartida);
        relojPausaEmpate.limpiar(codigoPartida);

        eventosActivosPorPartida.remove(codigoPartida);

        tiradasPorPartida.remove(codigoPartida);

        participantesDesempatePorPartida.remove(codigoPartida);
        partidasEnPausaPorEmpate.remove(codigoPartida);

        etapaTiradasPorPartida.remove(codigoPartida);
        DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(obtenerClaveTemporizadorEmpate(codigoPartida)));

        DespuesDeConfirmar.ejecutar(() ->
            gestorTemporizador.cancelarTemporizador(
                obtenerClaveTemporizadorEvento(codigoPartida)
            )
        );
    }

    private void finalizarEventoGlobal(Partida partida) {
        String codigoPartida = partida.getCodigoUnico();

        limpiarEstadoEventoGlobal(codigoPartida);

        // La reanudación se notifica cuando vence la presentación del resultado.
    }
}
