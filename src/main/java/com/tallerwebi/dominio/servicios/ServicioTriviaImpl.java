
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import org.springframework.stereotype.Service;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import java.util.List;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;

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
            && !Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado())) {
            trivia.getParticipantes().add(jugador);
        }
    }

    if (!trivia.getParticipantes().isEmpty()) {
    List<PreguntaGenerada> preguntas =
            clienteIA.generarPreguntas(dificultad.name(), 1);

    trivia.setPreguntaActual(preguntas.get(0));
    trivia.setInicioRonda(reloj.instant());
    }

    return trivia;  
}


public void responderPregunta(Trivia trivia, Jugador jugador, int opcionElegida) {
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

    if (trivia.getParticipantes().isEmpty()) {
        trivia.finalizar();
        return;
    }

    if (trivia.getParticipantes().size() == 1) {
        Jugador ultimoParticipante = trivia.getParticipantes().get(0);

        if (trivia.yaRespondioEnRonda(ultimoParticipante)) {
            trivia.declararGanador(ultimoParticipante);
            trivia.finalizar();
        }

        return;
    }

    if (trivia.getParticipantes().stream()
        .allMatch(trivia::yaRespondioEnRonda)) {

    if (trivia.getRondaActual() >= 5) {
        return;
    }

    trivia.avanzarRonda();

    List<PreguntaGenerada> preguntas =
            clienteIA.generarPreguntas(trivia.getDificultad().name(), 1);

    trivia.setPreguntaActual(preguntas.get(0));
    trivia.setInicioRonda(reloj.instant());
    }
}





}

