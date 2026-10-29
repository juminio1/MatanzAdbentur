package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

// Concentra las validaciones de eventos globales sin depender de Spring ni del estado de la partida.
public final class ValidadorEventoGlobal {

    public static void validarConfirmacion(Partida partida, Long idJugador) {
        if (partida == null || idJugador == null || partida.getJugadores().stream()
                .noneMatch(jugador -> idJugador.equals(jugador.getId())
                        && !Boolean.TRUE.equals(jugador.getEnBancarrota())
                        && !Boolean.TRUE.equals(jugador.getAbandono()))) {
            throw new IllegalStateException("El jugador no está habilitado para confirmar el evento.");
        }
    }

    public static boolean confirmaronTodosLosActivos(Partida partida, Set<Long> confirmados) {
        return partida.getJugadores().stream()
                .filter(jugador -> !Boolean.TRUE.equals(jugador.getEnBancarrota())
                        && !Boolean.TRUE.equals(jugador.getAbandono()))
                .allMatch(jugador -> confirmados.contains(jugador.getId()));
    }

    public static boolean tieneDeudasPendientes(Partida partida, String codigoPartida,
            com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra gestionQuiebra) {
        return gestionQuiebra != null && partida.getJugadores().stream()
                .filter(ValidadorEventoGlobal::estaActivo).anyMatch(jugador ->
                gestionQuiebra.obtenerDeudaPendiente(codigoPartida, jugador.getId()) != null);
    }

    public static void validarQueNoSeaPausaEmpate(String codigoPartida, Set<String> partidasEnPausaPorEmpate) throws DatosEnventoInvalidoExceptions {
        if (partidasEnPausaPorEmpate.contains(codigoPartida)) {
            throw new DatosEnventoInvalidoExceptions("Hay un empate en presentación. Esperá al próximo desempate.");
        }
    }

    public static Jugador obtenerJugadorEnPartida(Partida partida, Long idJugador) throws JugadorNoEncontradoException {
        if (idJugador == null || partida.getJugadores() == null) {
            throw new JugadorNoEncontradoException("No se identificó un participante válido");
        }
        return partida.getJugadores().stream()
                .filter(participante -> idJugador.equals(participante.getId()))
                .findFirst().orElseThrow(() -> new JugadorNoEncontradoException("El jugador no pertenece a la partida"));
    }

    public static void validarEventoActivoEnResolucion(EventoGlobal eventoActivo) throws DatosEnventoInvalidoExceptions {
        if (eventoActivo == null) {
            throw new DatosEnventoInvalidoExceptions("No existe un evento global activo para esta partida");
        }
    }

    public static void validarParticipantesEnResolucion(List<Jugador> participantes) throws JugadorNoEncontradoException {
        if (participantes.isEmpty()) {
            throw new JugadorNoEncontradoException("No hay jugadores habilitados para participar");
        }
    }

    public static void validarJugadoresElegidos(List<Jugador> jugadoresElegidos) throws JugadorNoEncontradoException {
        if (jugadoresElegidos.isEmpty()) {
            throw new JugadorNoEncontradoException("No se pudo determinar un jugador para resolver el evento");
        }
    }

    public static boolean puedeCompletarTiradasVencidas(Integer etapa, boolean enPausaPorEmpate, BooleanSupplier plazoVencido) {
        return etapa != null && !enPausaPorEmpate && plazoVencido.getAsBoolean();
    }

    public static boolean puedeFinalizarPausaEmpate(boolean enPausaPorEmpate, BooleanSupplier plazoVencido) {
        return enPausaPorEmpate && plazoVencido.getAsBoolean();
    }

    public static boolean puedeEjecutarVencimiento(boolean hayEventoActivo, boolean esMismaEtapa, boolean enPausaPorEmpate) {
        return hayEventoActivo && esMismaEtapa && !enPausaPorEmpate;
    }

    public static boolean puedeIniciarDesempate(boolean enPausaPorEmpate, Integer etapa, Integer etapaActual) {
        return enPausaPorEmpate && etapa != null && etapa.equals(etapaActual);
    }

