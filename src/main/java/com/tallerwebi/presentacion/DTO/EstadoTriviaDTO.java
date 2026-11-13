package com.tallerwebi.presentacion.DTO;

import java.util.List;

/** Estado público de la trivia de una partida. Nunca incluye la respuesta correcta. */
public record EstadoTriviaDTO(String codigoPartida, Integer ronda, Integer totalRondas, String dificultad,
        Boolean finalizada, List<ParticipanteTriviaDTO> enJuego, Long idGanador, String ganador,
        Boolean premioPendiente, Integer segundosParaResponder, Long venceEn, Boolean yaRespondio, Boolean cancelada, Boolean premioEntregado,
        Integer montoPremio, Boolean puedeElegirPremio, String tipoPremio) {
}
