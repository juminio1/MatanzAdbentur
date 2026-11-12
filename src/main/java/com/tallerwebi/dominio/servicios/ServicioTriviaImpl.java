
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ServicioTriviaImpl {

    private static final Duration TIEMPO_LIMITE_PREDETERMINADO =
            Duration.ofSeconds(30);

    private static final int MAXIMO_RONDAS = 5;
    private static final int CANTIDAD_UNICA = 1;

    private final ServicioParticipacion servicioParticipacion;
    private final ServicioPreguntasTrivia servicioPreguntas;
    private final Clock reloj;
    private final Duration tiempoLimiteRespuesta;

    @Autowired
    public ServicioTriviaImpl(ServicioParticipacion servicioParticipacion,
                              ServicioPreguntasTrivia servicioPreguntas) {
        this(
                servicioParticipacion,
                servicioPreguntas,
                Clock.systemUTC(),
                TIEMPO_LIMITE_PREDETERMINADO
        );
    }

    ServicioTriviaImpl(ServicioParticipacion servicioParticipacion,
                       ServicioPreguntasTrivia servicioPreguntas,
                       Clock reloj) {
        this(
                servicioParticipacion,
                servicioPreguntas,
                reloj,
                TIEMPO_LIMITE_PREDETERMINADO
        );
    }

    ServicioTriviaImpl(ServicioParticipacion servicioParticipacion,
                       ServicioPreguntasTrivia servicioPreguntas,
                       Clock reloj,
                       Duration tiempoLimiteRespuesta) {
        if (tiempoLimiteRespuesta == null
                || tiempoLimiteRespuesta.isNegative()
                || tiempoLimiteRespuesta.isZero()) {
            throw new IllegalArgumentException(
                    "El tiempo límite debe ser positivo.");
        }

        this.servicioParticipacion = servicioParticipacion;
        this.servicioPreguntas = servicioPreguntas;
        this.reloj = reloj;
        this.tiempoLimiteRespuesta = tiempoLimiteRespuesta;
    }

    public Trivia iniciarTrivia(Partida partida,
                                DificultadRuleta dificultad) {
        Trivia trivia = new Trivia();

        trivia.setDificultad(dificultad);
        trivia.setRondaActual(1);

        for (Jugador jugador : partida.getJugadores()) {
            if (servicioParticipacion.estaActivo(jugador)
                    && !Boolean.TRUE.equals(
                            jugador.getEstadoCarcel().getEncarcelado())) {
                trivia.getParticipantes().add(jugador);
            }
        }

        if (trivia.getParticipantes().isEmpty()) {
            trivia.finalizar();
            return trivia;
        }

        List<PreguntaGenerada> preguntas = obtenerPreguntas(partida, dificultad);

        // Si no hay preguntas (falló la IA y el banco está vacío), la trivia termina sin ganador.
        if (preguntas.isEmpty()) {
            trivia.finalizar();
            return trivia;
        }

        trivia.prepararPreguntas(preguntas);
        trivia.setInicioRonda(reloj.instant());

        return trivia;
    }

    public void responderPregunta(Trivia trivia,
                                  Jugador jugador,
                                  int opcionElegida) {

        if (trivia.estaFinalizada()
                || !trivia.getParticipantes().contains(jugador)
                || trivia.yaRespondioEnRonda(jugador)) {
            return;
        }

        validarOpcion(trivia, opcionElegida);

        Instant ahora = reloj.instant();

        if (vencioTiempo(trivia, ahora)) {
            cerrarRondaPorTiempo(trivia, ahora);
            return;
        }

        long tiempoRespuesta = Duration.between(
                trivia.getInicioRonda(),
                ahora
        ).toMillis();

        trivia.registrarTiempoRespuesta(jugador, tiempoRespuesta);
        trivia.registrarRespuesta(jugador);

        boolean respuestaCorrecta = Integer.valueOf(opcionElegida)
                .equals(trivia.getPreguntaActual().getRespuestaCorrecta());

        if (!respuestaCorrecta) {
            trivia.eliminarParticipante(jugador);
        }

        evaluarEstadoRonda(trivia);
    }

    public void cerrarRondaPorTiempo(Trivia trivia) {
        if (trivia.estaFinalizada()) {
            return;
        }

        cerrarRondaPorTiempo(trivia, reloj.instant());
    }

    private void cerrarRondaPorTiempo(Trivia trivia, Instant ahora) {
        if (!vencioTiempo(trivia, ahora)) {
            return;
        }

        List<Jugador> participantes =
                List.copyOf(trivia.getParticipantes());

        for (Jugador jugador : participantes) {
            if (!trivia.yaRespondioEnRonda(jugador)) {
                trivia.eliminarParticipante(jugador);
            }
        }

        evaluarEstadoRonda(trivia);
    }

    public void actualizarParticipantes(Trivia trivia) {
        if (trivia.estaFinalizada()) {
            return;
        }

        List<Jugador> participantes =
                List.copyOf(trivia.getParticipantes());

        for (Jugador jugador : participantes) {
            if (!servicioParticipacion.estaActivo(jugador)) {
                trivia.eliminarParticipante(jugador);
            }
        }

        evaluarEstadoRonda(trivia);
    }

    public void cancelarPorAbandonoQueFinalizaPartida(Trivia trivia) {
        if (trivia.estaPremioEntregado()) {
            throw new IllegalStateException(
                    "No se puede cancelar una trivia con premio entregado.");
        }

        trivia.cancelarSinPremio();
    }

    private void validarOpcion(Trivia trivia, int opcionElegida) {
        int cantidadOpciones =
                trivia.getPreguntaActual().getOpciones().size();

        if (opcionElegida < 0 || opcionElegida >= cantidadOpciones) {
            throw new IllegalArgumentException(
                    "La opción seleccionada no es válida.");
        }
    }

    private boolean vencioTiempo(Trivia trivia, Instant ahora) {
        Instant vencimiento = trivia.getInicioRonda()
                .plus(tiempoLimiteRespuesta);

        return !ahora.isBefore(vencimiento);
    }

    private void evaluarEstadoRonda(Trivia trivia) {
        if (trivia.getParticipantes().isEmpty()) {
            trivia.finalizar();
            return;
        }

        if (trivia.getParticipantes().size() == CANTIDAD_UNICA) {
            Jugador ultimoParticipante =
                    trivia.getParticipantes().get(0);

            if (trivia.yaRespondioEnRonda(ultimoParticipante)) {
                trivia.declararGanador(ultimoParticipante);
                trivia.finalizar();
            }

            return;
        }

        boolean todosRespondieron = trivia.getParticipantes().stream()
                .allMatch(trivia::yaRespondioEnRonda);

        if (!todosRespondieron) {
            return;
        }

        if (trivia.getRondaActual() >= trivia.getTotalRondas()) {
            declararGanadorPorTiempo(trivia);
            trivia.finalizar();
            return;
        }

        trivia.avanzarRonda();
        trivia.pasarASiguientePregunta();
        trivia.setInicioRonda(reloj.instant());
    }

    private void declararGanadorPorTiempo(Trivia trivia) {
        long menorTiempo = trivia.getParticipantes().stream()
                .mapToLong(trivia::getTiempoAcumuladoMilisegundos)
                .min()
                .orElseThrow();

        List<Jugador> jugadoresMasRapidos =
                trivia.getParticipantes().stream()
                        .filter(jugador ->
                                trivia.getTiempoAcumuladoMilisegundos(jugador)
                                        == menorTiempo)
                        .toList();

        if (jugadoresMasRapidos.size() == CANTIDAD_UNICA) {
            trivia.declararGanador(jugadoresMasRapidos.get(0));
        }
    }

    // ServicioPreguntasTrivia evita repetir preguntas en la partida y usa el banco si falla la IA.
    private List<PreguntaGenerada> obtenerPreguntas(Partida partida,
                                                    DificultadRuleta dificultad) {
        List<PreguntaTrivia> preguntas = servicioPreguntas.obtenerParaCasillero(
                partida.getCodigoUnico(), dificultad);

        if (preguntas == null) {
            return List.of();
        }

        return preguntas.stream()
                .limit(MAXIMO_RONDAS)
                .map(pregunta -> new PreguntaGenerada(
                        dificultad.name(),
                        pregunta.getEnunciado(),
                        pregunta.getOpciones(),
                        pregunta.getRespuestaCorrecta()))
                .toList();
    }
}
