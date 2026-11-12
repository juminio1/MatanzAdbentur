package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoAvisoTrivia;
import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.excepcion.AccionTriviaInvalidaException;
import com.tallerwebi.dominio.excepcion.ParticipanteTriviaNoHabilitadoException;
import com.tallerwebi.dominio.excepcion.TriviaNoEncontradaException;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTriviaWebSocket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

public class GestorTriviaPartidaTest {

    private static final String CODIGO = "ABC123";
    private static final int CORRECTA = 1;
    private static final int INCORRECTA = 2;

    private RelojAjustable reloj;
    private ServicioPremioTriviaImpl servicioPremio;
    private ServicioPartida servicioPartida;
    private ServicioCasillero servicioCasillero;
    private ScheduledExecutorService planificador;
    private List<EventoTriviaActualizada> avisos;
    private PublicadorEventos<EventoTriviaActualizada> publicador;
    private GestorTriviaPartida gestor;
    private Partida partida;
    private Jugador ana;
    private Jugador luna;

    /** Reloj que los tests pueden adelantar. */
    private static final class RelojAjustable extends Clock {
        private Instant ahora = Instant.parse("2026-11-12T12:00:00Z");

        void adelantar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @BeforeEach
    public void init() {
        reloj = new RelojAjustable();
        ServicioPreguntasTrivia preguntas = mock(ServicioPreguntasTrivia.class);
        when(preguntas.obtenerParaCasillero(any(), any())).thenReturn(cincoPreguntas());
        ServicioTriviaImpl servicioTrivia = new ServicioTriviaImpl(new ServicioParticipacionImpl(), preguntas, reloj);
        servicioPremio = mock(ServicioPremioTriviaImpl.class);
        servicioPartida = mock(ServicioPartida.class);
        servicioCasillero = mock(ServicioCasillero.class);
        planificador = mock(ScheduledExecutorService.class);
        avisos = new ArrayList<>();
        Observador<EventoTriviaActualizada> registro = avisos::add;
        publicador = new PublicadorEventos<>(List.of(registro));
        gestor = new GestorTriviaPartida(servicioTrivia, servicioPremio, servicioPartida, servicioCasillero,
                publicador, planificador, mock(PlatformTransactionManager.class), reloj);

        ana = jugador(1L, "Ana");
        luna = jugador(2L, "Luna");
        partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(new ArrayList<>(List.of(ana, luna)));
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
    }

    private List<PreguntaTrivia> cincoPreguntas() {
        List<PreguntaTrivia> preguntas = new ArrayList<>();
        for (int numero = 1; numero <= 5; numero++) {
            preguntas.add(new PreguntaTrivia("¿Cuál es la cabecera de La Matanza? " + numero,
                    List.of("Ramos Mejía", "San Justo", "Morón", "Haedo"), CORRECTA, DificultadTrivia.SIMPLE));
        }
        return preguntas;
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        jugador.setDinero(1000);
        return jugador;
    }

    private List<TipoAvisoTrivia> tiposAvisados() {
        return avisos.stream().map(EventoTriviaActualizada::tipo).toList();
    }

    private List<JsonNode> registrarPremiosWebSocket() {
        List<JsonNode> premios = new ArrayList<>();
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        doAnswer(invocacion -> {
            Map<String, Object> mensaje = invocacion.getArgument(1);
            if (mensaje.get("tipo") == TipoAvisoTrivia.PREMIO_ENTREGADO) {
                premios.add(new ObjectMapper().valueToTree(mensaje));
            }
            return null;
        }).when(notificador).notificarTrivia(eq(CODIGO), any());
        publicador.suscribir(new ObservadorTriviaWebSocket(notificador));
        return premios;
    }

    private void simularEntregaExitosa() {
        doAnswer(invocacion -> {
            invocacion.<Trivia>getArgument(0).registrarPremioEntregado();
            return null;
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));
    }

    private void verificarPremioAutomatico(DificultadRuleta dificultad) {
        List<JsonNode> premios = registrarPremiosWebSocket();
        simularEntregaExitosa();
        List<Boolean> entregadoAlPublicar = new ArrayList<>();
        publicador.suscribir(evento -> {
            if (evento.tipo() == TipoAvisoTrivia.PREMIO_ENTREGADO) {
                entregadoAlPublicar.add(evento.trivia().estaPremioEntregado());
            }
        });
        gestor.iniciar(CODIGO, dificultad);
        Runnable cierre = cierreProgramado(1);

        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);
        reloj.adelantar(Duration.ofSeconds(31));
        cierre.run();
        cierre.run();

