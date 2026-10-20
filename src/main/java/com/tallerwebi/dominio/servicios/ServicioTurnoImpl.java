package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import java.util.List;
import org.springframework.stereotype.Service;

/** Resuelvo el orden de los jugadores y el avance de las rondas. */
@Service("servicioTurno")
public class ServicioTurnoImpl implements ServicioTurno {

    @Override
    public Jugador obtenerJugadorActual(Partida partida) {
        Jugador jugador = buscarJugadorPorTurno(partida.getJugadores(), obtenerIndiceTurnoActual(partida));
        return esJugadorActivo(jugador) ? jugador : null;
    }

    @Override
    public void incrementarRondas(Partida partida) {
        Integer ronda = partida.getRondaActual() == null ? 1 : partida.getRondaActual();
        Integer turnosFinalizados =
            partida.getTurnosFinalizadosEnRonda() == null
                ? 0
                : partida.getTurnosFinalizadosEnRonda();

        Long jugadoresActivos = partida.getJugadores().stream()
            .filter(jugador -> !jugador.getEnBancarrota())
            .count();
        if (jugadoresActivos == 0) return;

        turnosFinalizados++;

        if (turnosFinalizados >= jugadoresActivos) {
            ronda++;
            turnosFinalizados = 0;
        }

        partida.setRondaActual(ronda);
        partida.setTurnosFinalizadosEnRonda(turnosFinalizados);
    }

    private Jugador buscarJugadorPorTurno(List<Jugador> jugadores, Integer indice) {
        for (Jugador jugador : jugadores) {
            if (indice.equals(jugador.getOrden())) {
                return jugador;
            }
        }
        return jugadores.get(0);
    }

    @Override
    public Integer obtenerIndiceTurnoActual(Partida partida) {
        return partida.getIndiceTurnoActual() == null ? 0 : partida.getIndiceTurnoActual();
    }

    @Override
    public Jugador resolverSiguienteJugador(Partida partida, Integer indiceActual) {
        Jugador candidato = buscarSiguienteJugadorActivo(partida, indiceActual, false);
        if (candidato != null) {
            return candidato;
        }
        // Repito la búsqueda si todos los activos perdían el turno.
        candidato = buscarSiguienteJugadorActivo(partida, indiceActual, true);
        if (candidato != null) {
            candidato.setPierdeTurno(false);
        }
        return candidato;
    }

    private Jugador buscarSiguienteJugadorActivo(Partida partida, Integer indiceActual,
                                                 Boolean incluirJugadoresPenalizados) {
        Integer cantidadJugadores = partida.getJugadores().size();
        for (Integer desplazamiento = 1; desplazamiento <= cantidadJugadores; desplazamiento++) {
            Integer siguienteIndice = (indiceActual + desplazamiento) % cantidadJugadores;
            Jugador candidato = buscarJugadorPorTurno(partida.getJugadores(), siguienteIndice);
            if (!esJugadorActivo(candidato)) {
                continue;
            }
            if (Boolean.TRUE.equals(candidato.getPierdeTurno()) && !incluirJugadoresPenalizados) {
                candidato.setPierdeTurno(false);
            } else {
                return candidato;
            }
        }
        return null;
    }

    @Override
    public void asignarOrdenInicialJugadores(Partida partida) {
        Integer orden = 0;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.setPierdeTurno(false);
            orden++;
        }
    }

    private Boolean esJugadorActivo(Jugador jugador) {
        return jugador != null && !jugador.getEnBancarrota();
    }
}
