
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;
import com.tallerwebi.presentacion.DTO.EstadoRuletaDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class GestorRuletaPartida {

    private static final int SEGUNDOS_ESPERA = 10;

    private final ServicioRuleta servicioRuleta;
    private final NotificadorPartida notificador;
    private final ScheduledExecutorService planificador;

    private final Map<String, EstadoRuletaDTO> estados = new HashMap<>();
    private final Map<String, Set<String>> activacionesUsadas = new HashMap<>();

    public GestorRuletaPartida(ServicioRuleta servicioRuleta, NotificadorPartida notificador, ScheduledExecutorService planificador) {
        this.servicioRuleta = servicioRuleta;
        this.notificador = notificador;
        this.planificador = planificador;
    }

    // El equipo de Trivia llamará a este método cuando corresponda.
    public synchronized EstadoRuletaDTO activarRuleta(String codigoPartida, String activacionId, Long idJugador) {

        validarDatosActivacion(codigoPartida, activacionId, idJugador);

        EstadoRuletaDTO actual = estados.get(codigoPartida);

        // La misma activación no puede volver a iniciarse.
        if (esMismaActivacion(actual, activacionId)) {
            return actual;
        }

        Set<String> usadas = activacionesUsadas.computeIfAbsent(codigoPartida, codigo -> new HashSet<>());

        validarNuevaActivacion(actual, usadas, activacionId);

        long vencimiento = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SEGUNDOS_ESPERA);

        EstadoRuletaDTO nuevoEstado = new EstadoRuletaDTO(codigoPartida, activacionId, idJugador, EstadoRuleta.ESPERANDO, null, vencimiento);

        usadas.add(activacionId);
        estados.put(codigoPartida, nuevoEstado);

        // Todos los jugadores reciben la activación.
        notificador.notificarRuleta(codigoPartida, nuevoEstado);

        // El backend ejecuta el giro si nadie lo hizo antes.
        planificador.schedule(() -> sortearAutomaticamente(codigoPartida, activacionId), SEGUNDOS_ESPERA, TimeUnit.SECONDS);

        return nuevoEstado;
    }

    private void validarDatosActivacion(String codigoPartida, String activacionId, Long idJugador) {
        if (codigoPartida == null || codigoPartida.isBlank() || activacionId == null || activacionId.isBlank() || idJugador == null) {
            throw new IllegalArgumentException("Los datos de activación son obligatorios.");
        }
    }

    private boolean esMismaActivacion(EstadoRuletaDTO actual, String activacionId) {
        return actual != null && actual.activacionId().equals(activacionId);
    }

    private void validarNuevaActivacion(EstadoRuletaDTO actual, Set<String> usadas, String activacionId) {

        if (usadas.contains(activacionId)) {
            throw new IllegalStateException("Esta activación ya fue utilizada.");
        }

        if (actual != null && actual.estado() == EstadoRuleta.ESPERANDO) {
            throw new IllegalStateException("Ya existe una ruleta activa en esta partida.");
        }
    }

    public synchronized EstadoRuletaDTO sortearDificultad(String codigoPartida, String activacionId, Long idJugador) {

        EstadoRuletaDTO actual = validarActivacion(codigoPartida, activacionId);

        if (!Objects.equals(actual.idJugador(), idJugador)) {
            throw new SecurityException("No sos el jugador habilitado para girar.");
        }

        return resolverSorteo(actual);
    }

    private synchronized void sortearAutomaticamente(String codigoPartida, String activacionId) {

        EstadoRuletaDTO actual = estados.get(codigoPartida);

        if (esMismaActivacion(actual, activacionId)) {
            resolverSorteo(actual);
        }
    }

    private EstadoRuletaDTO validarActivacion(String codigoPartida, String activacionId) {

        EstadoRuletaDTO actual = estados.get(codigoPartida);

        if (!esMismaActivacion(actual, activacionId)) {
            throw new IllegalStateException("No existe una activación válida de ruleta.");
        }

        return actual;
    }

    private EstadoRuletaDTO resolverSorteo(EstadoRuletaDTO actual) {

        // Una solicitud repetida devuelve el resultado existente.
        if (actual.estado() == EstadoRuleta.SORTEADA) {
            return actual;
        }

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        EstadoRuletaDTO resultado = new EstadoRuletaDTO(actual.codigoPartida(), actual.activacionId(), actual.idJugador(), EstadoRuleta.SORTEADA, dificultad, actual.venceEn());

        estados.put(actual.codigoPartida(), resultado);

        // Se comunica un solo resultado a todos los jugadores.
        notificador.notificarRuleta(actual.codigoPartida(), resultado);

        return resultado;
    }

    public synchronized EstadoRuletaDTO obtenerEstado(String codigoPartida) {
        return estados.get(codigoPartida);
    }
}
