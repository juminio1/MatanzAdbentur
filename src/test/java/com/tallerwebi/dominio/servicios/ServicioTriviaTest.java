
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoPartida;
import org.junit.jupiter.api.Test;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.eq;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioTriviaTest {

    private final ServicioParticipacion servicioParticipacion =
        mock(ServicioParticipacion.class);

    private final ServicioPreguntasTrivia preguntas =
        mock(ServicioPreguntasTrivia.class);

    private final ServicioTriviaImpl servicio =
        new ServicioTriviaImpl(servicioParticipacion, preguntas);

    
@BeforeEach
void configurarPreguntaPorDefecto() {
    PreguntaGenerada pregunta = new PreguntaGenerada(
            "FACIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), any()))
            .thenReturn(enPreguntasTrivia(pregunta, pregunta, pregunta, pregunta, pregunta));
}

// Convierte las preguntas de prueba al formato que devuelve ServicioPreguntasTrivia.
private static List<PreguntaTrivia> enPreguntasTrivia(PreguntaGenerada... generadas) {
    return java.util.Arrays.stream(generadas)
            .map(pregunta -> new PreguntaTrivia(pregunta.getEnunciado(), pregunta.getOpciones(),
                    pregunta.getRespuestaCorrecta(),
                    DificultadTrivia.desdeRuleta(DificultadRuleta.valueOf(pregunta.getDificultad()))))
            .toList();
}
    

    @Test
    void deberiaIniciarTriviaSoloConJugadoresQueNoEstanEnLaCarcel() {
        // Preparación
        Jugador jugador1 = new Jugador();
        Jugador jugador2 = new Jugador();
        Jugador jugador3 = new Jugador();

        jugador3.getEstadoCarcel().encarcelar();

        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(jugador1, jugador2, jugador3));

        when(servicioParticipacion.estaActivo(any(Jugador.class))).thenReturn(true);    

        // Ejecución
        Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

        // Verificación
        assertEquals(2, trivia.getParticipantes().size());
        assertTrue(trivia.getParticipantes().contains(jugador1));
        assertTrue(trivia.getParticipantes().contains(jugador2));
        assertFalse(trivia.getParticipantes().contains(jugador3));
    }

    
@Test
void deberiaGuardarLaDificultadAlIniciarLaTrivia() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class))).thenReturn(true);    

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.MEDIO);

    // Verificación
    assertEquals(DificultadRuleta.MEDIO, trivia.getDificultad());
}

@Test
void deberiaIniciarLaTriviaEnLaPrimeraRonda() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class))).thenReturn(true);    

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    assertEquals(1, trivia.getRondaActual());
}


@Test
void deberiaExcluirJugadoresInactivosDeLaTrivia() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();
    Jugador jugador3 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2, jugador3));

    when(servicioParticipacion.estaActivo(any(Jugador.class))).thenReturn(true);
    when(servicioParticipacion.estaActivo(jugador2)).thenReturn(false);

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // Verificación
    assertEquals(2, trivia.getParticipantes().size());
    assertTrue(trivia.getParticipantes().contains(jugador1));
    assertFalse(trivia.getParticipantes().contains(jugador2));
    assertTrue(trivia.getParticipantes().contains(jugador3));
}


@Test
void noDeberiaDeclararGanadorAutomaticamenteSiSoloHayUnParticipante() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
        "DIFICIL",
        "¿Cuál es la capital de Argentina?",
        List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
        1
);

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Verificación
    assertEquals(1, trivia.getParticipantes().size());
    assertNull(trivia.getGanador());
}


@Test
void deberiaGenerarUnaPreguntaSiHayUnSoloParticipante() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    ServicioTriviaImpl servicioConIA =
            new ServicioTriviaImpl(servicioParticipacion, preguntas);

    // Ejecución
    servicioConIA.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Verificación
    verify(preguntas).obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL));
}


@Test
void deberiaConservarLaPreguntaGeneradaParaLaTrivia() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Verificación
    assertEquals(pregunta.getEnunciado(),
            trivia.getPreguntaActual().getEnunciado());
}


@Test
void deberiaGanarLaTriviaSiElUnicoParticipanteRespondeCorrectamente() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 1);

    // Verificación
    assertSame(jugador1, trivia.getGanador());
}


@Test
void deberiaFinalizarSinGanadorSiElUnicoParticipanteRespondeIncorrectamente() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 0);

    // Verificación
    assertNull(trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}


@Test
void noDeberiaPermitirResponderUnaTriviaFinalizada() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.DIFICIL)))
            .thenReturn(enPreguntasTrivia(pregunta));

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 0);
    servicio.responderPregunta(trivia, jugador1, 1);

    // Verificación
    assertTrue(trivia.estaFinalizada());
    assertNull(trivia.getGanador());
}


