package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//Guarda la trivia en juego de cada partida y coordina sus rondas.
// Valida cada acción antes de delegar las reglas del torneo en ServicioTriviaImpl
// y los premios en ServicioPremioTriviaImpl. Las trivias se guardan en memoria, como la ruleta.

@Service
public class GestorTriviaPartida {

    // Debe coincidir con el límite de respuesta de ServicioTriviaImpl.
    public static final int SEGUNDOS_POR_PREGUNTA = 30;
    public static final String PREMIO_PLATA = "PLATA";
    public static final String PREMIO_ROBAR = "ROBAR";

    // Margen para que el cierre programado ocurra después del vencimiento.
    private static final long MARGEN_CIERRE_MILISEGUNDOS = 250;
    // Registra fallos de avisos o premios automáticos sin interrumpir los pasos restantes del torneo.
    private static final Logger LOG = LoggerFactory.getLogger(GestorTriviaPartida.class);

    private final ServicioTriviaImpl servicioTrivia;
    private final ServicioPremioTriviaImpl servicioPremio;
    private final ServicioPartida servicioPartida;
    private final ServicioCasillero servicioCasillero;
    private final PublicadorEventos<EventoTriviaActualizada> publicador;
    private final ScheduledExecutorService planificador;
    // Agrupa la entrega y el guardado del premio; la partida se libera después de confirmar la transacción.
    private final TransactionTemplate transaccion;
    // Controla el tiempo de las preguntas y permite adelantarlo en los tests sin esperar.
    private final Clock reloj;
    private final Map<String, Trivia> trivias = new HashMap<>();
    // Permite consultar el bloqueo sin esperar al gestor, evitando bloqueos cruzados con la base de datos.
    private final Set<String> partidasBloqueadas = ConcurrentHashMap.newKeySet();
    private final Map<String, List<Jugador>> participantesAlInicioDeRonda = new HashMap<>();
    // Guarda las tareas de cierre para cancelarlas cuando termina la ronda o se cancela la trivia.
    private final Map<String, ScheduledFuture<?>> cierresProgramados = new HashMap<>();

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
        partidasBloqueadas.add(codigoPartida);
        try {
            servicioPartida.actualizarPausaPorDeuda(codigoPartida);
            Trivia trivia = servicioTrivia.iniciarTrivia(partida, dificultad);
            trivias.put(codigoPartida, trivia);
            if (trivia.estaFinalizada()) {
                publicar(codigoPartida, TipoAvisoTrivia.FINALIZADA, trivia, null);
            } else {
                comenzarRonda(codigoPartida, trivia);
            }
            return trivia;
        } catch (RuntimeException ex) {
            actualizarBloqueo(codigoPartida);
            if (!estaBloqueandoPartida(codigoPartida)) {
                servicioPartida.actualizarPausaPorDeuda(codigoPartida);
            }
            throw ex;
        }
    }

    public boolean estaBloqueandoPartida(String codigoPartida) {
        return codigoPartida != null && partidasBloqueadas.contains(codigoPartida);
    }

    //actuliza los participantes de la trivia
    @Transactional(propagation = Propagation.NOT_SUPPORTED)//significa q este metodo se ejecuta sin una transaccion.
    public synchronized void participantesCambiaron(String codigoPartida) {
        if (!estaBloqueandoPartida(codigoPartida)) return;
        Trivia trivia = trivias.get(codigoPartida);
        if (trivia == null) return;
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null || partida.getEstado() == EstadoPartida.FINALIZADA) {
            cancelarTrivia(codigoPartida, trivia);
            return;
        }
        int ronda = trivia.getRondaActual();
        PreguntaGenerada pregunta = trivia.getPreguntaActual();
        boolean estabaFinalizada = trivia.estaFinalizada();
        if (!servicioTrivia.actualizarParticipantes(trivia, partida.getJugadores())) return;
        if (estabaFinalizada) {
            cancelarTrivia(codigoPartida, trivia);
            return;
        }
        publicar(codigoPartida, TipoAvisoTrivia.PARTICIPANTES_ACTUALIZADOS, trivia, null);
        procesarCambios(codigoPartida, trivia, ronda, pregunta);
    }

    private void cancelarTrivia(String codigoPartida, Trivia trivia) {
        trivia.cancelarSinPremio();
        trivia.getParticipantes().clear();
        participantesAlInicioDeRonda.remove(codigoPartida);
        cancelarCierre(codigoPartida);
        publicar(codigoPartida, TipoAvisoTrivia.FINALIZADA, trivia, null);
    }

    private void cancelarCierre(String codigoPartida) {
        ScheduledFuture<?> cierre = cierresProgramados.remove(codigoPartida);
        if (cierre != null) cierre.cancel(false);
    }

    private void actualizarBloqueo(String codigoPartida) {
        Trivia trivia = trivias.get(codigoPartida);
        boolean bloqueada = trivia != null && (!trivia.estaFinalizada()
                || (trivia.getGanador() != null && (estaBloqueandoPartida(codigoPartida)
                        || !trivia.estaPremioEntregado())));
        if (bloqueada) {
            partidasBloqueadas.add(codigoPartida);
        } else {
            partidasBloqueadas.remove(codigoPartida);
        }
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
        participantesCambiaron(codigoPartida);
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
        participantesCambiaron(codigoPartida);
        Trivia trivia = buscar(codigoPartida);
        validarEleccionDePremio(trivia, idJugador);
        if (!PREMIO_PLATA.equals(premio) && !PREMIO_ROBAR.equals(premio)) {
            throw new IllegalArgumentException("El premio elegido no es válido.");
        }
        entregarPremio(codigoPartida, trivia, premio, posicion);
        publicar(codigoPartida, TipoAvisoTrivia.PREMIO_ENTREGADO, trivia, null);
    }

    // Lo ejecuta el planificador cuando vence la pregunta, aunque nadie haya respondido.
    synchronized void cerrarPorTiempo(String codigoPartida, Trivia trivia, int ronda) {
        participantesCambiaron(codigoPartida);
        if (!Objects.equals(trivias.get(codigoPartida), trivia) || trivia.estaFinalizada() || trivia.getRondaActual() != ronda) {
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
        if (estaBloqueandoPartida(codigoPartida)) {
            throw new AccionTriviaInvalidaException("Ya hay una trivia en juego o un premio pendiente en esta partida.");
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
        participantesAlInicioDeRonda.put(codigoPartida, List.copyOf(trivia.getParticipantes()));
        publicar(codigoPartida, TipoAvisoTrivia.RONDA_NUEVA, trivia, null);
        int ronda = trivia.getRondaActual();
        long demora = Duration.between(reloj.instant(), vencimiento(trivia)).toMillis() + MARGEN_CIERRE_MILISEGUNDOS;
        ScheduledFuture<?> cierre = planificador.schedule(() -> cerrarPorTiempo(codigoPartida, trivia, ronda),
                Math.max(demora, 0), TimeUnit.MILLISECONDS);
        cierresProgramados.put(codigoPartida, cierre);
    }

    private void procesarCambios(String codigoPartida, Trivia trivia, int rondaAnterior,
                                 PreguntaGenerada preguntaAnterior) {
        boolean seCerroLaRonda = trivia.estaFinalizada() || trivia.getRondaActual() != rondaAnterior;
        if (!seCerroLaRonda) {
            return;
        }
        cancelarCierre(codigoPartida);
        List<Jugador> participantesIniciales = participantesAlInicioDeRonda.remove(codigoPartida);
        if (participantesIniciales == null) {
            return;
        }
        List<Jugador> eliminados = new ArrayList<>();
        for (Jugador jugador : participantesIniciales) {
            if (!trivia.getParticipantes().contains(jugador)) {
                eliminados.add(jugador);
            }
        }
        publicar(new EventoTriviaActualizada(codigoPartida, TipoAvisoTrivia.RONDA_CERRADA, trivia,
                preguntaAnterior.getRespuestaCorrecta(), rondaAnterior, eliminados));
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
                entregarPremio(codigoPartida, trivia, PREMIO_PLATA, null);
                publicar(codigoPartida, TipoAvisoTrivia.PREMIO_ENTREGADO, trivia, null);
            } catch (RuntimeException ex) {
                LOG.error("No se pudo entregar el premio de la trivia de la partida {}", codigoPartida, ex);
            }
        }
        publicar(codigoPartida, TipoAvisoTrivia.FINALIZADA, trivia, null);
    }

    // Recarga al ganador dentro de la transacción para que el premio se guarde sobre datos actuales.
    private void entregarPremio(String codigoPartida, Trivia trivia, String premio, Integer posicion) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigoPartida);
            if (partida == null) {
                throw new AccionTriviaInvalidaException("La partida ya terminó.");
            }
            servicioPartida.bloquearPartida(partida);
            if (partida.getEstado() != EstadoPartida.EN_CURSO) {
                throw new AccionTriviaInvalidaException("La partida ya terminó.");
            }
            Long idGanador = trivia.getGanador().getId();
            Jugador ganador = null;
            for (Jugador jugador : partida.getJugadores()) {
                if (Objects.equals(jugador.getId(), idGanador)
                        && !Boolean.TRUE.equals(jugador.getAbandono())
                        && !Boolean.TRUE.equals(jugador.getEnBancarrota())) {
                    ganador = jugador;
                    break;
                }
            }
            if (ganador == null) {
                throw new AccionTriviaInvalidaException("El ganador ya no está en la partida.");
            }
            trivia.declararGanador(ganador);
            if (PREMIO_PLATA.equals(premio)) {
                servicioPremio.entregarPremioEnDinero(trivia);
            } else {
                servicioPremio.entregarPremioRobandoPropiedad(trivia, partida, idPropiedad(partida, posicion));
            }
            servicioPartida.actualizarJugador(ganador);
        });
    }

    private void publicar(String codigoPartida, TipoAvisoTrivia tipo, Trivia trivia, Integer opcionCorrecta) {
        publicar(new EventoTriviaActualizada(codigoPartida, tipo, trivia, opcionCorrecta));
    }

    private void publicar(EventoTriviaActualizada evento) {
        boolean estabaBloqueada = estaBloqueandoPartida(evento.codigoPartida());
        if (evento.tipo() == TipoAvisoTrivia.PREMIO_ENTREGADO) {
            partidasBloqueadas.remove(evento.codigoPartida());
        } else if (evento.tipo() != TipoAvisoTrivia.RONDA_CERRADA) {
            actualizarBloqueo(evento.codigoPartida());
        }
        try {
            publicador.notificar(evento);
        } catch (RuntimeException ex) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("No se pudo avisar el cambio de la trivia de la partida {}", evento.codigoPartida(), ex);
            }
        }
        if (estabaBloqueada && !estaBloqueandoPartida(evento.codigoPartida())) {
            servicioPartida.actualizarPausaPorDeuda(evento.codigoPartida());
        }
    }
}