        assertEquals(List.of(TipoAvisoTrivia.RONDA_NUEVA, TipoAvisoTrivia.RONDA_CERRADA,
                TipoAvisoTrivia.PREMIO_ENTREGADO, TipoAvisoTrivia.FINALIZADA), tiposAvisados());
        assertEquals(List.of(true), entregadoAlPublicar);
        verify(servicioPremio, times(1)).entregarPremioEnDinero(any(Trivia.class));
        assertEquals(1, premios.size());
        assertTrue(premios.get(0).path("estado").path("finalizada").asBoolean());
        assertEquals(1L, premios.get(0).path("estado").path("idGanador").asLong());
        assertTrue(premios.get(0).path("estado").path("premioPendiente").isBoolean());
        assertFalse(premios.get(0).path("estado").path("premioPendiente").asBoolean());
    }

    @Test
    public void premioAutomaticoFacilSeNotificaUnaVezAntesDeFinalizada() {
        verificarPremioAutomatico(DificultadRuleta.FACIL);
    }

    @Test
    public void premioAutomaticoMedioSeNotificaUnaVezAntesDeFinalizada() {
        verificarPremioAutomatico(DificultadRuleta.MEDIO);
    }

    @Test
    public void premioElegidoSeNotificaUnaVezAunqueSeRepitaLaEleccion() {
        List<JsonNode> premios = registrarPremiosWebSocket();
        simularEntregaExitosa();
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        assertTrue(premios.isEmpty());

        gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null);
        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));

        assertEquals(1L, tiposAvisados().stream().filter(tipo -> tipo == TipoAvisoTrivia.PREMIO_ENTREGADO).count());
        assertEquals(1, premios.size());
        JsonNode estado = premios.get(0).path("estado");
        assertTrue(estado.path("finalizada").asBoolean());
        assertEquals(1L, estado.path("idGanador").asLong());
        assertEquals("Ana", estado.path("ganador").asText());
        assertTrue(estado.path("premioPendiente").isBoolean());
        assertFalse(estado.path("premioPendiente").asBoolean());
    }

    @Test
    public void entregaAutomaticaFallidaNoPublicaNiNotificaPremioEntregado() {
        List<JsonNode> premios = registrarPremiosWebSocket();
        doAnswer(invocacion -> {
            throw new IllegalStateException("Falla de entrega");
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));

        triviaGanadaPorAna(DificultadRuleta.FACIL);

        assertFalse(tiposAvisados().contains(TipoAvisoTrivia.PREMIO_ENTREGADO));
        assertTrue(premios.isEmpty());
        assertEquals(TipoAvisoTrivia.FINALIZADA, tiposAvisados().getLast());
    }

    @Test
    public void entregaElegidaFallidaNoPublicaNiNotificaPremioEntregado() {
        List<JsonNode> premios = registrarPremiosWebSocket();
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        doAnswer(invocacion -> {
            throw new IllegalStateException("Falla de entrega");
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));

        assertThrows(IllegalStateException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));

        assertFalse(tiposAvisados().contains(TipoAvisoTrivia.PREMIO_ENTREGADO));
        assertTrue(premios.isEmpty());
    }

    @Test
    public void torneoSinGanadorNoPublicaNiNotificaPremioEntregado() {
        List<JsonNode> premios = registrarPremiosWebSocket();
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 1L, 1, INCORRECTA);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);

        assertFalse(tiposAvisados().contains(TipoAvisoTrivia.PREMIO_ENTREGADO));
        assertTrue(premios.isEmpty());
        verify(servicioPremio, never()).entregarPremioEnDinero(any(Trivia.class));
        assertEquals(TipoAvisoTrivia.FINALIZADA, tiposAvisados().getLast());
    }

    @Test
    public void laFinalizacionWebSocketSeNotificaUnaSolaVezDespuesDeLaEntregaAutomatica() {
        List<JsonNode> finalizaciones = new ArrayList<>();
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        doAnswer(invocacion -> {
            Map<String, Object> mensaje = invocacion.getArgument(1);
            if (mensaje.get("tipo") == TipoAvisoTrivia.FINALIZADA) {
                finalizaciones.add(new ObjectMapper().valueToTree(mensaje));
            }
            return null;
        }).when(notificador).notificarTrivia(eq(CODIGO), any());
        publicador.suscribir(new ObservadorTriviaWebSocket(notificador));
        doAnswer(invocacion -> {
            invocacion.<Trivia>getArgument(0).registrarPremioEntregado();
            return null;
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        Runnable cierrePendiente = cierreProgramado(1);

        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);
        reloj.adelantar(Duration.ofSeconds(31));
        cierrePendiente.run();
        cierrePendiente.run();
        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.responder(CODIGO, 1L, 1, CORRECTA));

        assertEquals(1L, tiposAvisados().stream().filter(tipo -> tipo == TipoAvisoTrivia.FINALIZADA).count());
        assertEquals(1, finalizaciones.size());
        JsonNode estado = finalizaciones.get(0).path("estado");
        assertTrue(estado.path("finalizada").asBoolean());
        assertEquals(1L, estado.path("idGanador").asLong());
        assertEquals("Ana", estado.path("ganador").asText());
        assertTrue(estado.path("premioPendiente").isBoolean());
        assertFalse(estado.path("premioPendiente").asBoolean());
    }

    private List<JsonNode> registrarCierresWebSocket() {
        List<JsonNode> cierres = new ArrayList<>();
        NotificadorPartida notificador = mock(NotificadorPartida.class);
        doAnswer(invocacion -> {
            Map<String, Object> mensaje = invocacion.getArgument(1);
            if (mensaje.get("tipo") == TipoAvisoTrivia.RONDA_CERRADA) {
                cierres.add(new ObjectMapper().valueToTree(mensaje));
            }
            return null;
        }).when(notificador).notificarTrivia(eq(CODIGO), any());
        publicador.suscribir(new ObservadorTriviaWebSocket(notificador));
        return cierres;
    }

    @Test
    public void elCierreWebSocketIdentificaLaRondaAnteriorYNoLaPreguntaSiguiente() {
        List<JsonNode> cierres = registrarCierresWebSocket();
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);
        trivia.prepararPreguntas(List.of(
                new PreguntaGenerada("DIFICIL", "Primera pregunta", List.of("A", "B", "C", "D"), CORRECTA),
                new PreguntaGenerada("DIFICIL", "Siguiente pregunta", List.of("A", "B", "C", "D"), 3)));
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, CORRECTA);
        gestor.cerrarPorTiempo(CODIGO, trivia, 1);

        assertEquals(2, trivia.getRondaActual());
        assertEquals(3, trivia.getPreguntaActual().getRespuestaCorrecta());
        assertEquals(1, cierres.size());
        JsonNode cierre = cierres.get(0);
        assertEquals(1, cierre.path("rondaCerrada").asInt());
        assertEquals(CORRECTA, cierre.path("opcionCorrecta").asInt(-1));
        assertEquals(2, cierre.path("estado").path("ronda").asInt());
        assertEquals(0, cierre.path("eliminados").size());
        assertTrue(cierre.path("eliminados").isArray());
        assertFalse(cierre.has("pregunta"));
    }

    @Test
    public void elCierreWebSocketIncluyeAlEliminadoAntesDeLaUltimaRespuestaYSoloEnSuRonda() {
        Jugador sol = jugador(3L, "Sol");
        partida.getJugadores().add(sol);
        List<JsonNode> cierres = registrarCierresWebSocket();
        gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);
        gestor.responder(CODIGO, 3L, 1, INCORRECTA);
        assertTrue(cierres.isEmpty());
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, CORRECTA);

        assertEquals(1, cierres.size());
        JsonNode eliminados = cierres.get(0).path("eliminados");
        assertEquals(1, eliminados.size());
        assertEquals(3L, eliminados.path(0).path("id").asLong());
        assertEquals("Sol", eliminados.path(0).path("apodo").asText());
        java.util.Set<String> campos = new java.util.HashSet<>();
        eliminados.path(0).fieldNames().forEachRemaining(campos::add);
        assertTrue(campos.containsAll(java.util.Set.of("id", "apodo")));
        assertTrue(java.util.Set.of("id", "apodo", "milisegundos").containsAll(campos));

        gestor.responder(CODIGO, 1L, 2, CORRECTA);
        gestor.responder(CODIGO, 2L, 2, CORRECTA);

        assertEquals(2, cierres.size());
        assertEquals(2, cierres.get(1).path("rondaCerrada").asInt());
        assertTrue(cierres.get(1).path("eliminados").isArray());
        assertEquals(0, cierres.get(1).path("eliminados").size());
    }

    @Test
    public void elCierreWebSocketPorVencimientoIncluyeErroresPreviosYJugadoresSinRespuestaUnaSolaVez() {
        partida.getJugadores().add(jugador(3L, "Sol"));
        List<JsonNode> cierres = registrarCierresWebSocket();
        gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);
        gestor.responder(CODIGO, 3L, 1, INCORRECTA);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        Runnable cierreProgramado = cierreProgramado(1);
        reloj.adelantar(Duration.ofSeconds(31));

        cierreProgramado.run();
        cierreProgramado.run();

        assertEquals(1, cierres.size());
        JsonNode cierre = cierres.get(0);
        List<Long> eliminados = new ArrayList<>();
        cierre.path("eliminados").forEach(jugador -> eliminados.add(jugador.path("id").asLong()));
        assertEquals(List.of(2L, 3L), eliminados.stream().sorted().toList());
        assertEquals(1, cierre.path("rondaCerrada").asInt());
        assertEquals(CORRECTA, cierre.path("opcionCorrecta").asInt(-1));
        assertTrue(cierre.path("estado").path("finalizada").asBoolean());
        assertEquals(1, cierre.path("estado").path("enJuego").size());
        assertEquals(1L, cierre.path("estado").path("enJuego").path(0).path("id").asLong());
    }

    @Test
    public void elCierreWebSocketFinalConVariosParticipantesNoLosPresentaComoEliminados() {
        List<JsonNode> cierres = registrarCierresWebSocket();
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);
        for (int ronda = 1; ronda <= 5; ronda++) {
            gestor.responder(CODIGO, 1L, ronda, CORRECTA);
            gestor.responder(CODIGO, 2L, ronda, CORRECTA);
        }

        assertTrue(trivia.estaFinalizada());
        assertEquals(5, cierres.size());
        JsonNode cierre = cierres.get(4);
        assertEquals(5, cierre.path("rondaCerrada").asInt());
        assertEquals(CORRECTA, cierre.path("opcionCorrecta").asInt(-1));
        assertTrue(cierre.path("estado").path("finalizada").asBoolean());
        assertEquals(2, cierre.path("estado").path("enJuego").size());
        assertTrue(cierre.path("eliminados").isArray());
        assertEquals(0, cierre.path("eliminados").size());
        assertTrue(cierre.path("estado").path("idGanador").isNull());
    }

    private Runnable cierreProgramado(int cantidadEsperada) {
        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        verify(planificador, times(cantidadEsperada)).schedule(tarea.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));
        return tarea.getAllValues().get(cantidadEsperada - 1);
    }

    // Ana acierta y Luna se equivoca: queda Ana sola y gana.
    private Trivia triviaGanadaPorAna(DificultadRuleta dificultad) {
        Trivia trivia = gestor.iniciar(CODIGO, dificultad);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);
        return trivia;
    }

    @Test
    public void deberiaIniciarLaTriviaEnLaPrimeraRondaConLosJugadoresHabilitados() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertEquals(1, trivia.getRondaActual());
        assertEquals(List.of(ana, luna), trivia.getParticipantes());
        assertEquals(List.of(TipoAvisoTrivia.RONDA_NUEVA), tiposAvisados());
    }

    @Test
    public void deberiaProgramarElCierreDeLaRondaParaCuandoVenceElTiempo() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        ArgumentCaptor<Long> demora = ArgumentCaptor.forClass(Long.class);
        verify(planificador).schedule(any(Runnable.class), demora.capture(), eq(TimeUnit.MILLISECONDS));
        assertTrue(demora.getValue() >= GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA * 1000L);
    }

    @Test
    public void noDeberiaIniciarOtraTriviaMientrasHayUnaEnJuego() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertThrows(AccionTriviaInvalidaException.class, () -> gestor.iniciar(CODIGO, DificultadRuleta.MEDIO));
    }

    @Test
    public void deberiaPermitirIniciarUnaNuevaTriviaCuandoLaAnteriorTermino() {
        Trivia anterior = triviaGanadaPorAna(DificultadRuleta.FACIL);

        Trivia nueva = gestor.iniciar(CODIGO, DificultadRuleta.MEDIO);

        assertTrue(anterior.estaFinalizada());
        assertFalse(nueva.estaFinalizada());
    }

    @Test
    public void noDeberiaIniciarLaTriviaDeUnaPartidaQueNoExiste() {
        assertThrows(AccionTriviaInvalidaException.class, () -> gestor.iniciar("NOEXISTE", DificultadRuleta.FACIL));
    }

    @Test
    public void noDeberiaIniciarLaTriviaSinDificultad() {
        assertThrows(IllegalArgumentException.class, () -> gestor.iniciar(CODIGO, null));
    }

    @Test
    public void deberiaFinalizarAlInstanteSiNingunJugadorPuedeParticipar() {
        ana.getEstadoCarcel().encarcelar();
        luna.setEnBancarrota(true);

        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertTrue(trivia.estaFinalizada());
        assertEquals(List.of(TipoAvisoTrivia.FINALIZADA), tiposAvisados());
        verify(planificador, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void deberiaInformarQueLaRespuestaEsCorrecta() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertTrue(gestor.responder(CODIGO, 1L, 1, CORRECTA));
    }

    @Test
    public void deberiaInformarQueLaRespuestaEsIncorrecta() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertFalse(gestor.responder(CODIGO, 1L, 1, INCORRECTA));
    }

    @Test
    public void noDeberiaPermitirResponderDosVecesLaMismaPregunta() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.responder(CODIGO, 1L, 1, CORRECTA));

        assertEquals("Ya respondiste esta pregunta.", error.getMessage());
    }

    @Test
    public void noDeberiaAceptarRespuestasAUnaPreguntaQueYaNoEstaEnJuego() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.responder(CODIGO, 1L, 4, CORRECTA));

        assertEquals("Esa pregunta ya no está en juego.", error.getMessage());
    }

    @Test
    public void noDeberiaPermitirResponderAQuienNoParticipaDeLaTrivia() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertThrows(ParticipanteTriviaNoHabilitadoException.class, () -> gestor.responder(CODIGO, 99L, 1, CORRECTA));
    }

    @Test
    public void noDeberiaPermitirResponderAUnJugadorEliminado() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);

        assertThrows(ParticipanteTriviaNoHabilitadoException.class, () -> gestor.responder(CODIGO, 2L, 1, CORRECTA));
    }

    @Test
    public void deberiaRechazarUnaOpcionInexistente() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertThrows(IllegalArgumentException.class, () -> gestor.responder(CODIGO, 1L, 1, 4));
        assertThrows(IllegalArgumentException.class, () -> gestor.responder(CODIGO, 1L, 1, null));
    }

    @Test
    public void noDeberiaPermitirResponderSiNoHayTrivia() {
        assertThrows(TriviaNoEncontradaException.class, () -> gestor.responder(CODIGO, 1L, 1, CORRECTA));
    }

    @Test
    public void noDeberiaPermitirResponderCuandoLaTriviaTermino() {
        triviaGanadaPorAna(DificultadRuleta.FACIL);

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.responder(CODIGO, 1L, 1, CORRECTA));

        assertEquals("La trivia ya terminó.", error.getMessage());
    }

    @Test
    public void cuandoTodosRespondenBienDeberiaCerrarLaRondaYPasarALaSiguiente() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, CORRECTA);

        assertEquals(2, trivia.getRondaActual());
        assertEquals(List.of(TipoAvisoTrivia.RONDA_NUEVA, TipoAvisoTrivia.RONDA_CERRADA, TipoAvisoTrivia.RONDA_NUEVA),
                tiposAvisados());
    }

    @Test
    public void laOpcionCorrectaSoloSeAvisaCuandoSeCierraLaRonda() {
        gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);

        assertNull(avisos.get(0).opcionCorrecta());
        assertEquals(1, avisos.size());

        gestor.responder(CODIGO, 2L, 1, CORRECTA);

        assertEquals(TipoAvisoTrivia.RONDA_CERRADA, avisos.get(1).tipo());
        assertEquals(CORRECTA, avisos.get(1).opcionCorrecta());
    }

    @Test
    public void deberiaRechazarRespuestasFueraDeTiempoYCerrarLaRonda() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        reloj.adelantar(Duration.ofSeconds(GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA));

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.responder(CODIGO, 1L, 1, CORRECTA));

        assertEquals("Se terminó el tiempo para responder.", error.getMessage());
        assertTrue(trivia.estaFinalizada());
        assertNull(trivia.getGanador());
    }

    @Test
    public void alVencerElTiempoDeberiaEliminarAQuienesNoRespondieronAunqueNadieMasResponda() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        reloj.adelantar(Duration.ofSeconds(GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA + 1));

        cierreProgramado(1).run();

        assertTrue(trivia.estaFinalizada());
        assertEquals(ana, trivia.getGanador());
        assertTrue(tiposAvisados().contains(TipoAvisoTrivia.FINALIZADA));
    }

    @Test
    public void elCierreProgramadoDeUnaRondaAnteriorNoDeberiaHacerNada() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);
        gestor.responder(CODIGO, 1L, 1, CORRECTA);
        gestor.responder(CODIGO, 2L, 1, CORRECTA);
        int avisosAntes = avisos.size();
        reloj.adelantar(Duration.ofSeconds(GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA + 1));

        gestor.cerrarPorTiempo(CODIGO, trivia, 1);

        assertEquals(2, trivia.getRondaActual());
        assertEquals(List.of(ana, luna), trivia.getParticipantes());
        assertEquals(avisosAntes, avisos.size());
    }

    @Test
    public void enUnaTriviaFacilDeberiaEntregarElPremioEnDineroAutomaticamente() {
        Trivia trivia = triviaGanadaPorAna(DificultadRuleta.FACIL);

        verify(servicioPremio).entregarPremioEnDinero(trivia);
        verify(servicioPartida).actualizarJugador(ana);
        assertEquals(TipoAvisoTrivia.FINALIZADA, avisos.get(avisos.size() - 1).tipo());
    }

    @Test
    public void siFallaLaEntregaAutomaticaIgualDeberiaAvisarQueTermino() {
        doAnswer(invocacion -> {
            throw new IllegalStateException("Falla");
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));

        triviaGanadaPorAna(DificultadRuleta.MEDIO);

        assertEquals(TipoAvisoTrivia.FINALIZADA, avisos.get(avisos.size() - 1).tipo());
    }

    @Test
    public void enUnaTriviaDificilNoDeberiaEntregarElPremioHastaQueElGanadorElija() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);

        verify(servicioPremio, never()).entregarPremioEnDinero(any(Trivia.class));
    }

    @Test
    public void elGanadorDeUnaTriviaDificilPuedeElegirElPremioEnDinero() {
        Trivia trivia = triviaGanadaPorAna(DificultadRuleta.DIFICIL);

        gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null);

        verify(servicioPremio).entregarPremioEnDinero(trivia);
        verify(servicioPartida).actualizarJugador(ana);
        assertEquals(TipoAvisoTrivia.PREMIO_ENTREGADO, avisos.get(avisos.size() - 1).tipo());
    }

    @Test
    public void elGanadorDeUnaTriviaDificilPuedeRobarUnaPropiedadPorSuPosicion() {
        Trivia trivia = triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        CasilleroPropiedad casillero = mock(CasilleroPropiedad.class);
        Propiedad propiedad = new Propiedad();
        propiedad.setId(77L);
        when(casillero.getPropiedad()).thenReturn(propiedad);
        when(servicioCasillero.buscarCasilleroPropiedad(partida, 9)).thenReturn(casillero);

        gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_ROBAR, 9);

        verify(servicioPremio).entregarPremioRobandoPropiedad(trivia, partida, 77L);
    }

    @Test
    public void noDeberiaPermitirRobarSinElegirUnaPropiedadDisponible() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);

        assertThrows(IllegalArgumentException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_ROBAR, null));
        assertThrows(IllegalArgumentException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_ROBAR, 15));
    }

    @Test
    public void noDeberiaAceptarUnPremioDesconocido() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);

        assertThrows(IllegalArgumentException.class, () -> gestor.elegirPremio(CODIGO, 1L, "AUTO", null));
    }

    @Test
    public void soloElGanadorPuedeElegirElPremio() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);

        assertThrows(ParticipanteTriviaNoHabilitadoException.class,
                () -> gestor.elegirPremio(CODIGO, 2L, GestorTriviaPartida.PREMIO_PLATA, null));
    }

    @Test
    public void elPremioNoSePuedeReclamarDosVeces() {
        doAnswer(invocacion -> {
            invocacion.<Trivia>getArgument(0).registrarPremioEntregado();
            return null;
        }).when(servicioPremio).entregarPremioEnDinero(any(Trivia.class));
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null);

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));

        assertEquals("El premio ya fue entregado.", error.getMessage());
        verify(servicioPremio, times(1)).entregarPremioEnDinero(any(Trivia.class));
    }

    @Test
    public void noSePuedeElegirElPremioAntesDeQueTermineLaTrivia() {
        gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);

        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));
    }

    @Test
    public void noHayPremioParaElegirSiLaTriviaTerminoSinGanador() {
        gestor.iniciar(CODIGO, DificultadRuleta.DIFICIL);
        gestor.responder(CODIGO, 1L, 1, INCORRECTA);
        gestor.responder(CODIGO, 2L, 1, INCORRECTA);

        AccionTriviaInvalidaException error = assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));

        assertEquals("La trivia terminó sin ganador.", error.getMessage());
    }

    @Test
    public void enUnaTriviaFacilElGanadorNoEligePremio() {
        triviaGanadaPorAna(DificultadRuleta.FACIL);

        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_ROBAR, 9));
    }

    @Test
    public void noSePuedeEntregarElPremioSiLaPartidaYaTermino() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(null);

        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));
    }

    @Test
    public void noSePuedeEntregarElPremioSiElGanadorYaNoEstaEnLaPartida() {
        triviaGanadaPorAna(DificultadRuleta.DIFICIL);
        partida.setJugadores(new ArrayList<>(List.of(luna)));

        assertThrows(AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(CODIGO, 1L, GestorTriviaPartida.PREMIO_PLATA, null));
    }

    @Test
    public void deberiaPermitirConsultarElEstadoYLaPreguntaEnJuego() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertSame(trivia, gestor.consultarEstado(CODIGO, leida -> leida));
        assertEquals("¿Cuál es la cabecera de La Matanza? 1",
                gestor.consultarPregunta(CODIGO, leida -> leida.getPreguntaActual().getEnunciado()));
    }

    @Test
    public void noHayPreguntaParaMostrarCuandoLaTriviaTermino() {
        triviaGanadaPorAna(DificultadRuleta.FACIL);

        assertThrows(AccionTriviaInvalidaException.class, () -> gestor.consultarPregunta(CODIGO, leida -> leida));
    }

    @Test
    public void noHayEstadoParaMostrarSiLaPartidaNuncaTuvoTrivia() {
        assertThrows(TriviaNoEncontradaException.class, () -> gestor.consultarEstado(CODIGO, leida -> leida));
    }

    @Test
    public void elVencimientoSeCalculaDesdeElInicioDeLaRonda() {
        Trivia trivia = gestor.iniciar(CODIGO, DificultadRuleta.FACIL);

        long esperado = reloj.instant().plusSeconds(GestorTriviaPartida.SEGUNDOS_POR_PREGUNTA).toEpochMilli();
        assertEquals(esperado, GestorTriviaPartida.venceEnMilisegundos(trivia));
        assertNull(GestorTriviaPartida.venceEnMilisegundos(null));
        assertNull(GestorTriviaPartida.venceEnMilisegundos(new Trivia()));
    }

    @Test
    public void siFallaElAvisoPorWebSocketLaTriviaSigueFuncionando() {
        Observador<EventoTriviaActualizada> roto = evento -> {
            throw new IllegalStateException("Sin conexión");
        };
        ServicioPreguntasTrivia preguntas = mock(ServicioPreguntasTrivia.class);
        when(preguntas.obtenerParaCasillero(any(), any())).thenReturn(cincoPreguntas());
        GestorTriviaPartida conAvisoRoto = new GestorTriviaPartida(
                new ServicioTriviaImpl(new ServicioParticipacionImpl(), preguntas, reloj),
                servicioPremio, servicioPartida, servicioCasillero, new PublicadorEventos<>(List.of(roto)),
                planificador, mock(PlatformTransactionManager.class), reloj);

        conAvisoRoto.iniciar(CODIGO, DificultadRuleta.FACIL);

        assertTrue(conAvisoRoto.responder(CODIGO, 1L, 1, CORRECTA));
    }
}
