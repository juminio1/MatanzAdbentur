package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Decide si un jugador puede participar del torneo de trivia. */
@Service
public class ValidadorParticipanteTrivia {

    private final ServicioParticipacion participacion;

    public ValidadorParticipanteTrivia(ServicioParticipacion participacion) {
        this.participacion = participacion;
    }

    public void validarHabilitado(Partida partida, Long idJugador) {
        if (!participacion.pertenece(partida, idJugador)) {
            throw new ParticipanteTriviaNoHabilitadoException("No pertenecés a esta partida.");
        }
        Jugador jugador = buscarJugador(partida, idJugador);
        if (!participacion.estaActivo(jugador)) {
            throw new ParticipanteTriviaNoHabilitadoException("Los jugadores eliminados no pueden participar de la trivia.");
        }
        if (Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado())) {
            throw new ParticipanteTriviaNoHabilitadoException("Los jugadores presos no pueden participar de la trivia.");
        }
    }

    private Jugador buscarJugador(Partida partida, Long idJugador) {
        return partida.getJugadores().stream()
                .filter(jugador -> Objects.equals(jugador.getId(), idJugador))
                .findFirst()
                .orElseThrow(() -> new ParticipanteTriviaNoHabilitadoException("No pertenecés a esta partida."));
    }
}
