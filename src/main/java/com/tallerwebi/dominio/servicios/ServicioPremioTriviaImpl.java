
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ServicioPremioTriviaImpl {

    private static final int PREMIO_FACIL = 100;
    private static final int PREMIO_MEDIO = 250;
    private static final int PREMIO_DIFICIL = 500;

    private final ServicioEconomia economia;
    private final ServicioPropiedad servicioPropiedad;
    private final RepositorioPropiedad repositorioPropiedad;

    public ServicioPremioTriviaImpl(
            ServicioEconomia economia,
            ServicioPropiedad servicioPropiedad,
            RepositorioPropiedad repositorioPropiedad) {

        this.economia = economia;
        this.servicioPropiedad = servicioPropiedad;
        this.repositorioPropiedad = repositorioPropiedad;
    }

    
    public void entregarPremioEnDinero(Trivia trivia) {
    Jugador ganador = obtenerGanador(trivia);

        if (trivia.estaPremioEntregado()) {
        return;
        }

        int premio = switch (trivia.getDificultad()) {
        case FACIL -> PREMIO_FACIL;
        case MEDIO -> PREMIO_MEDIO;
        case DIFICIL -> PREMIO_DIFICIL;
     };

    economia.sumarDinero(ganador, premio);
    trivia.registrarPremioEntregado();
}


    
public void entregarPremioRobandoPropiedad(
        Trivia trivia, Partida partida, Long idPropiedad) {

    Jugador ganador = obtenerGanador(trivia);

    if (trivia.estaPremioEntregado()) {
        return;
    }

    if (trivia.getDificultad() != DificultadRuleta.DIFICIL) {
        throw new IllegalStateException(
                "Solo la trivia difícil permite robar propiedades.");
    }

    validarPartida(partida, ganador);

    List<Propiedad> propiedades =
            obtenerPropiedadesRivales(partida, ganador);

    if (propiedades.isEmpty()) {
        economia.sumarDinero(ganador, PREMIO_DIFICIL);
        trivia.registrarPremioEntregado();
        return;
    }

    Propiedad elegida = propiedades.stream()
            .filter(propiedad ->
                    Objects.equals(propiedad.getId(), idPropiedad)
                    && idPropiedad != null)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                    "La propiedad elegida no está disponible."));

    servicioPropiedad.transferirPropiedadParaJugadores(
            elegida.getId(),
            elegida.getPropietario().getId(),
            ganador.getId(),
            0
    );

    trivia.registrarPremioEntregado();
}


    private Jugador obtenerGanador(Trivia trivia) {
        if (trivia == null || !trivia.estaFinalizada()
                || trivia.getGanador() == null) {
            throw new IllegalStateException(
                    "La trivia debe estar finalizada y tener un ganador.");
        }

        if (trivia.getDificultad() == null) {
            throw new IllegalStateException(
                    "La trivia no tiene una dificultad definida.");
        }

        return trivia.getGanador();
    }

    private void validarPartida(Partida partida, Jugador ganador) {
        if (partida == null || partida.getJugadores() == null
                || ganador.getId() == null) {
            throw new IllegalArgumentException(
                    "La partida o el ganador no son válidos.");
        }

        boolean pertenece = partida.getJugadores().stream()
                .filter(Objects::nonNull)
                .anyMatch(jugador ->
                        Objects.equals(jugador.getId(), ganador.getId()));

        if (!pertenece) {
            throw new IllegalArgumentException(
                    "El ganador no pertenece a la partida.");
        }
    }

    private List<Propiedad> obtenerPropiedadesRivales(
            Partida partida, Jugador ganador) {

        List<Propiedad> disponibles = new ArrayList<>();

        for (Jugador rival : partida.getJugadores()) {
            if (rival == null
                    || Objects.equals(rival.getId(), ganador.getId())) {
                continue;
            }

            List<Propiedad> propiedadesRival =
                    repositorioPropiedad.buscarPropiedadesPorPropietario(rival);

            if (propiedadesRival == null) {
                continue;
            }

            for (Propiedad propiedad : propiedadesRival) {
                if (perteneceAlRival(propiedad, rival)) {
                    disponibles.add(propiedad);
                }
            }
        }

        return disponibles;
    }

    private boolean perteneceAlRival(
            Propiedad propiedad, Jugador rival) {

        return propiedad != null
                && propiedad.getPropietario() != null
                && rival.getId() != null
                && Objects.equals(
                        propiedad.getPropietario().getId(),
                        rival.getId());
    }
}
