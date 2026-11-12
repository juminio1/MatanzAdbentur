package com.tallerwebi.dominio.eventos;

import java.util.Map;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;

 //Cambio en la trivia de una partida.
 //opcionCorrecta solo se informa en RONDA_CERRADA, cuando ya nadie puede responder esa pregunta.
public record EventoTriviaActualizada(String codigoPartida, TipoAvisoTrivia tipo, Trivia trivia,
                                      Integer opcionCorrecta) {
}
