package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioEvento")
public class ServicioEventoImpl implements ServicioEvento {

    private final RepositorioEvento repositorioEvento;
    private final Random random;
    private static final Integer RONDAS_ENTRE_EVENTOS = 5;
    private final ServicioDado servicioDado;

    private final Map<String, Map<Long, Integer>> tiradasPorPartida = new ConcurrentHashMap<>();
    private final Map<String, EventoGlobal> eventosActivosPorPartida = new ConcurrentHashMap<>();

    @Autowired
    public ServicioEventoImpl(RepositorioEvento repositorioEvento, ServicioDado servicioDado) {
        this.repositorioEvento = repositorioEvento;
        this.servicioDado = servicioDado;
        this.random = new Random();
    }

    @Override
    public Evento obtenerEventoAleatorio() {
        return repositorioEvento.obtenerEventoAleatorio();
    }

    @Override
    public Evento obtenerEventosGlobalesAleatorio() {
        return this.repositorioEvento.obtenerEventoGlobalAleatorio();
    }

    @Override
    public void aplicarEvento(Jugador jugador, Evento evento) {
        if (this.esEventoInvalido(evento)) {
            return;
        }

        switch (evento.getTipoEfecto()) {
            case GANAR_PLATA:
                jugador.setDinero(jugador.getDinero() + evento.getValor());
                break;
            case PERDER_PLATA:
                jugador.setDinero(jugador.getDinero() - evento.getValor());
                break;
            case PERDER_TURNO:
                jugador.setPierdeTurno(true);
                break;
            case MOVER_CASILLEROS:
                this.moverCasilleros(jugador, evento);
                break;
            case IR_A_CASILLERO:
            case VOLVER_INCICO:
                jugador.setPosicionActual(evento.getValor());
                break;
        }
    }

