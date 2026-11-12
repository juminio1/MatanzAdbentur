package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PropiedadInvalidaException;
import java.util.Objects;

/** Reglas compartidas de pertenencia y participación en una partida. */
public interface ServicioParticipacion {
    void validarPartidaEnCurso(Partida partida);

    default void validarAccionOrdinaria(Partida partida) {
        validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo()) {
            throw new IllegalStateException("La partida está pausada por un evento global.");
        }
    }

    default void validarCompraEnPartida(Partida partida)
            throws com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException {
        validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo()) {
            throw new com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException(
                    "La partida está pausada por un evento global.");
        }
    }

    default void validarVentaParaDeuda(Partida partida, Boolean tieneDeuda) {
        validarPartidaEnCurso(partida);
        if (partida.getEventoGlobal().estaActivo() && !Boolean.TRUE.equals(tieneDeuda)) {
            throw new IllegalStateException("Durante el evento sólo podés vender para pagar una deuda.");
        }
    }

    default void validarCompraPorEvento(Partida partida, Jugador jugador, Integer ronda, Boolean aceptar) {
        var estado = partida.getEventoGlobal();
        if (aceptar == null || !Objects.equals(estado.getUltimaRonda(), ronda)
                || !Objects.equals(estado.getSeleccionado(), jugador.getId()) || estado.getPosicionCompra() == null) {
            throw new PropiedadNoDisponibleException("La oportunidad de compra ya no está disponible.");
        }
        if (!permiteAdministrarPropiedades(partida, jugador, jugador.getId())) {
            throw new IllegalStateException("No podés comprar en esta partida.");
        }
    }

    default void validarParticipantesDeTransferencia(Partida partidaVendedor, Partida partidaComprador,
                                                    Jugador vendedor, Jugador comprador) {
        if (!Objects.equals(partidaVendedor.getId(), partidaComprador.getId())) {
            throw new IllegalStateException("Los jugadores no pertenecen a la misma partida.");
        }
        if (!estaActivo(vendedor) || !estaActivo(comprador)) {
            throw new IllegalStateException("Solo los participantes activos pueden transferir propiedades.");
        }
        if (Objects.equals(vendedor.getId(), comprador.getId())) {
            throw new PropiedadInvalidaException("No podés comprarle una propiedad a vos mismo.");
        }
    }
    default void validarGestionAntesDeTirar(Partida partida, Long idJugador, boolean paraDeuda) {
        if (paraDeuda) return;
        Jugador jugador = partida.getJugadores().stream()
            .filter(j -> Objects.equals(j.getId(), idJugador)).findFirst().orElse(null);
        if (jugador == null || !Objects.equals(jugador.getOrden(), partida.getIndiceTurnoActual())
                || Boolean.TRUE.equals(jugador.getTiroEnTurno())) {
            throw new IllegalStateException("Solo podés gestionar propiedades antes de tirar, durante tu turno.");
        }
    }

    boolean estaActivo(Jugador jugador);
    boolean pertenece(Partida partida, Long idJugador);
    boolean permiteAdministrarPropiedades(Partida partida, Jugador jugador, Long idJugador);
}
