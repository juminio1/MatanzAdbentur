package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Resuelvo el orden de los jugadores y el avance de las rondas. */
@Service("servicioTurno")
public class ServicioTurnoImpl implements ServicioTurno {
    private final ServicioParticipacion participacion;

    public ServicioTurnoImpl(ServicioParticipacion participacion) {
        this.participacion = participacion;
    }

    @Override
    public Jugador obtenerJugadorActual(Partida partida) {
        Jugador jugador = buscarJugadorPorTurno(partida.getJugadores(), obtenerIndiceTurnoActual(partida));
        return participacion.estaActivo(jugador) ? jugador : null;
    }

    @Override
    public void incrementarRondas(Partida partida) {
        participacion.validarPartidaEnCurso(partida);
        if (partida.getJugadores().isEmpty()) {
            return;
        }
        if (partida.getRondaActual() == null) {
            partida.setRondaActual(1);
        }
        Jugador actual = buscarJugadorPorTurno(partida.getJugadores(), obtenerIndiceTurnoActual(partida));
        consumirOportunidad(partida, actual);
    }

    private void consumirOportunidad(Partida partida, Jugador jugador) {
        if (participacion.estaActivo(jugador)) {
            jugador.setUltimaRondaConOportunidad(partida.getRondaActual());
        }
        actualizarContadoresDeRonda(partida);
    }

    private void actualizarContadoresDeRonda(Partida partida) {
        int activos = 0;
        int turnosFinalizados = 0;
        for (Jugador jugador : partida.getJugadores()) {
            if (participacion.estaActivo(jugador)) {
                activos++;
                if (Objects.equals(partida.getRondaActual(), jugador.getUltimaRondaConOportunidad())) {
                    turnosFinalizados++;
                }
            }
        }
        if (activos == 0) {
            return;
        }
        if (turnosFinalizados == activos) {
            partida.setRondaActual(partida.getRondaActual() + 1);
            turnosFinalizados = 0;
        }
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
        participacion.validarPartidaEnCurso(partida);
        Jugador siguiente = buscarSiguienteJugadorActivo(partida, indiceActual);
        // Si todos estaban penalizados, consumimos la oportunidad de cada uno
        // y repetimos la búsqueda. Nunca habilitamos a alguien aún penalizado.
        while (siguiente == null && hayJugadoresActivos(partida)) {
            siguiente = buscarSiguienteJugadorActivo(partida, indiceActual);
        }
        return siguiente;
    }

    private boolean hayJugadoresActivos(Partida partida) {
        return partida.getJugadores().stream().anyMatch(participacion::estaActivo);
    }

    private Jugador buscarSiguienteJugadorActivo(Partida partida, Integer indiceActual) {
        Integer cantidadJugadores = partida.getJugadores().size();
        Jugador disponible = null;
        for (Integer desplazamiento = 1; desplazamiento <= cantidadJugadores && disponible == null; desplazamiento++) {
            Integer siguienteIndice = (indiceActual + desplazamiento) % cantidadJugadores;
            Jugador candidato = buscarJugadorPorTurno(partida.getJugadores(), siguienteIndice);
            if (participacion.estaActivo(candidato)) {
                if (Boolean.TRUE.equals(candidato.getPierdeTurno())) {
                    candidato.setPierdeTurno(false);
                    consumirOportunidad(partida, candidato);
                } else {
                    disponible = candidato;
                }
            }
        }
        return disponible;
    }

    @Override
    public void asignarOrdenInicialJugadores(Partida partida) {
        Integer orden = 0;
        for (Jugador jugador : partida.getJugadores()) {
            jugador.setOrden(orden);
            jugador.limpiarTurnosPenalizados();
            jugador.setTiroEnTurno(false);
            jugador.setUltimaRondaConOportunidad(0);
            orden++;
        }
    }

}
