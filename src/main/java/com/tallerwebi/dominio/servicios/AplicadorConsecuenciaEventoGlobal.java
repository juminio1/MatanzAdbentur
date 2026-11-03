package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

// Separa las consecuencias de la coordinación de tiradas para cumplir el límite de PMD.
public final class AplicadorConsecuenciaEventoGlobal {

    public static Boolean debeConservarElegido(EstadoEventoGlobal resultado, Partida partida, List<Long> activos) {
        return activos.contains(resultado.idJugadorElegido())
                || resultado.tipo() == TipoEventoGlobal.ROBO && partida != null && partida.getJugadores().stream()
                .anyMatch(jugador -> jugador.getId().equals(resultado.idJugadorElegido())
                        && Boolean.TRUE.equals(jugador.getEnBancarrota()) && !Boolean.TRUE.equals(jugador.getAbandono()));
    }

    public static Boolean hayMovimientoPendiente(Long idJugador, EventoGlobal evento) {
        return idJugador != null && evento != null;
    }

    private AplicadorConsecuenciaEventoGlobal() {
    }

    @FunctionalInterface
    public interface AplicadorEvento {
        public void ejecutar(Jugador jugador, EventoGlobal evento) throws EventoInexistenteException;
    }

    public static String aplicar(Partida partida, Jugador jugador, EventoGlobal evento, RepositorioPartida repositorio, ServicioGestionQuiebra gestionQuiebra, AplicadorEvento aplicadorEvento) throws EventoInexistenteException {
        String detalle;

        if (evento.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP) {
            detalle = aplicarAfip(partida, jugador);
        } else if (evento.getTipoEventoGlobal() == TipoEventoGlobal.ROBO) {
            detalle = aplicarRobo(partida, jugador, evento.getValor(), gestionQuiebra);
        } else {
            Integer posicionAnterior = jugador.getPosicionActual();
            aplicadorEvento.ejecutar(jugador, evento);
            detalle = obtenerDetalle(jugador, evento, posicionAnterior);
        }

        repositorio.actualizarJugador(jugador);
        return detalle;
    }

    // La selección aleatoria y la consecuencia pertenecen a la misma responsabilidad.
    public static void aplicarAlAzar(Partida partida, EventoGlobal evento, Random random, RepositorioPartida repositorio,
                                    ServicioGestionQuiebra gestionQuiebra, AplicadorEvento aplicadorEvento,
                                    BiConsumer<Jugador, String> alResolver) throws JugadorNoEncontradoException, EventoInexistenteException {
        List<Jugador> jugadores = partida.getJugadores();
        List<Jugador> activos = jugadores == null ? List.of() : jugadores.stream()
                .filter(ValidadorEventoGlobal::estaActivo)
                .toList();
        if (activos.isEmpty()) {
            throw new JugadorNoEncontradoException("No hay jugadores activos en la partida");
        }
        Jugador elegido = activos.get(random.nextInt(activos.size()));
        String detalle = aplicar(partida, elegido, evento, repositorio, gestionQuiebra, aplicadorEvento);
        alResolver.accept(elegido, detalle);
    }

    public static List<Jugador> obtenerJugadoresConResultadoElegido(List<Jugador> participantes, Map<Long, Integer> tiradas, TipoEventoGlobal tipoEvento) {
        List<Integer> puntosActivos = participantes.stream().map(jugador -> tiradas.get(jugador.getId()))
                .filter(Objects::nonNull).toList();
        if (puntosActivos.isEmpty()) {
            return List.of();
        }
        Integer resultadoElegido = tipoEvento == TipoEventoGlobal.PIQUETE
                ? Collections.min(puntosActivos) : Collections.max(puntosActivos);
        return participantes.stream().filter(jugador -> resultadoElegido.equals(tiradas.get(jugador.getId()))).toList();
    }

    public static Boolean modificaPosicion(EventoGlobal evento) {
        return evento.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP
                || evento.getTipoEventoGlobal() == TipoEventoGlobal.PIQUETE
                || evento.getTipoEventoGlobal() == TipoEventoGlobal.INUNDACION;
    }

    // La deuda del robo puede pagarse durante la presentación: su saldo debe reflejar ese cobro.
    public static Map<Long, Integer> obtenerSaldosResultado(EstadoEventoGlobal resultado, List<Jugador> activos) {
        return resultado.tipo() == TipoEventoGlobal.ROBO
                ? activos.stream().filter(ValidadorEventoGlobal::tieneSaldoRegistrable)
                        .collect(Collectors.toMap(Jugador::getId, Jugador::getDinero))
                : resultado.saldos();
    }

    public static Boolean obtenerDeudaRoboPendiente(TipoEventoGlobal tipo, String codigoPartida, Long elegido,
            ServicioGestionQuiebra gestionQuiebra) {
        return tipo == TipoEventoGlobal.ROBO && elegido != null && gestionQuiebra != null
                ? gestionQuiebra.obtenerDeudaPendiente(codigoPartida, elegido) != null : null;
    }

    public static String obtenerDetalleResultado(EstadoEventoGlobal resultado, Partida partida, Boolean deudaPendiente) {
        if (!Boolean.TRUE.equals(resultado.deudaRoboPendiente())) {
            return resultado.detalle();
        }
        if (partida != null && partida.getJugadores().stream().anyMatch(jugador ->
                Objects.equals(jugador.getId(), resultado.idJugadorElegido()) && Boolean.TRUE.equals(jugador.getEnBancarrota()))) {
            return "no pudo saldar la deuda por robo y quedó en quiebra.";
        }
        return Boolean.FALSE.equals(deudaPendiente)
                ? "saldó su deuda por robo. Puede continuar la partida." : resultado.detalle();
    }

