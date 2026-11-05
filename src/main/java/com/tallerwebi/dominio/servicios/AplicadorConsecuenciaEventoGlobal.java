package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.catalogo.CatalogoTablero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import org.springframework.stereotype.Component;

/** Aplica una consecuencia dentro de la transacción que resuelve el evento. */
@Component
public class AplicadorConsecuenciaEventoGlobal {

    private final ServicioEconomia economia;
    private final ServicioMovimiento movimiento;
    private final ServicioGestionQuiebra quiebras;
    private final ServicioParticipacion participacion;
    private final ServicioCasillero casilleros;
    private final ServicioPropiedad propiedades;
    private final ServicioEventoCasillero eventosCasillero;

    // Conserva el constructor empleado por las pruebas unitarias existentes.
    public AplicadorConsecuenciaEventoGlobal(
        ServicioEconomia economia,
        ServicioMovimiento movimiento,
        ServicioGestionQuiebra quiebras,
        ServicioParticipacion participacion
    ) {
        this(economia, movimiento, quiebras, participacion, null, null, null);
    }

    @Autowired
    public AplicadorConsecuenciaEventoGlobal(
        ServicioEconomia economia,
        ServicioMovimiento movimiento,
        ServicioGestionQuiebra quiebras,
        ServicioParticipacion participacion,
        ServicioCasillero casilleros,
        ServicioPropiedad propiedades,
        ServicioEventoCasillero eventosCasillero
    ) {
        this.economia = economia;
        this.movimiento = movimiento;
        this.quiebras = quiebras;
        this.participacion = participacion;
        this.casilleros = casilleros;
        this.propiedades = propiedades;
        this.eventosCasillero = eventosCasillero;
    }

    public String aplicar(Partida partida, EventoGlobal evento, Jugador jugador) {
        if (!participacion.estaActivo(jugador)) {
            throw new IllegalStateException("El jugador seleccionado dejó de estar activo.");
        }

        Boolean esMovimiento = evento.getTipoEventoGlobal().requiereDados();
        Boolean estaPreso = Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado());
        Integer posicionAnterior = jugador.getPosicionActual();
        String detalle;

        if (esMovimiento && estaPreso) {
            detalle = "no puede desplazarse porque está preso en la comisaría.";
        } else {
            detalle = ejecutarConsecuencia(partida, evento, jugador);
            // Solo se resuelve la caída si la ficha efectivamente cambió de casillero.
            if (resuelveDestino(evento) && !Objects.equals(posicionAnterior, jugador.getPosicionActual())) {
                detalle += resolverCaidaPorEventoGlobal(partida, jugador);
            }
        }

