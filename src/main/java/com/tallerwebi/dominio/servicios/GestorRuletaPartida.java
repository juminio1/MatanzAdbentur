
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
    private static final int PRESENTACION_MINIMA_SEGUNDOS = 7;
    private static final int LIMITE_PRESENTACION_SEGUNDOS = 45;

    private final ServicioRuleta servicioRuleta;
    private final NotificadorPartida notificador;
    private final ScheduledExecutorService planificador;
    private final GestorTriviaPartida gestorTrivia;
    private final com.tallerwebi.dominio.servicios.interfaces.ServicioPartida servicioPartida;
    private final ServicioTriviaImpl servicioTrivia;

    private final Map<String, EstadoRuletaDTO> estados = new HashMap<>();
    private final Map<String, Set<String>> activacionesUsadas = new HashMap<>();
    private final Map<String, Presentacion> presentaciones = new HashMap<>();

    private static final class Presentacion {
        private final EstadoRuletaDTO estado;
        private final Set<Long> pendientes;
        private boolean minimoCumplido;
        private java.util.concurrent.ScheduledFuture<?> limite;
        private java.util.concurrent.ScheduledFuture<?> minimo;

        private Presentacion(EstadoRuletaDTO estado, Set<Long> jugadores) {
            this.estado = estado;
            this.pendientes = new HashSet<>(jugadores);
        }
    }

    public GestorRuletaPartida(ServicioRuleta servicioRuleta, NotificadorPartida notificador,
                               ScheduledExecutorService planificador, GestorTriviaPartida gestorTrivia) {
        // Las pruebas aisladas no consultan participantes; Spring usa el constructor completo.
        this(servicioRuleta, notificador, planificador, gestorTrivia, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GestorRuletaPartida(ServicioRuleta servicioRuleta, NotificadorPartida notificador,
                               ScheduledExecutorService planificador, GestorTriviaPartida gestorTrivia,
                               com.tallerwebi.dominio.servicios.interfaces.ServicioPartida servicioPartida,
                               ServicioTriviaImpl servicioTrivia) {
        this.servicioRuleta = servicioRuleta;
        this.notificador = notificador;
        this.planificador = planificador;
        this.gestorTrivia = gestorTrivia;
        this.servicioPartida = servicioPartida;
        this.servicioTrivia = servicioTrivia;
    }

    /**
     * Marca la partida como ocupada por la trivia en cuanto alguien cae en el casillero,
     * dentro de la misma transacción de la jugada. Sin esto, durante los segundos de la
     * ruleta el turno podía avanzar y activar un evento global al mismo tiempo que la trivia.
     */
    public void reservarTrivia(String codigoPartida) {
        gestorTrivia.reservar(codigoPartida);
    }

    public void cancelarReservaTrivia(String codigoPartida) {
        gestorTrivia.liberarReserva(codigoPartida);
    }

    // El equipo de Trivia llamará a este método cuando corresponda.
    public synchronized EstadoRuletaDTO activarRuleta(String codigoPartida, String activacionId, Long idJugador) {
        try {
            return registrarActivacion(codigoPartida, activacionId, idJugador);
        } catch (RuntimeException ex) {
            // Si no quedó ninguna ruleta esperando, nadie va a iniciar la trivia reservada.
            EstadoRuletaDTO vigente = codigoPartida == null ? null : estados.get(codigoPartida);
            if (vigente == null || vigente.estado() != EstadoRuleta.ESPERANDO) {
                gestorTrivia.liberarReserva(codigoPartida);
            }
            throw ex;
        }
    }

    private EstadoRuletaDTO registrarActivacion(String codigoPartida, String activacionId, Long idJugador) {

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
        if (actual != null && presentaciones.containsKey(actual.codigoPartida())) {
            throw new IllegalStateException("La presentación anterior todavía no terminó.");
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
        if (actual.estado() != EstadoRuleta.ESPERANDO) {
            return actual;
        }

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        EstadoRuletaDTO resultado = new EstadoRuletaDTO(actual.codigoPartida(), actual.activacionId(), actual.idJugador(), EstadoRuleta.SORTEADA, dificultad, actual.venceEn());

        estados.put(actual.codigoPartida(), resultado);
        prepararPresentacion(resultado);

        IllegalStateException error = null;
        // Se comunica un solo resultado a todos los jugadores.
        try {
            notificador.notificarRuleta(actual.codigoPartida(), resultado);
        } catch (RuntimeException ex) {
            error = new IllegalStateException("No se pudo notificar el resultado de la ruleta.", ex);
        }

        if (error != null) {
            throw error;
        }
        return resultado;
    }

    private void prepararPresentacion(EstadoRuletaDTO resultado) {
        try {
            Presentacion presentacion = new Presentacion(resultado, jugadoresEsperados(resultado.codigoPartida()));
            presentaciones.put(resultado.codigoPartida(), presentacion);
            presentacion.minimo = planificador.schedule(() -> cumplirMinimo(presentacion), PRESENTACION_MINIMA_SEGUNDOS, TimeUnit.SECONDS);
            presentacion.limite = planificador.schedule(() -> iniciarTrasPresentacion(presentacion),
                    LIMITE_PRESENTACION_SEGUNDOS, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            Presentacion fallida = presentaciones.remove(resultado.codigoPartida());
            if (fallida != null && fallida.minimo != null) fallida.minimo.cancel(false);
            if (fallida != null && fallida.limite != null) fallida.limite.cancel(false);
            // Conserva el sorteo, pero libera una reserva que ya no puede llegar a trivia.
            EstadoRuletaDTO cancelada = new EstadoRuletaDTO(resultado.codigoPartida(), resultado.activacionId(),
                    resultado.idJugador(), EstadoRuleta.CANCELADA, resultado.dificultad(), resultado.venceEn());
            estados.put(resultado.codigoPartida(), cancelada);
            gestorTrivia.liberarReserva(resultado.codigoPartida());
            try {
                notificador.notificarRuleta(resultado.codigoPartida(), cancelada);
            } catch (RuntimeException avisoFallido) {
                ex.addSuppressed(avisoFallido);
            }
            throw ex;
        }
    }

    public synchronized void confirmarPresentacion(String codigo, String activacionId, Long jugador) {
        EstadoRuletaDTO estado = validarActivacion(codigo, activacionId);
        if (estado.estado() == EstadoRuleta.CANCELADA) return;
        if (estado.estado() != EstadoRuleta.SORTEADA) throw new IllegalStateException("La ruleta no terminó.");
        Presentacion presentacion = presentaciones.get(codigo);
        if (presentacion == null) return;
        presentacion.pendientes.remove(jugador);
        intentarInicio(presentacion);
    }

    private Set<Long> jugadoresEsperados(String codigo) {
        if (servicioPartida == null || servicioTrivia == null) return Set.of();
        var partida = servicioPartida.obtenerPartida(codigo);
        if (partida == null) return Set.of();
        return new HashSet<>(servicioTrivia.participantesHabilitados(partida).stream()
                .map(com.tallerwebi.dominio.entidades.Jugador::getId).toList());
    }

    private synchronized void cumplirMinimo(Presentacion presentacion) {
        presentacion.minimoCumplido = true;
        intentarInicio(presentacion);
    }

    private void intentarInicio(Presentacion presentacion) {
        if (presentacion.minimoCumplido && presentacion.pendientes.isEmpty()) iniciarTrasPresentacion(presentacion);
    }

    private synchronized void iniciarTrasPresentacion(Presentacion presentacion) {
        String codigo = presentacion.estado.codigoPartida();
        if (!presentaciones.remove(codigo, presentacion)) return;
        if (presentacion.limite != null) presentacion.limite.cancel(false);
        if (presentacion.minimo != null) presentacion.minimo.cancel(false);
        gestorTrivia.iniciar(codigo, presentacion.estado.dificultad());
    }

    public synchronized EstadoRuletaDTO obtenerEstado(String codigoPartida) {
        return estados.get(codigoPartida);
    }
}