    public static String obtenerDetalleResultado(EstadoEventoGlobal resultado, Partida partida,
            Boolean deudaPendiente, Boolean conservarElegido) {
        if (!conservarElegido && resultado.idJugadorElegido() != null) {
            Boolean movimientoDiferido = Set.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION,
                    TipoEventoGlobal.PIQUETE).contains(resultado.tipo());
            return movimientoDiferido || partida == null
                    ? "El jugador seleccionado abandonó la partida; no recibirá la consecuencia."
                    : resultado.detalle() + " El jugador seleccionado abandonó después de registrarse la consecuencia.";
        }
        return obtenerDetalleResultado(resultado, partida, deudaPendiente);
    }

    public static Long obtenerIdElegidoVigente(EstadoEventoGlobal resultado, Boolean conservarElegido) {
        return conservarElegido ? resultado.idJugadorElegido() : null;
    }

    public static String previsualizarMovimiento(Partida partida, Jugador jugador, EventoGlobal evento) {
        Integer origen = jugador.getPosicionActual();
        Integer destino = switch (evento.getTipoEventoGlobal()) {
            case TRAMITES_AFIP -> partida.getJugadores().stream().filter(ValidadorEventoGlobal::estaActivo)
                    .map(Jugador::getPosicionActual).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(origen);
            case PIQUETE -> 0;
            case INUNDACION -> Math.floorMod(origen + evento.getValor(), 30);
            default -> origen;
        };
        if (evento.getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP && Objects.equals(origen, destino)) {
            return "no avanza porque ya es el jugador más adelantado del tablero.";
        }
        return Objects.equals(origen, destino)
                ? "ya estaba en el casillero " + destino + ", por eso no se moverá."
                : "se moverá del casillero " + origen + " al " + destino + " cuando se reanude la partida.";
    }

    private static String aplicarAfip(Partida partida, Jugador jugador) {
        Integer posicionAnterior = jugador.getPosicionActual();
        Integer posicionMasAdelantada = partida.getJugadores().stream().filter(ValidadorEventoGlobal::estaActivo).map(Jugador::getPosicionActual).filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(posicionAnterior);
        jugador.setPosicionActual(posicionMasAdelantada);

        return posicionAnterior.equals(posicionMasAdelantada)
                ? "no avanza porque ya es el jugador más adelantado del tablero."
                : "avanzó del casillero " + posicionAnterior + " al " + posicionMasAdelantada + ".";
    }

    private static String aplicarRobo(Partida partida, Jugador jugador, Integer monto, ServicioGestionQuiebra gestionQuiebra) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto del robo no es válido");
        }

        Integer saldo = jugador.getDinero() == null ? 0 : jugador.getDinero();
        if (saldo >= monto) {
            jugador.setDinero(saldo - monto);
            return "perdió $" + monto + " y ahora tiene $" + jugador.getDinero() + ".";
        }

        return registrarDeudaPorRobo(partida, jugador, monto, gestionQuiebra);
    }

    private static String registrarDeudaPorRobo(Partida partida, Jugador jugador, Integer monto, ServicioGestionQuiebra gestionQuiebra) {
        if (gestionQuiebra == null) {
            throw new IllegalStateException("No está disponible el servicio de gestión de quiebras");
        }

        String codigoPartida = partida.getCodigoUnico();
        gestionQuiebra.registrarDeuda(codigoPartida, jugador, null, monto, "EVENTO_GLOBAL_ROBO");

        try {
            ResultadoDeuda resultado = gestionQuiebra.evaluarDeuda(codigoPartida, jugador.getId());
            return switch (resultado) {
                case PENDIENTE -> "debe $" + monto + " por el robo. Necesita vender propiedades o declararse en quiebra.";
                case QUIEBRA -> "no pudo afrontar el robo de $" + monto + " y quedó en quiebra.";
                case RESUELTA -> "pagó $" + monto + " por el robo.";
                case SIN_DEUDA -> "no tiene una deuda pendiente por el robo.";
            };
        } catch (ActivacionEventoInvalidoException | PartidaNoEncontradaException | EventoGlobalInvalidoException
                | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException exception) {
            throw new IllegalStateException("No se pudo gestionar la deuda del evento global", exception);
        }
    }

    private static String obtenerDetalle(Jugador jugador, EventoGlobal evento, Integer posicionAnterior) {
        return switch (evento.getTipoEventoGlobal()) {
            case ACCIDENTE_SARMIENTO -> "perderá su próximo turno por el accidente del Sarmiento.";
            case FIAT_600 -> "ganó $" + evento.getValor() + ". Su saldo actual es $" + jugador.getDinero() + ".";
            case INUNDACION -> "retrocedió hasta el casillero " + jugador.getPosicionActual() + ".";
            case PIQUETE -> Objects.equals(posicionAnterior, jugador.getPosicionActual())
                    ? "ya estaba en Inicio (casillero 0), por eso no se movió."
                    : "volvió del casillero " + posicionAnterior + " al Inicio (casillero " + jugador.getPosicionActual() + ").";
            case TRAMITES_AFIP -> "se adelantó hasta el jugador que iba primero.";
            case ROBO -> "perdió dinero por el robo.";
        };
    }
}