@Test
void deberiaGenerarUnaPreguntaComunParaVariosParticipantes() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();
    Jugador jugador3 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2, jugador3));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "MEDIO",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.MEDIO)))
            .thenReturn(enPreguntasTrivia(pregunta));

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.MEDIO);

    // Verificación
    assertEquals(3, trivia.getParticipantes().size());
    assertEquals(pregunta.getEnunciado(), trivia.getPreguntaActual().getEnunciado());

    verify(preguntas).obtenerParaCasillero(any(), eq(DificultadRuleta.MEDIO));
}


@Test
void deberiaEliminarAlParticipanteQueRespondeIncorrectamente() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();
    Jugador jugador3 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2, jugador3));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 0);

    assertEquals(2, trivia.getParticipantes().size());
    assertFalse(trivia.getParticipantes().contains(jugador1));
    assertTrue(trivia.getParticipantes().contains(jugador2));
    assertTrue(trivia.getParticipantes().contains(jugador3));
    }


@Test
void deberiaDeclararGanadorCuandoQuedaUnSoloParticipante() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 1);
    servicio.responderPregunta(trivia, jugador2, 0);

    // Verificación
    assertEquals(1, trivia.getParticipantes().size());
    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}


@Test
void deberiaEsperarLaRespuestaDelUltimoParticipanteAntesDeDeclararGanador() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // El jugador 2 responde incorrectamente
    servicio.responderPregunta(trivia, jugador2, 0);

    // La trivia debe esperar al jugador 1
    assertEquals(1, trivia.getParticipantes().size());
    assertNull(trivia.getGanador());
    assertFalse(trivia.estaFinalizada());

    // El jugador 1 responde correctamente
    servicio.responderPregunta(trivia, jugador1, 1);

    // Ahora sí debe ganar y finalizar la trivia
    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}


@Test
void deberiaAvanzarDeRondaCuandoTodosRespondenCorrectamente() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 1);

    // Todavía falta que responda el segundo jugador
    assertEquals(1, trivia.getRondaActual());

    servicio.responderPregunta(trivia, jugador2, 1);

    // Verificación
    assertEquals(2, trivia.getRondaActual());
    assertEquals(2, trivia.getParticipantes().size());
    assertNull(trivia.getGanador());
    assertFalse(trivia.estaFinalizada());
}


@Test
void deberiaGenerarUnaNuevaPreguntaAlAvanzarDeRonda() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada preguntaRonda1 = new PreguntaGenerada(
            "FACIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    PreguntaGenerada preguntaRonda2 = new PreguntaGenerada(
            "FACIL",
            "¿Cuánto es 2 + 2?",
            List.of("3", "4", "5", "6"),
            1
    );

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.FACIL)))
            .thenReturn(enPreguntasTrivia(preguntaRonda1, preguntaRonda2));

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 1);
    servicio.responderPregunta(trivia, jugador2, 1);

    // Verificación
    assertEquals(2, trivia.getRondaActual());
    assertEquals(preguntaRonda2.getEnunciado(), trivia.getPreguntaActual().getEnunciado());
}


@Test
void noDeberiaSuperarLasCincoRondas() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    // Ejecución
    for (int ronda = 1; ronda <= 5; ronda++) {
        servicio.responderPregunta(trivia, jugador1, 1);
        servicio.responderPregunta(trivia, jugador2, 1);
    }

    // Verificación
    assertEquals(5, trivia.getRondaActual());

    verify(preguntas, times(1))
            .obtenerParaCasillero(any(), eq(DificultadRuleta.FACIL));
    assertTrue(trivia.estaFinalizada());
}


@Test
void deberiaRegistrarElTiempoDeRespuestaDeUnJugador() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Clock reloj = mock(Clock.class);

    Instant inicio = Instant.parse("2026-01-01T12:00:00Z");
    Instant respuesta = Instant.parse("2026-01-01T12:00:04Z");

    when(reloj.instant()).thenReturn(inicio, respuesta);

    ServicioTriviaImpl servicioConReloj =
            new ServicioTriviaImpl(servicioParticipacion, preguntas, reloj);

    Trivia trivia = servicioConReloj.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // Ejecución
    servicioConReloj.responderPregunta(trivia, jugador1, 1);

    // Verificación
    assertEquals(4000L, trivia.getTiempoAcumuladoMilisegundos(jugador1));
}


