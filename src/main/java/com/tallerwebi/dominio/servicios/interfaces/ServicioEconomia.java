package com.tallerwebi.dominio.servicios.interfaces;
import com.tallerwebi.dominio.entidades.Jugador;

/** Operaciones de saldo; las deudas y la quiebra pertenecen a ServicioGestionQuiebra. */
public interface ServicioEconomia {
    boolean tieneDineroSuficiente(Jugador jugador, Integer monto);
    void sumarDinero(Jugador jugador, Integer monto);
    void restarDinero(Jugador jugador, Integer monto);
}
