
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
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import java.time.Clock;
import java.time.Instant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicioTriviaTest {

    private final ServicioParticipacion servicioParticipacion =
        mock(ServicioParticipacion.class);

    private final ClienteIA clienteIA =
        mock(ClienteIA.class);

    private final ServicioTriviaImpl servicio =
        new ServicioTriviaImpl(servicioParticipacion, clienteIA);

    
@BeforeEach
void configurarPreguntaPorDefecto() {
    PreguntaGenerada pregunta = new PreguntaGenerada(
            "FACIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(clienteIA.generarPreguntas(anyString(), eq(1)))
            .thenReturn(List.of(pregunta));
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

    when(clienteIA.generarPreguntas("DIFICIL", 1))
        .thenReturn(List.of(pregunta));

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

    ClienteIA clienteIA = mock(ClienteIA.class);

    PreguntaGenerada pregunta = new PreguntaGenerada(
            "DIFICIL",
            "¿Cuál es la capital de Argentina?",
            List.of("Córdoba", "Buenos Aires", "Rosario", "Mendoza"),
            1
    );

    when(clienteIA.generarPreguntas("DIFICIL", 1))
            .thenReturn(List.of(pregunta));

    ServicioTriviaImpl servicioConIA =
            new ServicioTriviaImpl(servicioParticipacion, clienteIA);

    // Ejecución
    servicioConIA.iniciarTrivia(partida, DificultadRuleta.DIFICIL);

    // Verificación
    verify(clienteIA).generarPreguntas("DIFICIL", 1);
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

    when(clienteIA.generarPreguntas("DIFICIL", 1))
            .thenReturn(List.of(pregunta));

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

    when(clienteIA.generarPreguntas("DIFICIL", 1))
            .thenReturn(List.of(pregunta));

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

    when(clienteIA.generarPreguntas("DIFICIL", 1))
            .thenReturn(List.of(pregunta));

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

    when(clienteIA.generarPreguntas("DIFICIL", 1))
            .thenReturn(List.of(pregunta));

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

    when(clienteIA.generarPreguntas("MEDIO", 1))
            .thenReturn(List.of(pregunta));

    // Ejecución
    Trivia trivia = servicio.iniciarTrivia(partida, DificultadRuleta.MEDIO);

    // Verificación
    assertEquals(3, trivia.getParticipantes().size());
    assertSame(pregunta, trivia.getPreguntaActual());

    verify(clienteIA).generarPreguntas("MEDIO", 1);
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

    when(clienteIA.generarPreguntas("FACIL", 1))
            .thenReturn(
                    List.of(preguntaRonda1),
                    List.of(preguntaRonda2)
            );

    Trivia trivia = servicio.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // Ejecución
    servicio.responderPregunta(trivia, jugador1, 1);
    servicio.responderPregunta(trivia, jugador2, 1);

    // Verificación
    assertEquals(2, trivia.getRondaActual());
    assertSame(preguntaRonda2, trivia.getPreguntaActual());
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

    verify(clienteIA, times(5))
            .generarPreguntas("FACIL", 1);
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
            new ServicioTriviaImpl(servicioParticipacion, clienteIA, reloj);

    Trivia trivia = servicioConReloj.iniciarTrivia(
            partida, DificultadRuleta.FACIL
    );

    // Ejecución
    servicioConReloj.responderPregunta(trivia, jugador1, 1);

    // Verificación
    assertEquals(4000L, trivia.getTiempoAcumuladoMilisegundos(jugador1));
}


}