@Test
void deberiaGanarQuienAcumulaMenorTiempoAlCompletarCincoRondas() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    AtomicReference<Instant> ahora = new AtomicReference<>(
            Instant.parse("2026-01-01T12:00:00Z")
    );

    Clock reloj = mock(Clock.class);
    when(reloj.instant()).thenAnswer(invocacion -> ahora.get());

    ServicioTriviaImpl servicioConReloj =
            new ServicioTriviaImpl(servicioParticipacion, preguntas, reloj);

    Trivia trivia = servicioConReloj.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    for (int ronda = 1; ronda <= 5; ronda++) {
        ahora.set(ahora.get().plusSeconds(2));
        servicioConReloj.responderPregunta(trivia, jugador1, 1);

        ahora.set(ahora.get().plusSeconds(3));
        servicioConReloj.responderPregunta(trivia, jugador2, 1);
    }

    assertEquals(10000L,
            trivia.getTiempoAcumuladoMilisegundos(jugador1));
    assertEquals(25000L,
            trivia.getTiempoAcumuladoMilisegundos(jugador2));

    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
    assertEquals(5, trivia.getRondaActual());
}



@Test
void deberiaEliminarAlJugadorQueNoRespondioCuandoVenceLaRonda() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Clock reloj = mock(Clock.class);
    Instant inicio = Instant.parse("2026-01-01T12:00:00Z");

    when(reloj.instant()).thenReturn(
            inicio,
            inicio.plusSeconds(2),
            inicio.plusSeconds(11)
    );

    ServicioTriviaImpl servicioConLimite = new ServicioTriviaImpl(
            servicioParticipacion,
            preguntas,
            reloj,
            Duration.ofSeconds(10)
    );

    Trivia trivia = servicioConLimite.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // El jugador 1 responde correctamente a los 2 segundos
    servicioConLimite.responderPregunta(trivia, jugador1, 1);

    // A los 11 segundos se intenta cerrar la ronda
    servicioConLimite.cerrarRondaPorTiempo(trivia);

    // Verificación
    assertFalse(trivia.getParticipantes().contains(jugador2));
    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}




@Test
void deberiaFinalizarSinGanadorSiNadieRespondeAntesDelVencimiento() {
    // Preparación
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Clock reloj = mock(Clock.class);
    Instant inicio = Instant.parse("2026-01-01T12:00:00Z");

    when(reloj.instant()).thenReturn(
            inicio,
            inicio.plusSeconds(11)
    );

    ServicioTriviaImpl servicioConLimite = new ServicioTriviaImpl(
            servicioParticipacion,
            preguntas,
            reloj,
            Duration.ofSeconds(10)
    );

    Trivia trivia = servicioConLimite.iniciarTrivia(
            partida, DificultadRuleta.MEDIO
    );

    // Ningún jugador responde
    servicioConLimite.cerrarRondaPorTiempo(trivia);

    // Verificación
    assertTrue(trivia.getParticipantes().isEmpty());
    assertNull(trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}



@Test
void deberiaExcluirAlJugadorQueAbandonaDuranteLaTrivia() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // El primer jugador ya respondió correctamente.
    servicio.responderPregunta(trivia, jugador1, 1);

    // El segundo abandona durante la trivia.
    jugador2.setAbandono(true);

    when(servicioParticipacion.estaActivo(jugador2))
            .thenReturn(false);

    servicio.actualizarParticipantes(trivia);

    assertFalse(trivia.getParticipantes().contains(jugador2));
    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}


@Test
void deberiaFinalizarSinGanadorSiNoHayPreguntasDisponibles() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.FACIL)))
            .thenReturn(List.of());

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    assertTrue(trivia.estaFinalizada());
    assertNull(trivia.getGanador());
}


@Test
void deberiaEliminarAlJugadorQueRespondeFueraDeTiempo() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Clock reloj = mock(Clock.class);
    Instant inicio = Instant.parse("2026-01-01T12:00:00Z");

    when(reloj.instant()).thenReturn(
            inicio,
            inicio.plusSeconds(2),
            inicio.plusSeconds(11)
    );

    ServicioTriviaImpl servicioConLimite = new ServicioTriviaImpl(
            servicioParticipacion,
            preguntas,
            reloj,
            Duration.ofSeconds(10)
    );

    Trivia trivia = servicioConLimite.iniciarTrivia(
            partida, DificultadRuleta.FACIL);

    servicioConLimite.responderPregunta(trivia, jugador1, 1);
    servicioConLimite.responderPregunta(trivia, jugador2, 1);

    assertFalse(trivia.getParticipantes().contains(jugador2));
    assertFalse(trivia.yaRespondioEnRonda(jugador2));
    assertSame(jugador1, trivia.getGanador());
    assertTrue(trivia.estaFinalizada());
}


