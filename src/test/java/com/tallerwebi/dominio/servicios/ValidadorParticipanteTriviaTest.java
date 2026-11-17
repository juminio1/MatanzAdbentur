package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ValidadorParticipanteTriviaTest {

    private ValidadorParticipanteTrivia validador;
    private Partida partida;
    private Jugador ana;

    @BeforeEach
    public void init() {
        validador = new ValidadorParticipanteTrivia(new ServicioParticipacionImpl());
        ana = jugador(1L, "Ana");
        partida = new Partida();
        partida.setJugadores(List.of(ana, jugador(2L, "Luna")));
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        return jugador;
    }

    @Test
    public void deberiaPermitirParticiparAUnJugadorActivoDeLaPartida() {
        assertDoesNotThrow(() -> validador.validarHabilitado(partida, 1L));
    }

    @Test
    public void deberiaRechazarAUnJugadorQueNoPerteneceALaPartida() {
        ParticipanteTriviaNoHabilitadoException error = assertThrows(ParticipanteTriviaNoHabilitadoException.class,
                () -> validador.validarHabilitado(partida, 99L));

        assertEquals("No pertenecés a esta partida.", error.getMessage());
    }

    @Test
    public void deberiaRechazarCuandoNoSeIdentificoAlJugador() {
        assertThrows(ParticipanteTriviaNoHabilitadoException.class, () -> validador.validarHabilitado(partida, null));
    }

    @Test
    public void deberiaRechazarAUnJugadorPreso() {
        ana.getEstadoCarcel().encarcelar();

        ParticipanteTriviaNoHabilitadoException error = assertThrows(ParticipanteTriviaNoHabilitadoException.class,
                () -> validador.validarHabilitado(partida, 1L));

        assertEquals("Los jugadores presos no pueden participar de la trivia.", error.getMessage());
    }

    @Test
    public void deberiaRechazarAUnJugadorEnBancarrota() {
        ana.setEnBancarrota(true);

        ParticipanteTriviaNoHabilitadoException error = assertThrows(ParticipanteTriviaNoHabilitadoException.class,
                () -> validador.validarHabilitado(partida, 1L));

        assertEquals("Los jugadores eliminados no pueden participar de la trivia.", error.getMessage());
    }

    @Test
    public void deberiaRechazarAUnJugadorQueAbandono() {
        ana.setAbandono(true);

        assertThrows(ParticipanteTriviaNoHabilitadoException.class, () -> validador.validarHabilitado(partida, 1L));
    }
}
