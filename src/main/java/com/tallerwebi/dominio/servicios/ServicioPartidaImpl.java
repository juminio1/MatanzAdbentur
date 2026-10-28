package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.CantidadJugadoresInsuficienteException;
import com.tallerwebi.dominio.excepcion.CantidadMaximaJugadoresSuperadaException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.FichaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaIniciadaException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.PosicionFicha;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//  Coordino las operaciones de la partida.
@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida {

    private static final Integer POSICION_INICIAL = 0;
    private static final Integer DURACION_TURNO_SEGUNDOS = 30;

    private final RepositorioPartida repositorioPartida;

    private final ServicioInicioPartida servicioInicioPartida;
    private final ServicioTurno servicioTurno;
    private final ServicioEvento servicioEvento;

    private final PublicadorEventos<EventoTurnoCambiado> publicadorTurno;
    private final GestorTemporizadorPartida gestorTemporizador;
    private final PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono;

    @Autowired
    public ServicioPartidaImpl(RepositorioPartida repositorioPartida, ServicioInicioPartida servicioInicioPartida, GestorTemporizadorPartida gestorTemporizador, ServicioTurno servicioTurno, @Autowired(required = false) PublicadorEventos<EventoTurnoCambiado> publicadorTurno, PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono, ServicioEvento servicioEvento) {
        this.repositorioPartida = repositorioPartida;
        this.servicioInicioPartida = servicioInicioPartida;
        this.servicioTurno = servicioTurno;
        this.gestorTemporizador = gestorTemporizador;
        this.publicadorTurno = publicadorTurno;
        this.publicadorAbandono = publicadorAbandono;
        this.servicioEvento = servicioEvento;
    }

    @Override
    @Transactional(readOnly = true)
    public Partida obtenerPartida(String codigoUnico) {
        return repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoUnico);
    }

    @Override
    public void actualizarJugador(Jugador jugador) {
        repositorioPartida.actualizarJugador(jugador);
    }

    @Override
    public void incrementarRondas(Partida partida) {
        servicioTurno.incrementarRondas(partida);
    }

    @Override
    public void inicializarTurnos(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (!esPartidaConJugadoresInvalida(partida)) {
            servicioTurno.asignarOrdenInicialJugadores(partida);
            partida.getJugadores().forEach(jugador -> jugador.setEnBancarrota(false));
            partida.setIndiceTurnoActual(servicioTurno.obtenerIndiceTurnoActual(partida));
            repositorioPartida.guardarPartida(partida);
        }
    }

    @Override
    public Jugador obtenerJugadorActual(String codigoUnico) {
        Partida partida = obtenerPartida(codigoUnico);
        if (esPartidaConJugadoresInvalida(partida)) {
            return null;
        }

        if (partida.getJugadores().get(0).getOrden() == null) {
            inicializarTurnos(codigoUnico);
        }

        return servicioTurno.obtenerJugadorActual(partida);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSegundosRestantesTurno(String codigoUnico) {
        return gestorTemporizador.obtenerSegundosRestantes(codigoUnico);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) //Permite iniciar una una transcacción nueva cuando el obvservador solicite reanudar la partida 
    public void iniciarTemporizadorTurnoSiNoExiste(String codigoUnico) {

        Partida partida = obtenerPartida(codigoUnico);

        if (partida != null && !verificarSiHayEventoGlobalActivo(codigoUnico)) {

            Boolean iniciado = gestorTemporizador.programarTemporizadorSiNoExiste(codigoUnico, DURACION_TURNO_SEGUNDOS, () -> avanzarTurno(codigoUnico));

            if (Boolean.TRUE.equals(iniciado)) {
                notificarTurnoActual(partida, false);
            }
        }
    }

    @Override
    public Jugador obtenerJugadorPorId(Long idJugador) {
        return repositorioPartida.buscarJugadorPorId(idJugador);
    }

    @Override
    public void prepararTiempoTurno(String codigoUnico, Integer segundosPresentacion) {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida != null && !verificarSiHayEventoGlobalActivo(codigoUnico)) {
            notificarTurnoActual(partida, true, DURACION_TURNO_SEGUNDOS + segundosPresentacion);
        }
    }

    @Override
    public void avanzarTurno(String codigoUnico) {

        Partida partida = obtenerPartida(codigoUnico);

        if (!esPartidaConJugadoresInvalida(partida) && !verificarSiHayEventoGlobalActivo(codigoUnico)) {

            DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigoUnico));

            Integer indiceActual = servicioTurno.obtenerIndiceTurnoActual(partida);

            Integer rondaAnterior = partida.getRondaActual();

            incrementarRondas(partida);

            Boolean cambioRonda = !rondaAnterior.equals(partida.getRondaActual());

            if (cambioRonda) {
                verificarEventoGlobalAlCambiarRonda(partida);
            }

            Jugador candidato = servicioTurno.resolverSiguienteJugador(partida, indiceActual);

            if (candidato != null) {
                candidato.setTiroEnTurno(false);
                partida.setIndiceTurnoActual(candidato.getOrden());
            }

            repositorioPartida.guardarPartida(partida);

            if (candidato != null && !verificarSiHayEventoGlobalActivo(codigoUnico)) {
                notificarTurnoActual(partida, true);
            }
        }
    }

    @Override
    public void iniciarPartida(String codigo, Long idUsuario) throws PartidaNoEncontradaException, CantidadJugadoresInsuficienteException, CantidadMaximaJugadoresSuperadaException, PartidaIniciadaException, SalaNoEncontradaException, FichaNoDisponibleException {
        Partida partida = servicioInicioPartida.iniciarPartida(codigo, idUsuario);
        DespuesDeConfirmar.ejecutar(() -> gestorTemporizador.cancelarTemporizador(codigo));
        notificarTurnoActual(partida, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosicionFicha> obtenerPosicionesFichas(String codigo) {
        Partida partida = obtenerPartida(codigo);
        if (esPartidaConJugadoresInvalida(partida)) {
            return List.of();
        }
        return partida.getJugadores().stream().map(this::mapearAJugadorPosicionDto).toList();
    }

    @Override
    public void abandonarPartida(String codigoUnico, Long idJugador) throws PartidaNoEncontradaException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        Partida partida = obtenerPartida(codigoUnico);
        if (partida == null) {
            throw new PartidaNoEncontradaException();
        }

        Jugador jugador = obtenerJugadorPorId(idJugador);
        if (jugador == null) {
            throw new IllegalArgumentException("El jugador especificado no existe.");
        }

        jugador.setAbandono(true);
        actualizarJugador(jugador);

        // Disparamos el evento con la seguridad de que todo existe
        publicadorAbandono.notificar(new EventoJugadorAbandonado(partida, jugador));
    }

    @Override
    public Boolean verificarSiHayEventoGlobalActivo(String codigoUnico) {
        return Boolean.TRUE.equals(this.servicioEvento.verificarSiHayEventoGlobalActivo(codigoUnico));
    }

    private PosicionFicha mapearAJugadorPosicionDto(Jugador jugador) {
        String nombreFicha = obtenerNombreFichaSegura(jugador);
        Integer posicion =
            jugador.getPosicionActual() != null ? jugador.getPosicionActual() : POSICION_INICIAL;
        return new PosicionFicha(jugador.getId(), jugador.getApodo(), nombreFicha, posicion);
    }

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador) {
        notificarTurnoActual(partida, iniciarTemporizador, DURACION_TURNO_SEGUNDOS);
    }

    private void notificarTurnoActual(Partida partida, Boolean iniciarTemporizador, Integer segundosTotales) {
    
        Jugador jugadorActual = obtenerJugadorActual(partida.getCodigoUnico());

        if (jugadorActual != null) {

            DespuesDeConfirmar.ejecutar(() -> {

                if (iniciarTemporizador && partida.getCodigoUnico() != null) {

                    gestorTemporizador.programarTemporizador(partida.getCodigoUnico(), segundosTotales, () -> avanzarTurno(partida.getCodigoUnico()));
                }

                if (publicadorTurno != null) {

                    EventoTurnoCambiado evento = new EventoTurnoCambiado(partida.getCodigoUnico(), jugadorActual.getId(), jugadorActual.getApodo(), jugadorActual.getOrden(), obtenerSegundosRestantesTurno(partida.getCodigoUnico()), partida.getRondaActual(), partida.getTurnosFinalizadosEnRonda());

                    publicarTurnoCambiado(evento);
                }
            });
        }
    }
    
    private void publicarTurnoCambiado(EventoTurnoCambiado evento) {
        try {
            publicadorTurno.notificar(evento);

        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException exception) {

            throw new IllegalStateException("Error al notificar el cambio de turno", exception);
        }
    }

    private Boolean esPartidaConJugadoresInvalida(Partida partida) {
        return partida == null || esListaDeJugadoresVacia(partida.getJugadores());
    }

    private Boolean esListaDeJugadoresVacia(List<Jugador> jugadores) {
        return jugadores == null || jugadores.isEmpty();
    }

    private String obtenerNombreFichaSegura(Jugador jugador) {
        return jugador.getFicha() == null ? null : jugador.getFicha().name();
    }
   
    private void verificarEventoGlobalAlCambiarRonda(Partida partida) {
        try {
            servicioEvento.verificarActivacionEventoGlobal(partida);

        } catch (PartidaNoEncontradaException | EventoGlobalInvalidoException | JugadorNoEncontradoException | EventoGlobalActivoException | EventoInexistenteException exception) {
            throw new IllegalStateException("Error al verificar el evento global de la partida", exception);
        }
    }
}
