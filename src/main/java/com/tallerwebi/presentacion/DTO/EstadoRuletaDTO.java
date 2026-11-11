
package com.tallerwebi.presentacion.DTO;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoRuleta;

public record EstadoRuletaDTO(String codigoPartida, String activacionId, Long idJugador,
        EstadoRuleta estado, DificultadRuleta  dificultad, long venceEn) { //Este objetito representa la info que enviamos por WebSockets y rest




}
