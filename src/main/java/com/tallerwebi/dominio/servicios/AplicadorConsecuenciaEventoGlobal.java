package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.catalogo.CatalogoTablero;
import com.tallerwebi.dominio.entidades.EventoGlobal;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import org.springframework.stereotype.Component;

/** Aplica una consecuencia dentro de la transacción que resuelve el evento. */
@Component
public class AplicadorConsecuenciaEventoGlobal {

    private final ServicioEconomia economia;
    private final ServicioMovimiento movimiento;
    private final ServicioGestionQuiebra quiebras;
    private final ServicioParticipacion participacion;

    public AplicadorConsecuenciaEventoGlobal(
        ServicioEconomia economia,
        ServicioMovimiento movimiento,
        ServicioGestionQuiebra quiebras,
        ServicioParticipacion participacion
    ) {
        this.economia = economia;
        this.movimiento = movimiento;
        this.quiebras = quiebras;
        this.participacion = participacion;
    }

    public String aplicar(Partida partida, EventoGlobal evento, Jugador jugador) {
        if (!participacion.estaActivo(jugador)) {
            throw new IllegalStateException("El jugador seleccionado dejó de estar activo.");
        }

        String detalle;

        if (
            evento.getTipoEventoGlobal().requiereDados() &&
            Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado())
        ) {
            detalle = "no puede desplazarse porque está preso en la comisaría.";
        } else {
            detalle = ejecutarConsecuencia(partida, evento, jugador);
        }

        return jugador.getApodo() + ": " + detalle;
    }

    
    public boolean hayDeudas(Partida partida) {
        return partida
        .getJugadores()
        .stream()
            .filter(participacion::estaActivo)
            .anyMatch(
                jugador ->
                    quiebras.obtenerDeudaPendiente(partida.getCodigoUnico(), jugador.getId()) !=
                    null
            );
        }
        
        private String avanzarHastaPrimero(Partida partida, Jugador jugador) {
        int destino = partida
            .getJugadores()
            .stream()
            .filter(participacion::estaActivo)
            .mapToInt(Jugador::getPosicionActual)
            .max()
            .orElse(jugador.getPosicionActual());
            if (destino == jugador.getPosicionActual()) {
                return "ya está en la posición más adelantada; no necesita avanzar.";
        }
        return mover(jugador, destino - jugador.getPosicionActual());
    }

    private String volverAInicio(Jugador jugador) {
        if (jugador.getPosicionActual() == 0) return "ya está en Inicio; no necesita desplazarse.";
        return mover(jugador, -jugador.getPosicionActual());
    }

    private String mover(Jugador jugador, int desplazamiento) {
        int anterior = jugador.getPosicionActual();
        movimiento.moverJugador(jugador, desplazamiento, CatalogoTablero.TOTAL_CASILLEROS);
        return "se mueve del casillero " + anterior + " al " + jugador.getPosicionActual() + ".";
    }
    
    private String cobrarRobo(Partida partida, Jugador jugador, int monto) {
        if (economia.tieneDineroSuficiente(jugador, monto)) {
            economia.restarDinero(jugador, monto);
            return "fue víctima de un robo y perdió $" + monto + ".";
        }
        quiebras.registrarDeuda(
            partida.getCodigoUnico(),
            jugador,
            null,
            monto,
            "ROBO_EVENTO_GLOBAL"
        );
        return (
            "sufrió un robo de $" +
            monto +
            ". Tiene $" +
            jugador.getDinero() +
            " disponibles y debe pagar $" +
            monto +
            " antes de continuar."
        );
    }
    
    private String premiar(Jugador jugador, int monto) {
        int saldoAnterior = jugador.getDinero();
        economia.sumarDinero(jugador, monto);
        return (
            "ayudó a empujar el Fiat 600 de Germancho. Tenía $" +
            saldoAnterior +
            ", recibió $" +
            monto +
            " y ahora tiene $" +
            jugador.getDinero() +
            "."
        );
    }
    
    private String perderTurno(Jugador jugador) {
        jugador.setPierdeTurno(true);
        return "perderá su próxima oportunidad de turno por el accidente del Sarmiento.";
    }
    
    // Ejecuta la consecuencia correspondiente al tipo de evento global.
    private String ejecutarConsecuencia(Partida partida, EventoGlobal evento, Jugador jugador) {
        return switch (evento.getTipoEventoGlobal()) {
            case TRAMITES_AFIP -> avanzarHastaPrimero(partida, jugador);
            case INUNDACION -> mover(jugador, evento.getValor());
            case PIQUETE -> volverAInicio(jugador);
            case ROBO -> cobrarRobo(partida, jugador, evento.getValor());
            case FIAT_600 -> premiar(jugador, evento.getValor());
            case ACCIDENTE_SARMIENTO -> perderTurno(jugador);
        };
    }
}
