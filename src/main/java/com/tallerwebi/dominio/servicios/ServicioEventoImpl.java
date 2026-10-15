package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
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
    private static final Integer RONDAS_ENTRE_EVENTOS = 2;
    private final ServicioDado servicioDado;
    private final Map<String, Map<Long, Integer>> tiradasPorPartida = new ConcurrentHashMap<>();

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
        if (this.esEventoInvalido(evento) == null) {
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
                int nuevaPosicion = jugador.getPosicionActual() + evento.getValor();
                if (nuevaPosicion < 0) {
                    nuevaPosicion = 30 + nuevaPosicion;
                }
                jugador.setPosicionActual(nuevaPosicion);
                break;
            case IR_A_CASILLERO:
                jugador.setPosicionActual(evento.getValor());
                break;
            case VOLVER_INCICO:
                jugador.setPosicionActual(evento.getValor());
                break;
        }
    }

    @Override
    public void iniciarEventoGlobal(Partida partida, EventoGlobal eventoGlobal)
        throws EventoGlobalInvalidoException, PartidaNoEncontradaException {
        if (esEventoInvalido(eventoGlobal) == null || eventoGlobal.getTipoEventoGlobal() == null) {
            throw new EventoGlobalInvalidoException("El evento global es inválido");
        }
        if (partida == null || partida.getCodigoUnico() == null) {
            throw new PartidaNoEncontradaException();
        }

        Boolean inundacion = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.INUNDACION);
        Boolean piquete = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.PIQUETE);
        Boolean afip = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.TRAMITES_AFIP);

        if (inundacion || piquete || afip) {
            tiradasPorPartida.putIfAbsent(partida.getCodigoUnico(), new ConcurrentHashMap<>()); //Esto prepara un espacio donde se registraran las tiradas de los jugadores de esa partida
        }

        eventosDeSeleccionDeJugadorAleatorio(partida, eventoGlobal);
    }

    @Override
    public void verificarActivacionEventoGlobal(Partida partida) {
        if (this.activarEventoGlobalPorRondas(partida.getRondaActual())) {
        }
    }

    private void validarJugadorEnPartida(Partida partida, Jugador jugador) throws DatosEnventoInvalidoExceptions, JugadorNoEncontradoException {

        Boolean perteneceAPartida = partida.getJugadores().stream().anyMatch(participante -> participante.getId().equals(jugador.getId()));

        if (!perteneceAPartida) {
            throw new JugadorNoEncontradoException("El jugador no se encontro en la partida");
        }
    }

    public ResultadoTiradaDTO realizarTiradaEventoGlobal(
        Partida partida,
        Jugador jugador,
        EventoGlobal eventoGlobal
    ) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException {
        ResultadoTiradaDTO resultado = servicioDado.tirarDados();

        int sumaDados = resultado.getDado1() + resultado.getDado2();

        registrarTiradaEventoGlobal(partida, jugador, sumaDados, eventoGlobal);

        return resultado;
    }

    private Boolean activarEventoGlobalPorRondas(Integer rondaActual) {
        return rondaActual % RONDAS_ENTRE_EVENTOS == 0;
    }

    private void registrarTiradaEventoGlobal(
        Partida partida,
        Jugador jugador,
        Integer sumaDados,
        EventoGlobal eventoGlobal
    ) throws DatosEnventoInvalidoExceptions, TiradaDadoInvalidaException {
        if (partida == null || jugador == null || eventoGlobal == null) {
            throw new DatosEnventoInvalidoExceptions("Datos del evento inválidos");
        }

        if (sumaDados == null || sumaDados < 2 || sumaDados > 12) {
            throw new TiradaDadoInvalidaException("La tirada de dados es inválida");
        }

        // Map<Long, Integer> tiradas = partida.getTiradasEventoGlobal();

        //tiradas.put(jugador.getId(), sumaDados);
    }

    private void eventosDeSeleccionDeJugadorAleatorio(Partida partida, EventoGlobal eventoGlobal) {
        List<Jugador> jugadores = partida.getJugadores();

        Boolean elKevin = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.EL_KEVIN);
        Boolean fiat600 = eventoGlobal.getTipoEventoGlobal().equals(TipoEventoGlobal.FIAT_600);
        Boolean sarmiento = eventoGlobal
            .getTipoEventoGlobal()
            .equals(TipoEventoGlobal.ACCIDENTE_SARMIENTO);

        if (elKevin || fiat600 || sarmiento) {
            if (jugadores != null && !jugadores.isEmpty()) {
                Integer indiceAleatorio = random.nextInt(jugadores.size());
                Jugador jugadorElegido = jugadores.get(indiceAleatorio);
                this.aplicarEvento(jugadorElegido, eventoGlobal);
            }
        }
    }

    private Boolean esEventoInvalido(Evento evento) {
        return evento == null || evento.getTipoEfecto() == null;
    }
}
