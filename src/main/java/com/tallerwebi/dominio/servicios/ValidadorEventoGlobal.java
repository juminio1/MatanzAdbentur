package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;

import java.util.Map;
import java.util.Set;

// Concentra las validaciones de eventos globales sin depender de Spring ni del estado de la partida.
public final class ValidadorEventoGlobal {

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
        if (partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
            throw new JugadorNoEncontradoException("No hay jugadores para participar del evento global");
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
        Set<Long> idsHabilitados = participantesDesempatePorPartida.get(partida.getCodigoUnico());

        if (idsHabilitados != null && !idsHabilitados.contains(jugador.getId())) {
            throw new TiradaDadoInvalidaException("El jugador no participa del desempate");
        }
    }

    public static Boolean esEventoInvalido(Evento evento) {
        return evento == null || evento.getTipoEfecto() == null;
    }
}
