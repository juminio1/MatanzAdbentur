package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.Objects;

// Separa las consecuencias de la coordinación de tiradas para cumplir el límite de PMD.
public final class AplicadorConsecuenciaEventoGlobal {

    private AplicadorConsecuenciaEventoGlobal() {
    }

    @FunctionalInterface
    public interface AplicadorEvento {
        public void ejecutar(Jugador jugador, Evento evento) throws EventoInexistenteException;
    }

    public static String aplicar(Partida partida, Jugador jugador, EventoGlobal evento, RepositorioPartida repositorio, ServicioGestionQuiebra gestionQuiebra, AplicadorEvento aplicadorEvento) throws EventoInexistenteException {
        String detalle;

        if (evento.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP) {
            detalle = aplicarAfip(partida, jugador);
        } else if (evento.getTipoEventoGlobal() == TipoEventoGlobal.ROBO) {
            detalle = aplicarRobo(partida, jugador, evento.getValor(), gestionQuiebra);
        } else {
            Integer posicionAnterior = jugador.getPosicionActual();
            aplicadorEvento.ejecutar(jugador, evento);
            detalle = obtenerDetalle(jugador, evento, posicionAnterior);
        }

        repositorio.actualizarJugador(jugador);
        return detalle;
    }

    private static String aplicarAfip(Partida partida, Jugador jugador) {
        Integer posicionAnterior = jugador.getPosicionActual();
        Integer posicionMasAdelantada = partida.getJugadores().stream().map(Jugador::getPosicionActual)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(posicionAnterior);
        jugador.setPosicionActual(posicionMasAdelantada);

        return posicionAnterior.equals(posicionMasAdelantada)
                ? "ya estaba en el casillero más adelantado (" + posicionAnterior + "), por eso no avanzó."
                : "avanzó del casillero " + posicionAnterior + " al " + posicionMasAdelantada + ".";
    }

    private static String aplicarRobo(Partida partida, Jugador jugador, Integer monto, ServicioGestionQuiebra gestionQuiebra) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto del robo no es válido");
        }

        Integer saldo = jugador.getDinero() == null ? 0 : jugador.getDinero();
        if (saldo >= monto) {
            jugador.setDinero(saldo - monto);
            return "perdió $" + monto + " y ahora tiene $" + jugador.getDinero() + ".";
        }

        return registrarDeudaPorRobo(partida, jugador, monto, gestionQuiebra);
    }

    private static String registrarDeudaPorRobo(Partida partida, Jugador jugador, Integer monto, ServicioGestionQuiebra gestionQuiebra) {
        if (gestionQuiebra == null) {
            throw new IllegalStateException("No está disponible el servicio de gestión de quiebras");
        }

        String codigoPartida = partida.getCodigoUnico();
        gestionQuiebra.registrarDeuda(codigoPartida, jugador, null, monto, "EVENTO_RESTA");

        try {
            ResultadoDeuda resultado = gestionQuiebra.evaluarDeuda(codigoPartida, jugador.getId());
            return switch (resultado) {
                case PENDIENTE -> "debe $" + monto + " por el robo. Necesita vender propiedades o declararse en quiebra.";
                case QUIEBRA -> "no pudo afrontar el robo de $" + monto + " y quedó en quiebra.";
                case RESUELTA -> "pagó $" + monto + " por el robo.";
                case SIN_DEUDA -> "no tiene una deuda pendiente por el robo.";
            };
        } catch (ActivacionEventoInvalidoException | PartidaNoEncontradaException | EventoGlobalInvalidoException
                | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException exception) {
            throw new IllegalStateException("No se pudo gestionar la deuda del evento global", exception);
        }
    }

    private static String obtenerDetalle(Jugador jugador, EventoGlobal evento, Integer posicionAnterior) {
        return switch (evento.getTipoEventoGlobal()) {
            case ACCIDENTE_SARMIENTO -> "perderá su próximo turno por el accidente del Sarmiento.";
            case FIAT_600 -> "ganó $" + evento.getValor() + ". Su saldo actual es $" + jugador.getDinero() + ".";
            case INUNDACION -> "retrocedió hasta el casillero " + jugador.getPosicionActual() + ".";
            case PIQUETE -> Objects.equals(posicionAnterior, jugador.getPosicionActual())
                    ? "ya estaba en Inicio (casillero 0), por eso no se movió."
                    : "volvió del casillero " + posicionAnterior + " al Inicio (casillero " + jugador.getPosicionActual() + ").";
            case TRAMITES_AFIP -> "se adelantó hasta el jugador que iba primero.";
            case ROBO -> "perdió dinero por el robo.";
        };
    }
}
