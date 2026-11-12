package com.tallerwebi.presentacion.mapeadores;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.GestorTriviaPartida;
import com.tallerwebi.presentacion.DTO.EstadoTriviaDTO;
import com.tallerwebi.presentacion.DTO.ParticipanteTriviaDTO;
import com.tallerwebi.presentacion.DTO.PreguntaTriviaDTO;
import java.util.List;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.ServicioPremioTriviaImpl;
import java.util.ArrayList;
import java.util.Map;
import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;

// Convierte los datos de la trivia en lo que se envía al navegador.
public final class MapeadorTrivia {

    private MapeadorTrivia() {
    }

    public static Map<String, Object> cierre(EventoTriviaActualizada evento) {
        List<ParticipanteTriviaDTO> eliminados = new ArrayList<>();
        for (Jugador jugador : evento.eliminados()) {
            eliminados.add(new ParticipanteTriviaDTO(jugador.getId(), jugador.getApodo(),
                    evento.trivia().getTiempoAcumuladoMilisegundos(jugador)));
        }
        return Map.of("tipo", evento.tipo(),
                "rondaCerrada", evento.rondaCerrada(),
                "opcionCorrecta", evento.opcionCorrecta(),
                "eliminados", List.copyOf(eliminados),
                "estado", estado(evento.codigoPartida(), evento.trivia()));
    }

    public static PreguntaTriviaDTO desdePregunta(PreguntaGenerada pregunta, Integer ronda, Integer totalRondas,
                                                  Long venceEn) {
        if (pregunta == null) {
            throw new IllegalArgumentException("No hay una pregunta para mostrar.");
        }
        List<String> opciones = pregunta.getOpciones() == null ? List.of() : List.copyOf(pregunta.getOpciones());
        return new PreguntaTriviaDTO(ronda, ronda, totalRondas, pregunta.getEnunciado(),
                opciones, pregunta.getDificultad(), GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA, venceEn);
    }

    public static PreguntaTriviaDTO pregunta(Trivia trivia) {
        return desdePregunta(trivia.getPreguntaActual(), trivia.getRondaActual(), trivia.getTotalRondas(),
                GestorTriviaPartida.venceEnMilisegundos(trivia));
    }

    private static Integer montoPremio(Trivia trivia) {
        if (trivia.getGanador() == null || trivia.getDificultad() == null
                || "ROBAR".equals(trivia.getTipoPremio())) return null;
        return ServicioPremioTriviaImpl.montoPremio(trivia.getDificultad());
    }

    public static EstadoTriviaDTO estado(String codigoPartida, Trivia trivia) {
        return estado(codigoPartida, trivia, null);
    }

    // El dato personal se calcula solo para el jugador resuelto desde la sesion.
    public static EstadoTriviaDTO estado(String codigoPartida, Trivia trivia, Long idJugador) {
        Boolean yaRespondio = null;
        if (idJugador != null) {
            yaRespondio = false;
            for (Jugador jugador : trivia.getParticipantes()) {
                if (idJugador.equals(jugador.getId())) {
                    yaRespondio = trivia.yaRespondioEnRonda(jugador);
                    break;
                }
            }
        }
        Jugador ganador = trivia.getGanador();
        boolean premioPendiente = ganador != null && trivia.estaFinalizada() && !trivia.estaPremioEntregado();
        List<ParticipanteTriviaDTO> enJuego = trivia.getParticipantes().stream()
                .map(jugador -> new ParticipanteTriviaDTO(jugador.getId(), jugador.getApodo(),
                        trivia.getTiempoAcumuladoMilisegundos(jugador)))
                .toList();
        return new EstadoTriviaDTO(codigoPartida, trivia.getRondaActual(), trivia.getTotalRondas(),
                trivia.getDificultad() == null ? null : trivia.getDificultad().name(), trivia.estaFinalizada(),
                enJuego, ganador == null ? null : ganador.getId(), ganador == null ? null : ganador.getApodo(),
                premioPendiente, GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA,
                trivia.estaFinalizada() ? null : GestorTriviaPartida.venceEnMilisegundos(trivia), yaRespondio,
                trivia.estaCancelada(), trivia.estaPremioEntregado(),
                montoPremio(trivia),
                premioPendiente && trivia.getDificultad() == DificultadRuleta.DIFICIL, trivia.getTipoPremio());
    }
}
