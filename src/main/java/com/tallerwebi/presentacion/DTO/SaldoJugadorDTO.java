package com.tallerwebi.presentacion.DTO;

import com.tallerwebi.dominio.entidades.Jugador;

public record SaldoJugadorDTO(Long idJugador, Integer dinero, Boolean enBancarrota) {
    public static SaldoJugadorDTO desdeJugador(Jugador jugador) {
        return new SaldoJugadorDTO(jugador.getId(), jugador.getDinero(), jugador.getEnBancarrota());
    }
}
