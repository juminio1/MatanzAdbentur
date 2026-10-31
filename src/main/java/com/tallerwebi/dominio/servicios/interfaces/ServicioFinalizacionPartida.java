package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.FinalizarPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;
import java.util.List;

public interface ServicioFinalizacionPartida {
    boolean finalizarSiCorresponde(Partida partida);
    boolean cumpleCondicionFinalizacion(Partida partida);
    List<FinalizarPartida> determinarGanadores(Partida partida);
    Integer calcularValorPatrimonial(Propiedad propiedad);
    Integer calcularPatrimonioTotal(String codigoPartida, Jugador jugador);
    FinalizarPartida conservarResultadoEconomico(
            Partida partida, Jugador jugador, EstadoParticipante estado);
}
