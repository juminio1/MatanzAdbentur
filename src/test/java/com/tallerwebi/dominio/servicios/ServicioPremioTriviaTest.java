

package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ServicioPremioTriviaTest {

    private final ServicioEconomia economia =
            mock(ServicioEconomia.class);

    private final ServicioPropiedad servicioPropiedad =
            mock(ServicioPropiedad.class);

    private final RepositorioPropiedad repositorioPropiedad =
            mock(RepositorioPropiedad.class);

    private final ServicioPremioTriviaImpl servicio =
            new ServicioPremioTriviaImpl(
                    economia,
                    servicioPropiedad,
                    repositorioPropiedad
            );

    private Jugador crearJugador(Long id) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        return jugador;
    }

    private Trivia crearTriviaGanada(DificultadRuleta dificultad,
                                    Jugador ganador) {
        Trivia trivia = new Trivia();
        trivia.setDificultad(dificultad);
        trivia.declararGanador(ganador);
        trivia.finalizar();
        return trivia;
    }

    @Test
    void deberiaEntregarCienAlGanarTriviaFacil() {
        Jugador ganador = crearJugador(1L);
        Trivia trivia = crearTriviaGanada(
                DificultadRuleta.FACIL, ganador);

        servicio.entregarPremioEnDinero(trivia);

        verify(economia).sumarDinero(ganador, 100);
    }

    @Test
    void deberiaEntregarDoscientosCincuentaAlGanarTriviaMedia() {
        Jugador ganador = crearJugador(1L);
        Trivia trivia = crearTriviaGanada(
                DificultadRuleta.MEDIO, ganador);

        servicio.entregarPremioEnDinero(trivia);

        verify(economia).sumarDinero(ganador, 250);
    }

    @Test
    void deberiaEntregarQuinientosSiElGanadorDificilEligeDinero() {
        Jugador ganador = crearJugador(1L);
        Trivia trivia = crearTriviaGanada(
                DificultadRuleta.DIFICIL, ganador);

        servicio.entregarPremioEnDinero(trivia);

        verify(economia).sumarDinero(ganador, 500);
        verifyNoInteractions(servicioPropiedad);
    }

    @Test
    void deberiaEntregarQuinientosSiNoHayPropiedadesParaRobar() {
        Jugador ganador = crearJugador(1L);
        Jugador rival = crearJugador(2L);

        Partida partida = new Partida();
        partida.setJugadores(List.of(ganador, rival));

        Trivia trivia = crearTriviaGanada(
                DificultadRuleta.DIFICIL, ganador);

        when(repositorioPropiedad
                .buscarPropiedadesPorPropietario(rival))
                .thenReturn(List.of());

        servicio.entregarPremioRobandoPropiedad(
                trivia, partida, null);

        verify(repositorioPropiedad)
                .buscarPropiedadesPorPropietario(rival);

        verify(economia).sumarDinero(ganador, 500);
        verifyNoInteractions(servicioPropiedad);
    }

    @Test
    void deberiaTransferirPropiedadSinCobrarAlGanadorDificil() {
        Jugador ganador = crearJugador(1L);
        Jugador rival = crearJugador(2L);

        Partida partida = new Partida();
        partida.setJugadores(List.of(ganador, rival));

        Trivia trivia = crearTriviaGanada(
                DificultadRuleta.DIFICIL, ganador);

        Propiedad propiedad = new Propiedad();
        propiedad.setId(10L);
        propiedad.setPropietario(rival);
        propiedad.setNivelMejora(2);

        when(repositorioPropiedad
                .buscarPropiedadesPorPropietario(rival))
                .thenReturn(List.of(propiedad));

        servicio.entregarPremioRobandoPropiedad(
                trivia, partida, 10L);

        verify(servicioPropiedad)
                .transferirPropiedadParaJugadores(
                        10L, 2L, 1L, 0);

        verify(economia, never())
                .sumarDinero(ganador, 500);
    }

    
@Test
void noDeberiaEntregarDosVecesElPremioDeUnaMismaTrivia() {
    // Preparación
    Jugador ganador = crearJugador(1L);

    Trivia trivia = crearTriviaGanada(
            DificultadRuleta.FACIL, ganador
    );

    // Ejecución
    servicio.entregarPremioEnDinero(trivia);
    servicio.entregarPremioEnDinero(trivia);

    // Verificación
    verify(economia, times(1))
            .sumarDinero(ganador, 100);
}


@Test
void noDeberiaRobarDosVecesPorLaMismaTrivia() {
    // Preparación
    Jugador ganador = crearJugador(1L);
    Jugador rival = crearJugador(2L);

    Partida partida = new Partida();
    partida.setJugadores(List.of(ganador, rival));

    Trivia trivia = crearTriviaGanada(
            DificultadRuleta.DIFICIL, ganador
    );

    Propiedad propiedad = new Propiedad();
    propiedad.setId(10L);
    propiedad.setPropietario(rival);

    when(repositorioPropiedad
            .buscarPropiedadesPorPropietario(rival))
            .thenReturn(List.of(propiedad));

    // Ejecución
    servicio.entregarPremioRobandoPropiedad(
            trivia, partida, 10L);

    servicio.entregarPremioRobandoPropiedad(
            trivia, partida, 10L);

    // Verificación
    verify(servicioPropiedad, times(1))
            .transferirPropiedadParaJugadores(
                    10L, 2L, 1L, 0);
}


@Test
void noDeberiaPermitirRobarUnaPropiedadSiYaCobroElPremioEnDinero() {
    // Preparación
    Jugador ganador = crearJugador(1L);
    Jugador rival = crearJugador(2L);

    Partida partida = new Partida();
    partida.setJugadores(List.of(ganador, rival));

    Trivia trivia = crearTriviaGanada(
            DificultadRuleta.DIFICIL, ganador);

    Propiedad propiedad = new Propiedad();
    propiedad.setId(10L);
    propiedad.setPropietario(rival);

    when(repositorioPropiedad
            .buscarPropiedadesPorPropietario(rival))
            .thenReturn(List.of(propiedad));

    // Ejecución
    servicio.entregarPremioEnDinero(trivia);

    servicio.entregarPremioRobandoPropiedad(
            trivia, partida, 10L);

    // Verificación
    verify(economia, times(1))
            .sumarDinero(ganador, 500);

    verifyNoInteractions(servicioPropiedad);
    verifyNoInteractions(repositorioPropiedad);

    assertTrue(trivia.estaPremioEntregado());
}


@Test
void noDeberiaPermitirCobrarDineroSiYaRoboUnaPropiedad() {
    // Preparación
    Jugador ganador = crearJugador(1L);
    Jugador rival = crearJugador(2L);

    Partida partida = new Partida();
    partida.setJugadores(List.of(ganador, rival));

    Trivia trivia = crearTriviaGanada(
            DificultadRuleta.DIFICIL, ganador);

    Propiedad propiedad = new Propiedad();
    propiedad.setId(10L);
    propiedad.setPropietario(rival);

    when(repositorioPropiedad
            .buscarPropiedadesPorPropietario(rival))
            .thenReturn(List.of(propiedad));

    // Ejecución
    servicio.entregarPremioRobandoPropiedad(
            trivia, partida, 10L);

    servicio.entregarPremioEnDinero(trivia);

    // Verificación
    verify(servicioPropiedad, times(1))
            .transferirPropiedadParaJugadores(
                    10L, 2L, 1L, 0);

    verify(economia, never())
            .sumarDinero(ganador, 500);

    assertTrue(trivia.estaPremioEntregado());
}

}