    @Override
    public void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal)
        throws EventoGlobalInvalidoException, PartidaNoEncontradaException, JugadorNoEncontradoException, EventoGlobalActivoException {
        if (esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new EventoGlobalInvalidoException("El evento global es inválido");
        }
        if (partida == null || partida.getCodigoUnico() == null) {
            throw new PartidaNoEncontradaException();
        }

        if (requiereTiradaDeDados(eventoGlobal)) {
            this.prepararTiradasEventoGlobal(partida, eventoGlobal);
        }

        this.eventosDeSeleccionDeJugadorAleatorio(partida, eventoGlobal);
    }

    @Override
    public void verificarActivacionEventoGlobal(Partida partida) {
        if (this.activarEventoGlobalPorRondas(partida.getRondaActual())) {
        }
    }

    @Override
    public ResultadoTiradaDTO realizarTiradaEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException, JugadorNoEncontradoException {
        
        this.validarDatosEventoGlobal(partida, jugador, eventoGlobal);

        this.validarJugadorEnPartida(partida, jugador);

        this.validarEventoActivo(partida, eventoGlobal);

        Map<Long, Integer> tiradas = obtenerTiradasDePartida(partida);

        this.validarQueNoHayaTirado(tiradas, jugador);

        ResultadoTiradaDTO resultado = servicioDado.tirarDados();

        this.registrarTiradaEventoGlobal(tiradas, jugador, resultado.sumarDados());

        return resultado;
    }

    private void validarDatosEventoGlobal(Partida partida, Jugador jugador, EventoGlobal eventoGlobal) throws DatosEnventoInvalidoExceptions {
        if (partida == null || jugador == null || esEventoInvalido(eventoGlobal) || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new DatosEnventoInvalidoExceptions("Datos del evento inválidos");
        }

        if (partida.getCodigoUnico() == null || partida.getJugadores() == null || jugador.getId() == null) {
            throw new DatosEnventoInvalidoExceptions("La partida o el jugador son inválidos");
        }
    }

    private void validarJugadorEnPartida(Partida partida, Jugador jugador)
        throws DatosEnventoInvalidoExceptions, JugadorNoEncontradoException {
        Boolean perteneceAPartida = partida.getJugadores().stream().anyMatch(participante -> participante.getId().equals(jugador.getId()));

        if (!perteneceAPartida) {
            throw new JugadorNoEncontradoException("El jugador no pertenece a la partida");
        }
    }

    private void validarEventoActivo(Partida partida, EventoGlobal eventoGlobal)
        throws DatosEnventoInvalidoExceptions {
        EventoGlobal eventoActivo = eventosActivosPorPartida.get(partida.getCodigoUnico());

        if (eventoActivo == null || eventoActivo.getId() == null || !eventoActivo.getId().equals(eventoGlobal.getId())) {
            throw new DatosEnventoInvalidoExceptions("El evento global no está activo");
        }
    }

    private Map<Long, Integer> obtenerTiradasDePartida(Partida partida)
        throws DatosEnventoInvalidoExceptions {
        Map<Long, Integer> tiradas = tiradasPorPartida.get(partida.getCodigoUnico());

        if (tiradas == null) {
            throw new DatosEnventoInvalidoExceptions("No hay tiradas para esta partida");
        }

        return tiradas;
    }

    private void validarQueNoHayaTirado(Map<Long, Integer> tiradas, Jugador jugador)
        throws TiradaDadoInvalidaException {
        if (tiradas.get(jugador.getId()) != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    private void registrarTiradaEventoGlobal(Map<Long, Integer> tiradas, Jugador jugador, Integer sumaDados) throws TiradaDadoInvalidaException {
        if (sumaDados == null || sumaDados < 2 || sumaDados > 12) {
            throw new TiradaDadoInvalidaException("La tirada de dados es inválida");
        }

        Integer tiradaAnterior = tiradas.putIfAbsent(jugador.getId(), sumaDados);

        if (tiradaAnterior != null) {
            throw new TiradaDadoInvalidaException("El jugador ya realizó su tirada");
        }
    }

    private void moverCasilleros(Jugador jugador, Evento evento) {
        int nuevaPosicion = jugador.getPosicionActual() + evento.getValor();
        if (nuevaPosicion < 0) {
            nuevaPosicion = 30 + nuevaPosicion;
        }
        jugador.setPosicionActual(nuevaPosicion);
    }

    private void prepararTiradasEventoGlobal(Partida partida, EventoGlobal eventoGlobal) throws EventoGlobalActivoException {
        String codigoPartida = partida.getCodigoUnico();

        EventoGlobal eventoAnterior = eventosActivosPorPartida.putIfAbsent(codigoPartida, eventoGlobal);

        if (eventoAnterior != null) {
            throw new EventoGlobalActivoException("Ya existe un evento global activo");
        }

        tiradasPorPartida.put(codigoPartida, new ConcurrentHashMap<>());
    }

    private Boolean requiereTiradaDeDados(EventoGlobal eventoGlobal) {
        Boolean inundacion = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.INUNDACION);

        Boolean piquete = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.PIQUETE);

        Boolean afip = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.TRAMITES_AFIP);

        return inundacion || piquete || afip;
    }

    //Eventos con jugador aleatorio

    private void eventosDeSeleccionDeJugadorAleatorio(Partida partida, EventoGlobal eventoGlobal)
        throws JugadorNoEncontradoException {
        List<Jugador> jugadores = partida.getJugadores();

        Boolean elKevin = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.EL_KEVIN);
        Boolean fiat600 = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.FIAT_600);
        Boolean sarmiento = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.ACCIDENTE_SARMIENTO);

        if (elKevin || fiat600 || sarmiento) {
            this.seleccionarJugadorYAplicarEvento(jugadores, eventoGlobal);
        }
    }

    private void seleccionarJugadorYAplicarEvento(List<Jugador> jugadores, EventoGlobal eventoGlobal) throws JugadorNoEncontradoException {

        if (jugadores == null || jugadores.isEmpty()) {
            throw new JugadorNoEncontradoException("No hay jugadores en la partida");
        }

        Integer indiceAleatorio = random.nextInt(jugadores.size());

        Jugador jugadorElegido = jugadores.get(indiceAleatorio);

        this.aplicarEvento(jugadorElegido, eventoGlobal);
    }

    private Boolean esEventoInvalido(Evento evento) {
        return evento == null || evento.getTipoEfecto() == null;
    }

    private Boolean activarEventoGlobalPorRondas(Integer rondaActual) {
        return rondaActual % RONDAS_ENTRE_EVENTOS == 0;
    }
}
/*
intentarResolverEventoGlobal(partida);
obtenerJugadoresConResultadoElegido(...);
prepararDesempate(...);
aplicarConsecuenciaEventoGlobal(...);
finalizarEventoGlobal(...);
*/