        return jugador.getApodo() + ": " + detalle;
    }

    private Boolean resuelveDestino(EventoGlobal evento) {
        return evento.getTipoEventoGlobal() == TipoEventoGlobal.INUNDACION
            || evento.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP;
    }

    
    public Boolean hayDeudas(Partida partida) {
        return partida.getJugadores().stream().filter(participacion::estaActivo)
            .anyMatch(jugador ->
                        quiebras.obtenerDeudaPendiente(partida.getCodigoUnico(), jugador.getId()) != null);
        }
        
        private String avanzarHastaPrimero(Partida partida, Jugador jugador) {
        Integer destino = partida.getJugadores().stream().filter(participacion::estaActivo).mapToInt(Jugador::getPosicionActual).max().orElse(jugador.getPosicionActual());
            if (Objects.equals(destino, jugador.getPosicionActual())) {
                return "ya está en la posición más adelantada; no necesita avanzar.";
        }
        return mover(jugador, destino - jugador.getPosicionActual());
    }

    private String volverAInicio(Jugador jugador) {
        if (jugador.getPosicionActual() == 0) return "ya está en Inicio; no necesita desplazarse.";
        return mover(jugador, -jugador.getPosicionActual());
    }

    private String mover(Jugador jugador, int desplazamiento) {
        int anterior = jugador.getPosicionActual();
        movimiento.moverJugador(jugador, desplazamiento, CatalogoTablero.TOTAL_CASILLEROS);
        return "se mueve del casillero " + anterior + " al " + jugador.getPosicionActual() + ".";
    }
    

    // No ejecuta una segunda tarjeta al moverse por efecto de la primera.
    private String resolverCaidaPorEventoGlobal(Partida partida, Jugador jugador) {
        String detalle = resolverTarjetaDeDestino(partida, jugador);
        if (participacion.estaActivo(jugador)) {
            detalle += resolverCasilleroFinal(partida, jugador);
        }
        return detalle;
    }

    private String resolverTarjetaDeDestino(Partida partida, Jugador jugador) {
        String detalle = "";
        if (eventosCasillero != null && casilleros != null && esCasilleroEvento(jugador.getPosicionActual())) {
            Optional<ResultadoEvento> resultado = eventosCasillero.aplicarSiCorresponde(partida.getCodigoUnico(), jugador);

            if (resultado.isPresent()) {
                partida.getEventoGlobal().guardarTarjeta(resultado.get().evento());
                detalle = " Cayó en EVENTOS y recibió «" + resultado.get().evento().getNombre() + "»: " + resultado.get().evento().getDescripcion();
            }
        }
        return detalle;
    }

    private Boolean esCasilleroEvento(Integer posicion) {
        return casilleros.obtenerTodosLosCasilleros().stream().filter(CasilleroEvento.class::isInstance).map(CasilleroEvento.class::cast).anyMatch(casillero -> Objects.equals(posicion, casillero.getPosicion()));
    }

    private String resolverCasilleroFinal(Partida partida, Jugador jugador) {
        String detalle = "";
        if (Objects.equals(jugador.getPosicionActual(), 8)) {
            jugador.getEstadoCarcel().encarcelar();
            detalle = " Llegó a la comisaría y quedó preso.";
        } else if (casilleros != null && propiedades != null) {
            detalle = cobrarAlquilerDeDestino(partida, jugador);
        }
        return detalle;
    }

    private String cobrarAlquilerDeDestino(Partida partida, Jugador jugador) {
        String detalle = "";
        CasilleroPropiedad casillero = casilleros.buscarCasilleroPropiedad(
            partida, jugador.getPosicionActual());
        registrarCompraDisponible(partida, jugador, casillero);
        if (casillero != null && casillero.getPropiedad() != null) {
            Propiedad propiedad = casillero.getPropiedad();
            Jugador propietario = propiedad.getPropietario();
            Boolean esAjena = propietario != null
                && !Objects.equals(propietario.getId(), jugador.getId());
            if (esAjena && quiebras.obtenerDeudaPendiente(partida.getCodigoUnico(), jugador.getId()) == null) {
                Integer alquiler = propiedades.calcularAlquiler(casillero, propiedad);
                ResultadoDeuda resultado = quiebras.procesarAlquiler(
                    partida.getCodigoUnico(), propiedad, jugador, alquiler);
                detalle = " Cayó en «" + propiedad.getNombre() + "», propiedad de "
                    + propietario.getApodo() + ". Alquiler: $" + alquiler + ".";
                if (resultado == ResultadoDeuda.PENDIENTE) {
                    detalle += " Debe resolver su deuda antes de continuar.";
                } else if (resultado == ResultadoDeuda.QUIEBRA) {
                    detalle += " Quedó en quiebra.";
                }
            }
        }
        return detalle;
    }

    private void registrarCompraDisponible(Partida partida, Jugador jugador, CasilleroPropiedad casillero) {
        Boolean estaLibre = casillero == null || propiedades.estaDisponible(casillero.getPropiedad());
        if (estaLibre && casilleros.existePosicionDePropiedad(partida, jugador.getPosicionActual())) {
            partida.getEventoGlobal().ofrecerCompra(jugador.getPosicionActual());
        }
    }

    private String cobrarRobo(Partida partida, Jugador jugador, Integer monto) {
        if (economia.tieneDineroSuficiente(jugador, monto)) {
            economia.restarDinero(jugador, monto);
            return "fue víctima de un robo y perdió $" + monto + ".";
        }
        quiebras.registrarDeuda(partida.getCodigoUnico(),
            jugador, null, monto, "ROBO_EVENTO_GLOBAL");
        return (
            "sufrió un robo de $" + monto +
            ". Tiene $" + jugador.getDinero() +
            " disponibles y debe pagar $" + monto +
            " antes de continuar.");
    }
    
    private String premiar(Jugador jugador, int monto) {
        int saldoAnterior = jugador.getDinero();
        economia.sumarDinero(jugador, monto);
        return ("ayudó a empujar el Fiat 600 de Germancho. Tenía $" + saldoAnterior + ", recibió $" + monto + " y ahora tiene $" + jugador.getDinero() + ".");
    }
    
    private String perderTurno(Jugador jugador) {
        jugador.setPierdeTurno(true);
        return "perderá su próxima oportunidad de turno por el accidente del Sarmiento.";
    }
    
    // Ejecuta la consecuencia correspondiente al tipo de evento global.
    private String ejecutarConsecuencia(Partida partida, EventoGlobal evento, Jugador jugador) {
        return switch (evento.getTipoEventoGlobal()) {
            case TRAMITES_AFIP -> avanzarHastaPrimero(partida, jugador);
            case INUNDACION -> mover(jugador, evento.getValor());
            case PIQUETE -> volverAInicio(jugador);
            case ROBO -> cobrarRobo(partida, jugador, evento.getValor());
            case FIAT_600 -> premiar(jugador, evento.getValor());
            case ACCIDENTE_SARMIENTO -> perderTurno(jugador);
        };
    }
}