    public static boolean puedeReanudarResultado(Partida partida, String codigoPartida, ServicioGestionQuiebra gestionQuiebra,
            boolean vencioPlazo, Set<Long> confirmaciones) {
        return partida != null && !tieneDeudasPendientes(partida, codigoPartida, gestionQuiebra)
                && (vencioPlazo || confirmaronTodosLosActivos(partida, confirmaciones));
    }

    public static void validarResultadoPendiente(String codigoPartida, Map<String, EstadoEventoGlobal> resultados) {
        if (!resultados.containsKey(codigoPartida)) {
            throw new IllegalStateException("No hay un resultado global esperando confirmación.");
        }
    }

    public static void validarReanudacion(Partida partida, String codigoPartida, ServicioGestionQuiebra gestionQuiebra) {
        if (partida == null) {
            throw new IllegalStateException("No se encontró la partida.");
        }
        if (tieneDeudasPendientes(partida, codigoPartida, gestionQuiebra)) {
            throw new IllegalStateException("Hay deudas pendientes. Primero deben resolverse.");
        }
    }


    public static void validarAusenciaDeEventoAnterior(EventoGlobal eventoAnterior) throws EventoGlobalActivoException {
        if (eventoAnterior != null) {
            throw new EventoGlobalActivoException("Ya existe un evento global activo");
        }
    }

    public static boolean puedeResolverTrasAbandonos(Partida partida, Map<Long, Integer> tiradas,
            Map<String, Set<Long>> participantesDesempatePorPartida) {
        boolean hayDatos = partida != null && tiradas != null;
        if (!hayDatos) {
            return false;
        }
        List<Jugador> habilitados = obtenerParticipantesHabilitados(partida, participantesDesempatePorPartida);
        return !habilitados.isEmpty() && habilitados.stream().allMatch(jugador -> tiradas.containsKey(jugador.getId()));
    }

    public static boolean tienePosicionRegistrable(Jugador jugador) {
        return jugador.getId() != null && jugador.getPosicionActual() != null;
    }

    public static boolean tieneSaldoRegistrable(Jugador jugador) {
        return jugador.getId() != null && jugador.getDinero() != null;
    }

    public static boolean hayMovimientoPendiente(Long idJugador, EventoGlobal evento) {
        return idJugador != null && evento != null;
    }

    private ValidadorEventoGlobal() {
    }

    public static void validarEventoGlobal(EventoGlobal eventoGlobal) throws EventoGlobalInvalidoException {
        if (esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new EventoGlobalInvalidoException("El evento global es inválido");
        }
    }

    public static void validarPartida(Partida partida) throws PartidaNoEncontradaException {
        if (partida == null || partida.getCodigoUnico() == null) {
            throw new PartidaNoEncontradaException();
        }
    }

    public static void validarJugadores(Partida partida) throws JugadorNoEncontradoException {
        if (partida.getJugadores() == null || partida.getJugadores().stream().noneMatch(ValidadorEventoGlobal::estaActivo)) {
            throw new JugadorNoEncontradoException("No hay jugadores activos para participar del evento global");
        }
    }

    public static void validarPartidaParaActivacion(Partida partida) throws PartidaNoEncontradaException {
        if (partida == null || partida.getCodigoUnico() == null || partida.getCodigoUnico().isBlank()) {
            throw new PartidaNoEncontradaException();
        }
    }

    public static void validarQueNoExistaEventoActivo(String codigoPartida, Map<String, EventoGlobal> eventosActivosPorPartida) throws EventoGlobalActivoException {
        if (eventosActivosPorPartida.containsKey(codigoPartida)) {
            throw new EventoGlobalActivoException("Ya existe un evento global pendiente de resolución");
        }
    }

    public static void validarDatosEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions {
        if (partida == null || jugador == null || esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new DatosEnventoInvalidoExceptions("Datos del evento inválidos");
        }

        if (partida.getCodigoUnico() == null || partida.getJugadores() == null || jugador.getId() == null) {
            throw new DatosEnventoInvalidoExceptions("La partida o el jugador son inválidos");
        }
    }

