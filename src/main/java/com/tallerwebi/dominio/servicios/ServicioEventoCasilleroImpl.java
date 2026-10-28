
package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.excepcion.EventoCasilleroNoEncontradoException;
import com.tallerwebi.dominio.excepcion.TipoEfectoEventoNoSoportadoException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioEventoCasillero;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntBiFunction;
import com.tallerwebi.dominio.resultados.ResultadoEvento;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioEventoCasillero")
@Transactional
public class ServicioEventoCasilleroImpl implements ServicioEventoCasillero {

    private final RepositorioEventoCasillero repositorioEventoCasillero;
    private final ServicioCasillero servicioCasillero;
    private final ServicioEconomia economia;
    private final ServicioMovimiento movimiento;
    private final ServicioGestionQuiebra servicioGestionQuiebra;

    private final Map<TipoEfectoEvento, ToIntBiFunction<EventoCasillero, Jugador>> aplicadores;

    @Autowired
    public ServicioEventoCasilleroImpl(
            RepositorioEventoCasillero repositorioEventoCasillero,
            ServicioCasillero servicioCasillero,
            ServicioEconomia economia, ServicioMovimiento movimiento,
            ServicioGestionQuiebra servicioGestionQuiebra) {

        this.repositorioEventoCasillero = repositorioEventoCasillero;
        this.servicioCasillero = servicioCasillero;
        this.economia = economia;
        this.movimiento = movimiento;
        this.servicioGestionQuiebra = servicioGestionQuiebra;
        this.aplicadores = crearAplicadores();
    }

    @Override
    public Optional<ResultadoEvento> aplicarSiCorresponde(String codigoPartida, Jugador jugador) {

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

        int premio = 0;
        if (evento.getTipoEfecto() == TipoEfectoEvento.RESTA) {
            procesarEventoResta(codigoPartida, evento, jugador);
        } else {
            ToIntBiFunction<EventoCasillero, Jugador> aplicador =
                    aplicadores.get(evento.getTipoEfecto());

            if (aplicador == null) {
                throw new TipoEfectoEventoNoSoportadoException(evento.getTipoEfecto());
            }

            premio = aplicador.applyAsInt(evento, jugador);
        }

        return Optional.of(new ResultadoEvento(evento, premio));
    }

    private void procesarEventoResta(
            String codigoPartida,
            EventoCasillero evento,
            Jugador jugador) {

        Integer monto = evento.getMonto();

        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("Monto de evento inválido");
        }

        if (servicioGestionQuiebra.obtenerDeudaPendiente(
                codigoPartida, jugador.getId()) != null) {
            throw new IllegalStateException("El jugador ya tiene un pago pendiente");
        }

        if (economia.tieneDineroSuficiente(jugador, monto)) {
            economia.restarDinero(jugador, monto);
            return;
        }

        servicioGestionQuiebra.registrarDeuda(
                codigoPartida,
                jugador,
                null,
                monto,
                "EVENTO_RESTA"
        );

        servicioGestionQuiebra.evaluarDeuda(codigoPartida, jugador.getId());
    }

    private Boolean esCasilleroDeEvento(Integer posicion) {
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroEvento.class::isInstance)
                .map(CasilleroEvento.class::cast)
                .map(CasilleroEvento::getPosicion)
                .anyMatch(posicion::equals);
    }

    private Map<TipoEfectoEvento, ToIntBiFunction<EventoCasillero, Jugador>> crearAplicadores() {
        Map<TipoEfectoEvento, ToIntBiFunction<EventoCasillero, Jugador>> resultado =
                new EnumMap<>(TipoEfectoEvento.class);

        resultado.put(TipoEfectoEvento.SUMA,
                (evento, jugador) -> {
                    economia.sumarDinero(jugador, evento.getMonto());
                    return 0;
                });

        resultado.put(TipoEfectoEvento.PIERDE_TURNO,
                (evento, jugador) -> {
                    jugador.setPierdeTurno(true);
                    return 0;
                });

        resultado.put(TipoEfectoEvento.AVANZA_CASILLEROS, this::avanzarCasilleros);

        resultado.put(TipoEfectoEvento.AVANZA_A_POSICION, this::avanzarAPosicion);

        return Map.copyOf(resultado);
    }

    private int avanzarCasilleros(EventoCasillero evento, Jugador jugador) {
        return movimiento.avanzar(jugador, evento.getCantidadCasilleros());
    }
    private int avanzarAPosicion(EventoCasillero evento, Jugador jugador) {
        return movimiento.avanzarAPosicion(jugador, evento.getPosicionDestino());
    }
}