@Test
void noDeberiaCerrarUnaRondaAntesDeQueVenzaElTiempo() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Clock reloj = mock(Clock.class);
    Instant inicio = Instant.parse("2026-01-01T12:00:00Z");

    when(reloj.instant()).thenReturn(
            inicio,
            inicio.plusSeconds(5)
    );

    ServicioTriviaImpl servicioConLimite = new ServicioTriviaImpl(
            servicioParticipacion,
            preguntas,
            reloj,
            Duration.ofSeconds(10)
    );

    Trivia trivia = servicioConLimite.iniciarTrivia(
            partida, DificultadRuleta.FACIL);

    servicioConLimite.cerrarRondaPorTiempo(trivia);

    assertEquals(2, trivia.getParticipantes().size());
    assertEquals(1, trivia.getRondaActual());
    assertFalse(trivia.estaFinalizada());
}


@Test
void deberiaFinalizarSinPremioSiUnAbandonoTerminaLaPartida() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.DIFICIL);

    servicio.responderPregunta(trivia, jugador1, 1);

    // El sistema de partidas confirma que un abandono
    // provocó la finalización de la partida.
    partida.setEstado(EstadoPartida.FINALIZADA);

    servicio.cancelarPorAbandonoQueFinalizaPartida(trivia);

    assertTrue(trivia.estaFinalizada());
    assertNull(trivia.getGanador());
    assertFalse(trivia.estaPremioEntregado());
}


@Test
void noDeberiaAceptarUnaOpcionDeRespuestaInvalida() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.FACIL);

    // Las opciones válidas tienen índices de 0 a 3.
    assertThrows(IllegalArgumentException.class, () ->
            servicio.responderPregunta(trivia, jugador1, 4)
    );

    assertFalse(trivia.yaRespondioEnRonda(jugador1));
    assertTrue(trivia.getParticipantes().contains(jugador1));
    assertFalse(trivia.estaFinalizada());
}


@Test
void deberiaFinalizarSinGanadorSiNoHayJugadoresHabilitados() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    jugador1.getEstadoCarcel().encarcelar();
    jugador2.getEstadoCarcel().encarcelar();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.FACIL);

    assertTrue(trivia.getParticipantes().isEmpty());
    assertTrue(trivia.estaFinalizada());
    assertNull(trivia.getGanador());

    verifyNoInteractions(preguntas);
}



@Test
void deberiaJugarSoloLasRondasQueTienenPregunta() {
    Jugador jugador1 = new Jugador();
    Jugador jugador2 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1, jugador2));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada primera = new PreguntaGenerada("FACIL", "¿Uno?",
            List.of("A", "B", "C", "D"), 1);
    PreguntaGenerada segunda = new PreguntaGenerada("FACIL", "¿Dos?",
            List.of("E", "F", "G", "H"), 1);

    when(preguntas.obtenerParaCasillero(any(), eq(DificultadRuleta.FACIL)))
            .thenReturn(enPreguntasTrivia(primera, segunda));

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    for (int ronda = 1; ronda <= 2; ronda++) {
        servicio.responderPregunta(trivia, jugador1, 1);
        servicio.responderPregunta(trivia, jugador2, 1);
    }

    assertEquals(2, trivia.getTotalRondas());
    assertTrue(trivia.estaFinalizada());
}

@Test
void deberiaPedirLasPreguntasConElCodigoDeLaPartida() {
    Jugador jugador1 = new Jugador();

    Partida partida = new Partida();
    partida.setCodigoUnico("ABC123");
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    servicio.iniciarTrivia(partida, DificultadRuleta.MEDIO);

    verify(preguntas).obtenerParaCasillero("ABC123", DificultadRuleta.MEDIO);
}

@Test
void deberiaFinalizarSinGanadorSiElServicioDePreguntasNoDevuelveNada() {
    Jugador jugador1 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);
    when(preguntas.obtenerParaCasillero(any(), any())).thenReturn(null);

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    assertTrue(trivia.estaFinalizada());
}

@Test
void noDeberiaJugarMasDeCincoRondasAunqueHayaMasPreguntas() {
    Jugador jugador1 = new Jugador();

    Partida partida = new Partida();
    partida.setEstado(EstadoPartida.EN_CURSO);
    partida.setJugadores(List.of(jugador1));

    when(servicioParticipacion.estaActivo(any(Jugador.class)))
            .thenReturn(true);

    PreguntaGenerada pregunta = new PreguntaGenerada("FACIL", "¿Uno?",
            List.of("A", "B", "C", "D"), 1);
    when(preguntas.obtenerParaCasillero(any(), any()))
            .thenReturn(enPreguntasTrivia(pregunta, pregunta, pregunta, pregunta, pregunta, pregunta));

    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.FACIL);

    assertEquals(5, trivia.getTotalRondas());
}
}
