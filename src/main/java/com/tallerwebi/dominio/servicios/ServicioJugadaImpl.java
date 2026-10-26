package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.excepcion.*;
import com.tallerwebi.dominio.resultados.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina una jugada completa dentro de una misma transacción. */
@Service
@Transactional(rollbackFor = Exception.class)
public class ServicioJugadaImpl implements ServicioJugada {
    private final ServicioPartida partidas;
    private final ServicioCasillero casilleros;
    private final ServicioPropiedad propiedades;
    private final ServicioEventoCasillero eventos;
    private final ServicioGestionQuiebra quiebras;
    private final ServicioMovimiento movimiento;
    public ServicioJugadaImpl(ServicioPartida partidas, ServicioCasillero casilleros,
            ServicioPropiedad propiedades, ServicioEventoCasillero eventos,
            ServicioGestionQuiebra quiebras, ServicioMovimiento movimiento) {
        this.partidas = partidas;
        this.casilleros = casilleros;
        this.propiedades = propiedades;
        this.eventos = eventos;
        this.quiebras = quiebras;
        this.movimiento = movimiento;
    }
    @Override
    public ResultadoJugada tirarYMover(String codigo, Long idJugador)
            throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        Partida partida = partidas.obtenerPartida(codigo);
        if (partida == null) throw new PartidaNoEncontradaException();
        Jugador jugador = obtenerJugador(partida, idJugador);
        validarTirada(codigo, jugador);
        Integer anterior = jugador.getPosicionActual();
        ResultadoTirada tirada = partidas.tirarDados();
        int premio = movimiento.avanzar(jugador, tirada.sumarDados());
        Integer caida = jugador.getPosicionActual();
        Optional<EventoCasillero> evento = aplicarEvento(codigo, jugador);
        premio += premioPorEvento(evento, caida, jugador.getPosicionActual());
        partidas.actualizarJugador(jugador);
        Destino destino = resolverDestino(partida, jugador);
        avanzarTurnoSiCorresponde(codigo, jugador, destino.requiereDecision());
        Partida actualizada = partidas.obtenerPartida(codigo);
        Partida paraSaldos = actualizada == null ? partida : actualizada;
        return new ResultadoJugada(idJugador, anterior, jugador.getPosicionActual(), caida, tirada,
                evento.map(e -> new ResultadoJugada.EventoAplicado(e.getNombre(), e.getDescripcion(),
                        e.getTipoEfecto().name())).orElse(null), premio,
                paraSaldos.getJugadores().stream().map(SaldoJugador::desdeJugador).toList(), destino.alquiler());
    }
    private Jugador obtenerJugador(Partida partida, Long idJugador) throws JugadorNoEncontradoException {
        return partida.getJugadores().stream().filter(j -> Objects.equals(j.getId(), idJugador))
                .findFirst().orElseThrow(() -> new JugadorNoEncontradoException("No se encontró el jugador en esta partida"));
    }
    private void validarTirada(String codigo, Jugador jugador) {
        Jugador actual = partidas.obtenerJugadorActual(codigo);
        if (!ReglasParticipacion.estaActivo(jugador) || actual == null || !Objects.equals(actual.getId(), jugador.getId())) {
            throw new TiradaNoPermitidaException("No podés tirar los dados en este turno.");
        }
        if (quiebras.obtenerDeudaPendiente(codigo, jugador.getId()) != null) {
            throw new TiradaNoPermitidaException("Tenés una deuda pendiente. Debés resolverla antes de volver a tirar los dados.");
        }
    }
    private Optional<EventoCasillero> aplicarEvento(String codigo, Jugador jugador) {
        return eventos == null ? Optional.empty() : eventos.aplicarSiCorresponde(codigo, jugador);
    }
    private Destino resolverDestino(Partida partida, Jugador jugador) {
        if (!ReglasParticipacion.estaActivo(jugador) || Boolean.TRUE.equals(jugador.getPierdeTurno())) return new Destino(false, null);
        CasilleroPropiedad casillero = casilleros.buscarOCrearPropiedadEnPosicion(partida, jugador.getPosicionActual());
        if (casillero == null) return new Destino(false, null);
        Propiedad propiedad = casillero.getPropiedad();
        if (propiedades.estaDisponible(propiedad)) return new Destino(true, null);
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || Objects.equals(propietario.getId(), jugador.getId())) return new Destino(false, null);
        int alquiler = propiedades.calcularAlquiler(casillero, propiedad);
        ResultadoDeuda deuda = quiebras.procesarAlquiler(partida.getCodigoUnico(), propiedad, jugador, alquiler);
        return new Destino(deuda == ResultadoDeuda.PENDIENTE, new AlquilerJugada(jugador.getId(),
                jugador.getApodo(), propietario.getApodo(), propiedad.getNombre(), alquiler));
    }
    private void avanzarTurnoSiCorresponde(String codigo, Jugador jugador, boolean requiereDecision) {
        if (requiereDecision || !ReglasParticipacion.estaActivo(jugador)) return;
        if (quiebras.obtenerDeudaPendiente(codigo, jugador.getId()) != null) return;
        partidas.avanzarTurno(codigo);
    }
    private int premioPorEvento(Optional<EventoCasillero> evento, int caida, int destino) {
        if (evento.isEmpty()) return 0;
        EventoCasillero efecto = evento.get();
        if (efecto.getTipoEfecto() == com.tallerwebi.dominio.enums.TipoEfectoEvento.AVANZA_CASILLEROS) {
            return movimiento.calcularPremioPorAvance(caida, efecto.getCantidadCasilleros());
        }
        return destino < caida ? ServicioMovimientoImpl.PREMIO_INICIO : 0;
    }
    private record Destino(boolean requiereDecision, AlquilerJugada alquiler) { }
}