    public static void validarJugadorEnPartida(Partida partida, Jugador jugador) throws DatosEnventoInvalidoExceptions, JugadorNoEncontradoException {
        Boolean perteneceAPartida = partida.getJugadores().stream().anyMatch(participante -> participante.getId().equals(jugador.getId()));

        if (!perteneceAPartida) {
            throw new JugadorNoEncontradoException("El jugador no pertenece a la partida");
        }
    }

    public static void validarEventoActivo(EventoGlobal eventoActivo, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions {
        if (eventoActivo == null || eventoActivo.getId() == null || !eventoActivo.getId().equals(eventoGlobal.getId())) {
            throw new DatosEnventoInvalidoExceptions("El evento global no está activo");
        }
    }

    public static Map<Long, Integer> obtenerTiradasDePartida(Partida partida, Map<String, Map<Long, Integer>> tiradasPorPartida) throws DatosEnventoInvalidoExceptions {
        Map<Long, Integer> tiradas = tiradasPorPartida.get(partida.getCodigoUnico());

        if (tiradas == null) {
            throw new DatosEnventoInvalidoExceptions("No hay tiradas para esta partida");
        }

        return tiradas;
    }

    public static void validarQueNoHayaTirado(Map<Long, Integer> tiradas, Jugador jugador) throws TiradaDadoInvalidaException {
        if (tiradas.get(jugador.getId()) != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    public static void registrarTiradaEventoGlobal(Map<Long, Integer> tiradas, Jugador jugador, Integer sumaDados) throws TiradaDadoInvalidaException {
        if (sumaDados == null || sumaDados < 2 || sumaDados > 12) {
            throw new TiradaDadoInvalidaException("La tirada de dados es inválida");
        }

        Integer tiradaAnterior = tiradas.putIfAbsent(jugador.getId(), sumaDados);

        if (tiradaAnterior != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    public static void validarJugadorHabilitado(Partida partida, Jugador jugador, Map<String, Set<Long>> participantesDesempatePorPartida) throws TiradaDadoInvalidaException {
        if (!estaActivo(jugador) || obtenerParticipantesHabilitados(partida, participantesDesempatePorPartida).stream()
                .noneMatch(participante -> participante.getId().equals(jugador.getId()))) {
            throw new TiradaDadoInvalidaException("El jugador ya no participa del evento global");
        }
    }

    public static boolean estaActivo(Jugador jugador) {
        return jugador != null && !Boolean.TRUE.equals(jugador.getAbandono())
                && !Boolean.TRUE.equals(jugador.getEnBancarrota());
    }

    public static boolean correspondeActivarEnRonda(Integer ronda, Integer intervalo) {
        return ronda != null && ronda > 0 && ronda % intervalo == 0;
    }

    public static List<Jugador> obtenerParticipantesHabilitados(Partida partida, Map<String, Set<Long>> participantesDesempate) {
        Set<Long> habilitados = participantesDesempate.get(partida.getCodigoUnico());
        return partida.getJugadores().stream()
                .filter(ValidadorEventoGlobal::estaActivo)
                .filter(jugador -> habilitados == null || habilitados.contains(jugador.getId()))
                .toList();
    }

    public static boolean existeEventoGlobalActivo(String codigoPartida, Map<String, EventoGlobal> eventosActivos,
                                                   Map<String, EstadoEventoGlobal> resultadosEnPresentacion) {
        return codigoPartida != null && (eventosActivos.containsKey(codigoPartida)
                || resultadosEnPresentacion.containsKey(codigoPartida));
    }

    public static void validarDeudasAntesDeReanudar(Partida partida, ServicioGestionQuiebra servicioGestionQuiebra) {
        if (servicioGestionQuiebra != null && partida.getJugadores().stream().anyMatch(jugador ->
                servicioGestionQuiebra.obtenerDeudaPendiente(partida.getCodigoUnico(), jugador.getId()) != null)) {
            throw new IllegalStateException("Hay deudas pendientes. Primero deben resolverse.");
        }
    }

    public static Boolean esEventoInvalido(Evento evento) {
        return evento == null || evento.getTipoEfecto() == null;
    }
}
