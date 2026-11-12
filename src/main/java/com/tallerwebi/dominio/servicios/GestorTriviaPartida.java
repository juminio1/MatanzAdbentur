package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;
import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.dominio.excepcion.AccionTriviaInvalidaException;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import com.tallerwebi.dominio.excepcion.TriviaNoEncontradaException;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Guarda la trivia en juego de cada partida y coordina sus rondas.
 * Valida cada acción antes de delegar las reglas del torneo en ServicioTriviaImpl
 * y los premios en ServicioPremioTriviaImpl. Las trivias se guardan en memoria, como la ruleta.
 */
@Service
public class GestorTriviaPartida {

    // Debe coincidir con el límite de respuesta de ServicioTriviaImpl.
    public static final int SEGUNDOS_POR_PREGUNTA = 30;
    public static final String PREMIO_PLATA = "PLATA";
    public static final String PREMIO_ROBAR = "ROBAR";

    // Margen para que el cierre programado ocurra después del vencimiento.
    private static final long MARGEN_CIERRE_MILISEGUNDOS = 250;
    private static final Logger LOG = LoggerFactory.getLogger(GestorTriviaPartida.class);

    private final ServicioTriviaImpl servicioTrivia;
    private final ServicioPremioTriviaImpl servicioPremio;
    private final ServicioPartida servicioPartida;
    private final ServicioCasillero servicioCasillero;
    private final PublicadorEventos<EventoTriviaActualizada> publicador;
    private final ScheduledExecutorService planificador;
    private final TransactionTemplate transaccion;
    private final Clock reloj;
    private final Map<String, Trivia> trivias = new HashMap<>();

    @Autowired
    public GestorTriviaPartida(ServicioTriviaImpl servicioTrivia, ServicioPremioTriviaImpl servicioPremio,
                               ServicioPartida servicioPartida, ServicioCasillero servicioCasillero,
                               PublicadorEventos<EventoTriviaActualizada> publicador,
                               ScheduledExecutorService planificador,
                               PlatformTransactionManager administradorTransacciones) {
        this(servicioTrivia, servicioPremio, servicioPartida, servicioCasillero, publicador, planificador,
                administradorTransacciones, Clock.systemUTC());
    }

    GestorTriviaPartida(ServicioTriviaImpl servicioTrivia, ServicioPremioTriviaImpl servicioPremio,
                        ServicioPartida servicioPartida, ServicioCasillero servicioCasillero,
                        PublicadorEventos<EventoTriviaActualizada> publicador,
                        ScheduledExecutorService planificador,
                        PlatformTransactionManager administradorTransacciones, Clock reloj) {
        this.servicioTrivia = servicioTrivia;
        this.servicioPremio = servicioPremio;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.publicador = publicador;
        this.planificador = planificador;
        this.transaccion = new TransactionTemplate(administradorTransacciones);
        this.reloj = reloj;
    }

    /** Momento en que vence la pregunta actual, en milisegundos, o null si no hay ronda en juego. */
    public static Long venceEnMilisegundos(Trivia trivia) {
        if (trivia == null || trivia.getInicioRonda() == null) {
            return null;
        }
        return vencimiento(trivia).toEpochMilli();
    }

    private static Instant vencimiento(Trivia trivia) {
        return trivia.getInicioRonda().plusSeconds(SEGUNDOS_POR_PREGUNTA);
    }

