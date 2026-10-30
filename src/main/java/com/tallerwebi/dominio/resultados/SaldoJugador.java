package com.tallerwebi.dominio.resultados;

import com.tallerwebi.dominio.entidades.Jugador;

public record SaldoJugador(Long idJugador, Integer dinero, Boolean enBancarrota,
                           Boolean encarcelado, Integer intentosCarcel) {
    public static SaldoJugador desdeJugador(Jugador jugador) {
        return new SaldoJugador(jugador.getId(), jugador.getDinero(), jugador.getEnBancarrota(),
                Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado()),
                jugador.getEstadoCarcel().getIntentosSalirCarcel());
    }
}
