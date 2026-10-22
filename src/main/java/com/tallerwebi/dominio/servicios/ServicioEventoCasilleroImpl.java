package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.excepcion.EventoCasilleroNoEncontradoException;
import com.tallerwebi.dominio.excepcion.TipoEfectoEventoNoSoportadoException;
import com.tallerwebi.infraestructura.RepositorioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioEventoCasillero")
@Transactional
public class ServicioEventoCasilleroImpl implements ServicioEventoCasillero {
    private static final Integer TOTAL_CASILLEROS = 30;
    private static final Integer DINERO_POR_PASAR_POR_INICIO = 200;

    private final RepositorioEventoCasillero repositorioEventoCasillero;
    private final ServicioCasillero servicioCasillero;
    private final ServicioPartida servicioPartida;
    private final Map<TipoEfectoEvento, BiConsumer<EventoCasillero, Jugador>> aplicadores;

    @Autowired
    public ServicioEventoCasilleroImpl(RepositorioEventoCasillero repositorioEventoCasillero,
                                       ServicioCasillero servicioCasillero,
                                       ServicioPartida servicioPartida) {
        this.repositorioEventoCasillero = repositorioEventoCasillero;
        this.servicioCasillero = servicioCasillero;
        this.servicioPartida = servicioPartida;
        this.aplicadores = crearAplicadores();
    }

    @Override
    public Optional<EventoCasillero> aplicarSiCorresponde(Jugador jugador) {
        Integer posicion = jugador.getPosicionActual();
        if (posicion == null || !esCasilleroDeEvento(posicion)) {
            return Optional.empty();
        }

        List<EventoCasillero> eventos = repositorioEventoCasillero.obtenerTodos();
        if (eventos.isEmpty()) {
            throw new EventoCasilleroNoEncontradoException();
        }

        Integer indiceEvento = ThreadLocalRandom.current().nextInt(eventos.size());
        EventoCasillero evento = eventos.get(indiceEvento);
        BiConsumer<EventoCasillero, Jugador> aplicador = aplicadores.get(evento.getTipoEfecto());
        if (aplicador == null) {
            throw new TipoEfectoEventoNoSoportadoException(evento.getTipoEfecto());
        }

        aplicador.accept(evento, jugador);
        return Optional.of(evento);
    }

    private Boolean esCasilleroDeEvento(Integer posicion) {
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroEvento.class::isInstance)
                .map(CasilleroEvento.class::cast)
                .map(CasilleroEvento::getPosicion)
                .anyMatch(posicion::equals);
    }

    private Map<TipoEfectoEvento, BiConsumer<EventoCasillero, Jugador>> crearAplicadores() {
        Map<TipoEfectoEvento, BiConsumer<EventoCasillero, Jugador>> resultado =
                new EnumMap<>(TipoEfectoEvento.class);
        resultado.put(TipoEfectoEvento.SUMA,
                (evento, jugador) -> servicioPartida.sumarDinero(jugador, evento.getMonto()));
        resultado.put(TipoEfectoEvento.RESTA,
                (evento, jugador) -> servicioPartida.restarDinero(jugador, evento.getMonto()));
        resultado.put(TipoEfectoEvento.PIERDE_TURNO,
                (evento, jugador) -> jugador.setPierdeTurno(true));
        resultado.put(TipoEfectoEvento.AVANZA_CASILLEROS, this::avanzarCasilleros);
        resultado.put(TipoEfectoEvento.AVANZA_A_POSICION, this::avanzarAPosicion);
        return Map.copyOf(resultado);
    }

    private void avanzarCasilleros(EventoCasillero evento, Jugador jugador) {
        Integer posicionAnterior = jugador.getPosicionActual();
        servicioPartida.moverJugador(jugador, evento.getCantidadCasilleros(), TOTAL_CASILLEROS);
        sumarDineroSiPasoPorInicio(jugador, posicionAnterior, jugador.getPosicionActual());
    }

    private void avanzarAPosicion(EventoCasillero evento, Jugador jugador) {
        Integer posicionAnterior = jugador.getPosicionActual();
        Integer posicionDestino = evento.getPosicionDestino();
        jugador.setPosicionActual(posicionDestino);
        sumarDineroSiPasoPorInicio(jugador, posicionAnterior, posicionDestino);
    }

    private void sumarDineroSiPasoPorInicio(Jugador jugador, Integer posicionAnterior,
                                             Integer posicionDestino) {
        if (posicionDestino < posicionAnterior) {
            servicioPartida.sumarDinero(jugador, DINERO_POR_PASAR_POR_INICIO);
        }
    }
}
