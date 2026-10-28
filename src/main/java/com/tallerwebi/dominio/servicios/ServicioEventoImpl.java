package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private static final Integer RONDAS_ENTRE_EVENTOS = 5;
    private static final Integer CANTIDAD_CASILLEROS = 30;
    private static final Integer CANTIDAD_MAXIMA_JUGADORES_ELEGIDOS = 1;
    private static final Integer DURACION_EVENTO_GLOBAL_SEGUNDOS = 15;
    private static final String PREFIJO_TEMPORIZADOR_EVENTO = "evento-global:";
    private static final String FASE_INICIADO = "INICIADO";
    private static final String FASE_TIRADA = "TIRADA";
    private static final String FASE_DESEMPATE = "DESEMPATE";
    private static final String FASE_FINALIZADO = "FINALIZADO";

    private final Map<String, Integer> etapaTiradasPorPartida = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Integer>> tiradasPorPartida = new ConcurrentHashMap<>();
    private final Map<String, EventoGlobal> eventosActivosPorPartida = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> participantesDesempatePorPartida = new ConcurrentHashMap<>();
    private final Map<String, Integer> ultimaRondaConEventoPorPartida = new ConcurrentHashMap<>();

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
        if (this.esEventoInvalido(evento)) {
            throw new EventoInexistenteException("El evento no existe");
        }

        switch (evento.getTipoEfecto()) {
            case GANAR_PLATA:
                jugador.setDinero(jugador.getDinero() + evento.getValor());
                break;
            case PERDER_PLATA:
                jugador.setDinero(jugador.getDinero() - evento.getValor());
                break;
            case PERDER_TURNO:
                jugador.setPierdeTurno(true);
                break;
            case MOVER_CASILLEROS:
                this.moverCasilleros(jugador, evento);
                break;
            case IR_A_CASILLERO:
            case VOLVER_INICIO:
                jugador.setPosicionActual(evento.getValor());
                break;
        }
    }

    @Override
    public void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        validarEventoGlobal(eventoGlobal);
        validarPartida(partida);
        validarJugadores(partida);

        if (requiereTiradaDeDados(eventoGlobal)) {
            this.prepararTiradasEventoGlobal(partida, eventoGlobal);
        }

        this.eventosDeSeleccionDeJugadorAleatorio(partida, eventoGlobal);
    }

    @Override
    public synchronized void verificarActivacionEventoGlobal(Partida partida) throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        validarPartidaParaActivacion(partida);

        Integer rondaActual = partida.getRondaActual();
        String codigoPartida = partida.getCodigoUnico();

      if (activarEventoGlobalPorRondas(rondaActual) && !yaSeActivoEventoEnEstaRonda(codigoPartida, rondaActual)) {

            validarQueNoExistaEventoActivo(codigoPartida);

            EventoGlobal eventoGlobal = obtenerEventoGlobalValido();

            iniciarEventoGlobal(partida, eventoGlobal);

            ultimaRondaConEventoPorPartida.put(codigoPartida, rondaActual);
        }
    }

    @Override
    public synchronized ResultadoTirada realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {

        this.validarDatosEventoGlobal(partida, jugador, eventoGlobal); //valida los datos del evento que sean validos

        this.validarJugadorEnPartida(partida, jugador); //valida que los jugadores pertenezcan a la partida

        this.validarEventoActivo(partida, eventoGlobal); //valida que el evento exista y este activo

        this.validarJugadorHabilitado(partida, jugador); //valida que el jugador este habilitado para realizar la tirada de dados

        Map<Long, Integer> tiradas = obtenerTiradasDePartida(partida);

        this.validarQueNoHayaTirado(tiradas, jugador); //valida que el jugador no haya tirado

        ResultadoTirada resultado = servicioDado.tirarDados();

        this.registrarTiradaEventoGlobal(tiradas, jugador, resultado.sumarDados());
        publicarEstado(partida, eventoGlobal, FASE_TIRADA, null);

        this.resolverEventoGlobal(partida);

        return resultado;
    }

    @Override
    public synchronized EstadoEventoGlobal consultarEstadoEventoGlobal(String codigoPartida) {
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);
        EstadoEventoGlobal estado = null;
        if (eventoActivo != null) {
            Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
            if (partida != null) {
                Integer etapa = etapaTiradasPorPartida.get(codigoPartida);
                String fase = etapa != null && etapa > 0 ? FASE_DESEMPATE : FASE_INICIADO;
                estado = construirEstadoEvento(partida, eventoActivo, fase, null);
            }
        }
        return estado;
    }

    @Override
    public synchronized ResultadoTirada tirarDadosEventoGlobal(String codigoPartida, Long idJugador) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        validarPartida(partida);
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);
        if (eventoActivo == null) {
            throw new DatosEnventoInvalidoExceptions("No hay un evento global pendiente de tiradas");
        }
        if (idJugador == null || partida.getJugadores() == null) {
            throw new JugadorNoEncontradoException("No se identificó un participante válido");
        }
        Jugador jugador = partida.getJugadores().stream().filter(participante -> idJugador.equals(participante.getId())).findFirst().orElseThrow(() -> new JugadorNoEncontradoException("El jugador no pertenece a la partida"));
        return realizarTiradaEventoGlobal(partida, jugador, eventoActivo);
    }

    @Override
    public Boolean verificarSiHayEventoGlobalActivo(String codigoUnico) {
        return codigoUnico != null && eventosActivosPorPartida.containsKey(codigoUnico);
    }

    @Override
    public synchronized void realizarTiradasAutomaticasPendientes(Partida partida) throws PartidaNoEncontradaException, DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException, TiradasFaltantesException, EventoInexistenteException {

        validarPartida(partida);

        String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoActivo = eventosActivosPorPartida.get(codigoPartida);

        if (eventoActivo != null) {

            Map<Long, Integer> tiradas = obtenerTiradasDePartida(partida);

            List<Jugador> participantes = obtenerParticipantesHabilitados(partida);

            List<Jugador> jugadoresPendientes = participantes.stream().filter(jugador -> !tiradas.containsKey(jugador.getId())).toList();

            for (Jugador jugador : jugadoresPendientes) {
                realizarTiradaEventoGlobal(partida, jugador, eventoActivo);
            }
        }
    }
    

    private void validarEventoGlobal(EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException {
        if (esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new EventoGlobalInvalidoException("El evento global es inválido");
        }
    }

    private void validarPartida(Partida partida) throws PartidaNoEncontradaException {
        if (partida == null || partida.getCodigoUnico() == null) {
            throw new PartidaNoEncontradaException();
        }
    }

    private void validarJugadores(Partida partida) throws JugadorNoEncontradoException {
        if (partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
            throw new JugadorNoEncontradoException( "No hay jugadores para participar del evento global");
        }
    }

    private void validarPartidaParaActivacion(Partida partida) throws PartidaNoEncontradaException {
        if (partida == null || partida.getCodigoUnico() == null || partida.getCodigoUnico().isBlank()) {
            throw new PartidaNoEncontradaException();
        }
    }

    private Boolean yaSeActivoEventoEnEstaRonda(String codigoPartida, Integer rondaActual) {
        Integer ultimaRonda = ultimaRondaConEventoPorPartida.get(codigoPartida);
        return rondaActual.equals(ultimaRonda);
    }

    private void validarQueNoExistaEventoActivo(String codigoPartida) throws EventoGlobalActivoException {
        if (eventosActivosPorPartida.containsKey(codigoPartida)) {
            throw new EventoGlobalActivoException(
                "Ya existe un evento global pendiente de resolución"
            );
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

    if (eventoActivo == null) {
        throw new DatosEnventoInvalidoExceptions("No existe un evento global activo para esta partida");
    }

    Map<Long, Integer> tiradas = obtenerTiradasDePartida(partida);

    List<Jugador> participantes = obtenerParticipantesHabilitados(partida);

    if (participantes.isEmpty()) {
        throw new JugadorNoEncontradoException("No hay jugadores habilitados para participar");
    }

    Boolean faltanTiradas = participantes.stream().anyMatch(jugador -> !tiradas.containsKey(jugador.getId()));

    if (!faltanTiradas) {

        List<Jugador> jugadoresElegidos = obtenerJugadoresConResultadoElegido(participantes, tiradas, eventoActivo.getTipoEventoGlobal());

        if (jugadoresElegidos.isEmpty()) {
            throw new JugadorNoEncontradoException("No se pudo determinar un jugador para resolver el evento");
        }

        if (jugadoresElegidos.size() > CANTIDAD_MAXIMA_JUGADORES_ELEGIDOS) {
            prepararDesempate(partida, jugadoresElegidos);
        } else {
            aplicarConsecuenciaEventoGlobal(partida, jugadoresElegidos.get(0), eventoActivo);
            publicarEstado(partida, eventoActivo, FASE_FINALIZADO, jugadoresElegidos.get(0).getId());

            finalizarEventoGlobal(partida);
        }
    }
}

    private void validarDatosEventoGlobal( Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions {
        if (partida == null || jugador == null || esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new DatosEnventoInvalidoExceptions("Datos del evento inválidos");
        }

        if (partida.getCodigoUnico() == null || partida.getJugadores() == null || jugador.getId() == null) {
            throw new DatosEnventoInvalidoExceptions("La partida o el jugador son inválidos");
        }
    }

    private void validarJugadorEnPartida(Partida partida, Jugador jugador) throws DatosEnventoInvalidoExceptions, JugadorNoEncontradoException {
        Boolean perteneceAPartida = partida.getJugadores().stream().anyMatch(participante -> participante.getId().equals(jugador.getId()));

        if (!perteneceAPartida) {
            throw new JugadorNoEncontradoException("El jugador no pertenece a la partida");
        }
    }

    private void validarEventoActivo(Partida partida, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions {
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(partida.getCodigoUnico());

        if (eventoActivo == null || eventoActivo.getId() == null || !eventoActivo.getId().equals(eventoGlobal.getId())) {
            throw new DatosEnventoInvalidoExceptions("El evento global no está activo");
        }
    }

    private Map<Long, Integer> obtenerTiradasDePartida(Partida partida) throws DatosEnventoInvalidoExceptions {
        Map<Long, Integer> tiradas = tiradasPorPartida.get(partida.getCodigoUnico());

        if (tiradas == null) {
            throw new DatosEnventoInvalidoExceptions("No hay tiradas para esta partida");
        }

        return tiradas;
    }

    private void validarQueNoHayaTirado(Map<Long, Integer> tiradas, Jugador jugador) throws TiradaDadoInvalidaException {
        if (tiradas.get(jugador.getId()) != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    private void registrarTiradaEventoGlobal(Map<Long, Integer> tiradas, Jugador jugador, Integer sumaDados) throws TiradaDadoInvalidaException {
        if (sumaDados == null || sumaDados < 2 || sumaDados > 12) {
            throw new TiradaDadoInvalidaException("La tirada de dados es inválida");
        }

        Integer tiradaAnterior = tiradas.putIfAbsent(jugador.getId(), sumaDados);

        if (tiradaAnterior != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    private void moverCasilleros(Jugador jugador, Evento evento) {
        Integer nuevaPosicion = Math.floorMod(jugador.getPosicionActual() + evento.getValor(),CANTIDAD_CASILLEROS);

        jugador.setPosicionActual(nuevaPosicion);
    }

    private void prepararTiradasEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalActivoException {
         String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoAnterior = eventosActivosPorPartida.putIfAbsent(codigoPartida, eventoGlobal);

        if (eventoAnterior != null) {
            throw new EventoGlobalActivoException("Ya existe un evento global activo");
        }

        tiradasPorPartida.put(codigoPartida, new ConcurrentHashMap<>());

        etapaTiradasPorPartida.put(codigoPartida, 0);

        programarTiempoEventoGlobal(codigoPartida, 0);

        publicarEstado(partida, eventoGlobal, FASE_INICIADO, null);
    }

    private String obtenerClaveTemporizadorEvento(String codigoPartida) { //Diferencia del termporizador de la partida con el temporizador del tirar dados por el evento
        return PREFIJO_TEMPORIZADOR_EVENTO + codigoPartida;
    }

    private void programarTiempoEventoGlobal(String codigoPartida, Integer etapa) {

        DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.programarTemporizador( obtenerClaveTemporizadorEvento(codigoPartida), DURACION_EVENTO_GLOBAL_SEGUNDOS, 
        () -> ejecutarVencimientoEventoGlobal(codigoPartida, etapa)));
    }

    private synchronized void ejecutarVencimientoEventoGlobal(String codigoPartida, Integer etapa) {

        Boolean eventoActivo = eventosActivosPorPartida.containsKey(codigoPartida);

        Boolean mismaEtapa = etapa.equals(etapaTiradasPorPartida.get(codigoPartida));

        if (eventoActivo && mismaEtapa) {

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

    private Boolean requiereTiradaDeDados(EventoGlobal eventoGlobal) {
        Boolean inundacion = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.INUNDACION);

        Boolean piquete = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.PIQUETE);

        Boolean afip = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.TRAMITES_AFIP);

        return inundacion || piquete || afip;
    }

    //Eventos con jugador aleatorio

    private void eventosDeSeleccionDeJugadorAleatorio(Partida partida, EventoGlobal eventoGlobal) throws JugadorNoEncontradoException, EventoInexistenteException {
        List<Jugador> jugadores = partida.getJugadores();

        Boolean elKevin = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.EL_KEVIN);
        Boolean fiat600 = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.FIAT_600);
        Boolean sarmiento = eventoGlobal
            .getTipoEventoGlobal()
            .equals(TipoEventoGlobal.ACCIDENTE_SARMIENTO);

        if (elKevin || fiat600 || sarmiento) {
            this.seleccionarJugadorYAplicarEvento(partida, jugadores, eventoGlobal);
        }
    }

    private void seleccionarJugadorYAplicarEvento(Partida partida, List<Jugador> jugadores, EventoGlobal eventoGlobal) throws JugadorNoEncontradoException, EventoInexistenteException {
        if (jugadores == null || jugadores.isEmpty()) {
            throw new JugadorNoEncontradoException("No hay jugadores en la partida");
        }

        Integer indiceAleatorio = random.nextInt(jugadores.size());

        Jugador jugadorElegido = jugadores.get(indiceAleatorio);

        this.aplicarEvento(jugadorElegido, eventoGlobal);
        publicarEstadoInmediato(partida, eventoGlobal, jugadorElegido.getId());
    }

    private Boolean esEventoInvalido(Evento evento) {
        return evento == null || evento.getTipoEfecto() == null;
    }

    private Boolean activarEventoGlobalPorRondas(Integer rondaActual) {
        return rondaActual != null && rondaActual > 0 && rondaActual % RONDAS_ENTRE_EVENTOS == 0;
    }

    private List<Jugador> obtenerJugadoresConResultadoElegido(List<Jugador> participantes, Map<Long, Integer> tiradas, TipoEventoGlobal tipoEvento) {
        //El map relaciona el id de cada jugador con el resultado de la tirada

        if (tiradas.isEmpty()) {
            return List.of();
        }

        Integer resultadoElegido =
            tipoEvento == TipoEventoGlobal.PIQUETE
                ? Collections.min(tiradas.values())
                : Collections.max(tiradas.values()); //Buscan el número más grande o más chico de una colección

        return participantes
            .stream()
            .filter(jugador -> resultadoElegido.equals(tiradas.get(jugador.getId())))
            .toList();
    }

    private void prepararDesempate(Partida partida, List<Jugador> jugadoresEmpatados) {
        String codigoPartida = partida.getCodigoUnico();

        Set<Long> jugadoresHabilitados = ConcurrentHashMap.newKeySet(); //se guarda los ids de los jugadores que empataron

        for (Jugador jugador : jugadoresEmpatados) {
            jugadoresHabilitados.add(jugador.getId());
        }

        participantesDesempatePorPartida.put(codigoPartida, jugadoresHabilitados);

        tiradasPorPartida.get(codigoPartida).clear();

        Integer nuevaEtapa = etapaTiradasPorPartida.merge(codigoPartida, 1, Integer::sum);

        programarTiempoEventoGlobal(codigoPartida, nuevaEtapa); //Cada ver que los jugadores empatan, se reinicia el tiempo de tirada 

        publicarEstado(partida, eventosActivosPorPartida.get(codigoPartida), FASE_DESEMPATE, null);
    }

    private List<Jugador> obtenerParticipantesHabilitados(Partida partida) {
        Set<Long> idsHabilitados = participantesDesempatePorPartida.get(partida.getCodigoUnico());

        if (idsHabilitados == null) {
            return partida.getJugadores();
        }

        return partida.getJugadores().stream().filter(jugador -> idsHabilitados.contains(jugador.getId())).toList(); //solo devuelve los jugadores que pueden tirar
    }

    private void validarJugadorHabilitado(Partida partida, Jugador jugador) throws TiradaDadoInvalidaException {
        //valida que un jugador pueda tirar durante el desempate

        Set<Long> idsHabilitados = participantesDesempatePorPartida.get(partida.getCodigoUnico());

        if (idsHabilitados != null && !idsHabilitados.contains(jugador.getId())) {
            throw new TiradaDadoInvalidaException("El jugador no participa del desempate");
        }
    }

    private void aplicarConsecuenciaEventoGlobal(Partida partida, Jugador jugadorElegido, EventoGlobal eventoGlobal) throws EventoInexistenteException {
        if (eventoGlobal.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP) {
            Integer posicionMasAdelantada = partida.getJugadores().stream().mapToInt(Jugador::getPosicionActual).max().orElse(jugadorElegido.getPosicionActual());
            jugadorElegido.setPosicionActual(posicionMasAdelantada);
        } else {
            this.aplicarEvento(jugadorElegido, eventoGlobal);
        }
    }

    private EstadoEventoGlobal construirEstadoEvento(Partida partida, EventoGlobal eventoGlobal, String fase, Long elegido) {
        String codigoPartida = partida.getCodigoUnico();
        Integer etapa = etapaTiradasPorPartida.getOrDefault(codigoPartida, 0);
        Integer segundos = gestorTemporizador.obtenerSegundosRestantes(obtenerClaveTemporizadorEvento(codigoPartida));
        if (segundos == null) {
            segundos = DURACION_EVENTO_GLOBAL_SEGUNDOS;
        }
        Map<Long, Integer> tiradas = tiradasPorPartida.getOrDefault(codigoPartida, Map.of());
        List<Long> habilitados = obtenerParticipantesHabilitados(partida).stream().map(Jugador::getId).filter(id -> id != null).toList();
        return new EstadoEventoGlobal(codigoPartida, fase, eventoGlobal.getTipoEventoGlobal(), eventoGlobal.getDescripcion(), etapa,
                FASE_FINALIZADO.equals(fase) ? 0 : segundos, List.copyOf(habilitados), Map.copyOf(tiradas), elegido);
    }

    private void publicarEstado(Partida partida, EventoGlobal eventoGlobal, String fase, Long jugadorElegido) {
        EstadoEventoGlobal snapshot = construirEstadoEvento(partida, eventoGlobal, fase, jugadorElegido);
        if (FASE_INICIADO.equals(fase) || FASE_DESEMPATE.equals(fase)) {
            snapshot = new EstadoEventoGlobal(snapshot.codigoPartida(), snapshot.fase(), snapshot.tipo(), snapshot.descripcion(),
                    snapshot.etapa(), DURACION_EVENTO_GLOBAL_SEGUNDOS, snapshot.participantes(), snapshot.tiradas(), snapshot.idJugadorElegido());
        }
        publicarSnapshot(snapshot);
    }

    private void publicarEstadoInmediato(Partida partida, EventoGlobal eventoGlobal, Long idJugadorElegido) {
        List<Long> participantes = idJugadorElegido == null ? List.of() : List.of(idJugadorElegido);
        EstadoEventoGlobal snapshot = new EstadoEventoGlobal(partida.getCodigoUnico(), FASE_FINALIZADO,
                eventoGlobal.getTipoEventoGlobal(), eventoGlobal.getDescripcion(), 0, 0, participantes, Map.of(), idJugadorElegido);
        publicarSnapshot(snapshot);
    }

    private void publicarSnapshot(EstadoEventoGlobal snapshot) {
        DespuesDeConfirmar.ejecutar(() -> publicadorEventos.publishEvent(new EventoGlobalActualizado(snapshot)));
    }

    private void limpiarEstadoEventoGlobal(String codigoPartida) {

        eventosActivosPorPartida.remove(codigoPartida);

        tiradasPorPartida.remove(codigoPartida);

        participantesDesempatePorPartida.remove(codigoPartida);

        etapaTiradasPorPartida.remove(codigoPartida);

        DespuesDeConfirmar.ejecutar(() ->
            gestorTemporizador.cancelarTemporizador(
                obtenerClaveTemporizadorEvento(codigoPartida)
            )
        );
    }

    private void finalizarEventoGlobal(Partida partida) {
        String codigoPartida = partida.getCodigoUnico();

        limpiarEstadoEventoGlobal(codigoPartida);

        DespuesDeConfirmar.ejecutar(() -> publicadorEventos.publishEvent(new EventoGlobalFinalizado(codigoPartida)));
    }
}
