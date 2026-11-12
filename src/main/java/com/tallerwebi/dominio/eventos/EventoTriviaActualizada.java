package com.tallerwebi.dominio.eventos;

import java.util.List;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;

 //Cambio en la trivia de una partida.
 //opcionCorrecta solo se informa en RONDA_CERRADA, cuando ya nadie puede responder esa pregunta.
public record EventoTriviaActualizada(String codigoPartida, TipoAvisoTrivia tipo, Trivia trivia,
                                      Integer opcionCorrecta, Integer rondaCerrada, List<Jugador> eliminados) {
    public EventoTriviaActualizada {
        eliminados = List.copyOf(eliminados);
    }

    public EventoTriviaActualizada(String codigoPartida, TipoAvisoTrivia tipo, Trivia trivia,
                                   Integer opcionCorrecta) {
        this(codigoPartida, tipo, trivia, opcionCorrecta,
                tipo == TipoAvisoTrivia.RONDA_CERRADA ? trivia.getRondaActual() : null, List.of());
    }
}
