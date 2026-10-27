package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import org.springframework.stereotype.Service;

@Service
public class ServicioEconomiaImpl implements ServicioEconomia {
    @Override
    public boolean tieneDineroSuficiente(Jugador jugador, Integer monto) {
        validar(jugador, monto);
        return jugador.getDinero() != null && jugador.getDinero() >= monto;
    }
    @Override
    public void sumarDinero(Jugador jugador, Integer monto) {
        validarSaldo(jugador, monto);
        jugador.setDinero(Math.addExact(jugador.getDinero(), monto));
    }
    @Override
    public void restarDinero(Jugador jugador, Integer monto) {
        validarSaldo(jugador, monto);
        jugador.setDinero(Math.subtractExact(jugador.getDinero(), monto));
    }
    private void validar(Jugador jugador, Integer monto) {
        if (jugador == null || monto == null || monto < 0) {
            throw new IllegalArgumentException("Datos inválidos");
        }
    }
    private void validarSaldo(Jugador jugador, Integer monto) {
        validar(jugador, monto);
        if (jugador.getDinero() == null) throw new IllegalArgumentException("Saldo no inicializado");
    }
}
