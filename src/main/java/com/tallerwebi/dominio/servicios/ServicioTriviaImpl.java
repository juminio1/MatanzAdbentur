
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
public class ServicioTriviaImpl {

    private final ServicioParticipacion servicioParticipacion;
    private final ClienteIA clienteIA;
    private final Clock reloj;

    @Autowired
    public ServicioTriviaImpl(ServicioParticipacion servicioParticipacion,
                              ClienteIA clienteIA) {
        this(servicioParticipacion, clienteIA, Clock.systemUTC());
    }

    ServicioTriviaImpl(ServicioParticipacion servicioParticipacion,
                       ClienteIA clienteIA,
                       Clock reloj) {
        this.servicioParticipacion = servicioParticipacion;
        this.clienteIA = clienteIA;
        this.reloj = reloj;
    }

    public Trivia iniciarTrivia(Partida partida, DificultadRuleta dificultad) {
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

        if (!trivia.getParticipantes().isEmpty()) {
            trivia.setPreguntaActual(solicitarPregunta(dificultad));
            trivia.setInicioRonda(reloj.instant());
        }

        return trivia;
    }

    public void responderPregunta(Trivia trivia, Jugador jugador,
                                  int opcionElegida) {

        if (trivia.estaFinalizada()
                || !trivia.getParticipantes().contains(jugador)
                || trivia.yaRespondioEnRonda(jugador)) {
            return;
        }

        long tiempoRespuesta = Duration.between(
                trivia.getInicioRonda(),
                reloj.instant()
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

    private void evaluarEstadoRonda(Trivia trivia) {
        if (trivia.getParticipantes().isEmpty()) {
            trivia.finalizar();
            return;
        }

        if (trivia.getParticipantes().size() == 1) {
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

        if (trivia.getRondaActual() >= 5) {
            declararGanadorPorTiempo(trivia);
            trivia.finalizar();
            return;
        }

        PreguntaGenerada siguientePregunta =
                solicitarPregunta(trivia.getDificultad());

        trivia.avanzarRonda();
        trivia.setPreguntaActual(siguientePregunta);
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

        if (jugadoresMasRapidos.size() == 1) {
            trivia.declararGanador(jugadoresMasRapidos.get(0));
        }
    }

    private PreguntaGenerada solicitarPregunta(DificultadRuleta dificultad) {
        List<PreguntaGenerada> preguntas =
                clienteIA.generarPreguntas(dificultad.name(), 1);

        if (preguntas == null || preguntas.isEmpty()
                || preguntas.get(0) == null) {
            throw new IllegalStateException(
                    "La IA no devolvió una pregunta válida."
            );
        }

        return preguntas.get(0);
    }
}