    // Lo llama la integración con la ruleta cuando se sortea la dificultad.
    public synchronized Trivia iniciar(String codigoPartida, DificultadRuleta dificultad) {
        validarInicio(codigoPartida, dificultad);
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) {
            throw new AccionTriviaInvalidaException("La partida no existe o ya terminó.");
        }
        Trivia trivia = servicioTrivia.iniciarTrivia(partida, dificultad);
        trivias.put(codigoPartida, trivia);
        if (trivia.estaFinalizada()) {
            publicar(codigoPartida, TipoAvisoTrivia.FINALIZADA, trivia, null);
        } else {
            comenzarRonda(codigoPartida, trivia);
        }
        return trivia;
    }

    /** Lee la trivia de la partida (en juego o terminada) sin que cambie mientras se arma la respuesta. */
    public synchronized <T> T consultarEstado(String codigoPartida, Function<Trivia, T> lectura) {
        return lectura.apply(buscar(codigoPartida));
    }

    /** Lee la pregunta en juego. Falla si la trivia ya terminó. */
    public synchronized <T> T consultarPregunta(String codigoPartida, Function<Trivia, T> lectura) {
        return lectura.apply(enJuego(codigoPartida));
    }

    /** Registra la respuesta y devuelve si fue correcta. La opción correcta se avisa al cerrar la ronda. */
    public synchronized boolean responder(String codigoPartida, Long idJugador, Integer ronda, Integer opcion) {
        Trivia trivia = enJuego(codigoPartida);
        Jugador jugador = participante(trivia, idJugador);
        if (!Objects.equals(ronda, trivia.getRondaActual())) {
            throw new AccionTriviaInvalidaException("Esa pregunta ya no está en juego.");
        }
        if (trivia.yaRespondioEnRonda(jugador)) {
            throw new AccionTriviaInvalidaException("Ya respondiste esta pregunta.");
        }
        PreguntaGenerada pregunta = trivia.getPreguntaActual();
        validarOpcion(pregunta, opcion);
        int rondaRespondida = trivia.getRondaActual();
        if (!reloj.instant().isBefore(vencimiento(trivia))) {
            servicioTrivia.cerrarRondaPorTiempo(trivia);
            procesarCambios(codigoPartida, trivia, rondaRespondida, pregunta);
            throw new AccionTriviaInvalidaException("Se terminó el tiempo para responder.");
        }
        servicioTrivia.responderPregunta(trivia, jugador, opcion);
        procesarCambios(codigoPartida, trivia, rondaRespondida, pregunta);
        return opcion.equals(pregunta.getRespuestaCorrecta());
    }

    /** El ganador de una trivia difícil elige "PLATA" o "ROBAR" (con la posición de la propiedad). */
    public synchronized void elegirPremio(String codigoPartida, Long idJugador, String premio, Integer posicion) {
        Trivia trivia = buscar(codigoPartida);
        validarEleccionDePremio(trivia, idJugador);
        if (PREMIO_PLATA.equals(premio)) {
            entregarPremio(codigoPartida, trivia, partida -> servicioPremio.entregarPremioEnDinero(trivia));
        } else if (PREMIO_ROBAR.equals(premio)) {
            entregarPremio(codigoPartida, trivia, partida ->
                    servicioPremio.entregarPremioRobandoPropiedad(trivia, partida, idPropiedad(partida, posicion)));
        } else {
            throw new IllegalArgumentException("El premio elegido no es válido.");
        }
        publicar(codigoPartida, TipoAvisoTrivia.PREMIO_ENTREGADO, trivia, null);
    }

    // Lo ejecuta el planificador cuando vence la pregunta, aunque nadie haya respondido.
    synchronized void cerrarPorTiempo(String codigoPartida, Trivia trivia, int ronda) {
        if (trivias.get(codigoPartida) != trivia || trivia.estaFinalizada() || trivia.getRondaActual() != ronda) {
            return;
        }
        PreguntaGenerada pregunta = trivia.getPreguntaActual();
        servicioTrivia.cerrarRondaPorTiempo(trivia);
        procesarCambios(codigoPartida, trivia, ronda, pregunta);
    }

    private void validarInicio(String codigoPartida, DificultadRuleta dificultad) {
        if (codigoPartida == null || codigoPartida.isBlank() || dificultad == null) {
            throw new IllegalArgumentException("Los datos para iniciar la trivia son obligatorios.");
        }
        Trivia actual = trivias.get(codigoPartida);
        if (actual != null && !actual.estaFinalizada()) {
            throw new AccionTriviaInvalidaException("Ya hay una trivia en juego en esta partida.");
        }
    }

    private Trivia buscar(String codigoPartida) {
        Trivia trivia = trivias.get(codigoPartida);
        if (trivia == null) {
            throw new TriviaNoEncontradaException("No hay una trivia en esta partida.");
        }
        return trivia;
    }

    private Trivia enJuego(String codigoPartida) {
        Trivia trivia = buscar(codigoPartida);
        if (trivia.estaFinalizada()) {
            throw new AccionTriviaInvalidaException("La trivia ya terminó.");
        }
        return trivia;
    }

    // Usa la misma instancia guardada en la trivia: así la reconoce entre una petición y otra.
    private Jugador participante(Trivia trivia, Long idJugador) {
        return trivia.getParticipantes().stream()
                .filter(jugador -> idJugador != null && idJugador.equals(jugador.getId()))
                .findFirst()
                .orElseThrow(() -> new ParticipanteTriviaNoHabilitadoException(
                        "No estás participando de esta ronda de la trivia."));
    }

    private void validarOpcion(PreguntaGenerada pregunta, Integer opcion) {
        int cantidad = pregunta.getOpciones() == null ? 0 : pregunta.getOpciones().size();
        if (opcion == null || opcion < 0 || opcion >= cantidad) {
            throw new IllegalArgumentException("La opción elegida no es válida.");
        }
    }

    private void validarEleccionDePremio(Trivia trivia, Long idJugador) {
        validarQueHayaGanador(trivia);
        if (!Objects.equals(trivia.getGanador().getId(), idJugador)) {
            throw new ParticipanteTriviaNoHabilitadoException("Solo el ganador puede elegir el premio.");
        }
        if (trivia.estaPremioEntregado()) {
            throw new AccionTriviaInvalidaException("El premio ya fue entregado.");
        }
        if (trivia.getDificultad() != DificultadRuleta.DIFICIL) {
            throw new AccionTriviaInvalidaException("Solo el ganador de una trivia difícil elige su premio.");
        }
    }

    private void validarQueHayaGanador(Trivia trivia) {
        if (!trivia.estaFinalizada()) {
            throw new AccionTriviaInvalidaException("La trivia todavía no terminó.");
        }
        if (trivia.getGanador() == null) {
            throw new AccionTriviaInvalidaException("La trivia terminó sin ganador.");
        }
    }

    private Long idPropiedad(Partida partida, Integer posicion) {
        if (posicion == null) {
            throw new IllegalArgumentException("Elegí la propiedad que querés robar.");
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null || casillero.getPropiedad() == null) {
            throw new IllegalArgumentException("La propiedad elegida no está disponible.");
        }
        return casillero.getPropiedad().getId();
    }

    private void comenzarRonda(String codigoPartida, Trivia trivia) {
        publicar(codigoPartida, TipoAvisoTrivia.RONDA_NUEVA, trivia, null);
        int ronda = trivia.getRondaActual();
        long demora = Duration.between(reloj.instant(), vencimiento(trivia)).toMillis() + MARGEN_CIERRE_MILISEGUNDOS;
        planificador.schedule(() -> cerrarPorTiempo(codigoPartida, trivia, ronda),
                Math.max(demora, 0), TimeUnit.MILLISECONDS);
    }

    private void procesarCambios(String codigoPartida, Trivia trivia, int rondaAnterior,
                                 PreguntaGenerada preguntaAnterior) {
        boolean seCerroLaRonda = trivia.estaFinalizada() || trivia.getRondaActual() != rondaAnterior;
        if (!seCerroLaRonda) {
            return;
        }
        publicar(codigoPartida, TipoAvisoTrivia.RONDA_CERRADA, trivia, preguntaAnterior.getRespuestaCorrecta());
        if (trivia.estaFinalizada()) {
            finalizar(codigoPartida, trivia);
        } else {
            comenzarRonda(codigoPartida, trivia);
        }
    }

    // En las trivias fácil y media el premio es dinero y se entrega solo; en la difícil lo elige el ganador.
    private void finalizar(String codigoPartida, Trivia trivia) {
        if (trivia.getGanador() != null && trivia.getDificultad() != DificultadRuleta.DIFICIL) {
            try {
                entregarPremio(codigoPartida, trivia, partida -> servicioPremio.entregarPremioEnDinero(trivia));
            } catch (RuntimeException ex) {
                LOG.error("No se pudo entregar el premio de la trivia de la partida {}", codigoPartida, ex);
            }
        }
        publicar(codigoPartida, TipoAvisoTrivia.FINALIZADA, trivia, null);
    }

    // Recarga al ganador dentro de la transacción para que el premio se guarde sobre datos actuales.
    private void entregarPremio(String codigoPartida, Trivia trivia, Consumer<Partida> entrega) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigoPartida);
            if (partida == null) {
                throw new AccionTriviaInvalidaException("La partida ya terminó.");
            }
            Long idGanador = trivia.getGanador().getId();
            Jugador ganador = partida.getJugadores().stream()
                    .filter(jugador -> Objects.equals(jugador.getId(), idGanador))
                    .findFirst()
                    .orElseThrow(() -> new AccionTriviaInvalidaException("El ganador ya no está en la partida."));
            trivia.declararGanador(ganador);
            entrega.accept(partida);
            servicioPartida.actualizarJugador(ganador);
        });
    }

    private void publicar(String codigoPartida, TipoAvisoTrivia tipo, Trivia trivia, Integer opcionCorrecta) {
        try {
            publicador.notificar(new EventoTriviaActualizada(codigoPartida, tipo, trivia, opcionCorrecta));
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo avisar el cambio de la trivia de la partida {}", codigoPartida, ex);
        }
    }
}
