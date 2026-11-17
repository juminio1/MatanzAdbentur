package com.tallerwebi.integracion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Evento.Trivia;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.eventos.EventoTriviaActualizada;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TriviaContinuidadIntegracionTest.Config.class)
class TriviaContinuidadIntegracionTest {
    @Configuration
    @Import({EventosGlobalesIntegracionTest.Config.class, GestorTriviaPartida.class,
            ServicioTriviaImpl.class, ServicioPremioTriviaImpl.class})
    static class Config {
        @Bean @Primary GestorTemporizadorPartida temporizadorReal(PlatformTransactionManager tx) {
            return new GestorTemporizadorPartida(tx);
        }
        @Bean ServicioPreguntasTrivia preguntas() { return mock(ServicioPreguntasTrivia.class); }
        @Bean ScheduledExecutorService planificador() { return mock(ScheduledExecutorService.class); }
        @Bean PublicadorEventos<EventoTriviaActualizada> trivias() { return new PublicadorEventos<>(List.of()); }
    }

    @Autowired SessionFactory sesiones;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired GestorTriviaPartida gestor;
    @Autowired ServicioPartida partidas;
    @Autowired ServicioPreguntasTrivia preguntas;
    @Autowired ServicioJugada jugadas;
    @Autowired ServicioDado dados;
    @Autowired ScheduledExecutorService planificador;
    @Autowired GestorTemporizadorPartida temporizador;
    private String codigo;
    private List<Long> jugadores;

    @BeforeEach
    void preparar() {
        reset(planificador, dados);
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 1));
        codigo = UUID.randomUUID().toString();
        when(preguntas.obtenerParaCasillero(anyString(), any())).thenReturn(List.of(
                new PreguntaTrivia("Pregunta", List.of("Correcta", "Incorrecta", "Otra", "Ultima"), 0, DificultadTrivia.SIMPLE)));
        new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            Usuario creador = new Usuario();
            creador.setEmail(codigo + "@test.com");
            creador.setUsername(codigo);
            sesiones.getCurrentSession().persist(creador);
            Partida partida = new Partida();
            partida.setCodigoUnico(codigo);
            partida.setCreador(creador);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(1);
            for (int i = 0; i < 2; i++) {
                Jugador jugador = new Jugador();
                jugador.setApodo("Jugador " + i);
                jugador.setOrden(i);
                jugador.setDinero(1000);
                jugador.setPosicionActual(1);
                partida.getJugadores().add(jugador);
            }
            sesiones.getCurrentSession().persist(partida);
            jugadores = partida.getJugadores().stream().map(Jugador::getId).toList();
        });
    }

    @AfterEach
    void cancelarTemporizador() {
        temporizador.cancelarTemporizador(codigo);
        temporizador.consumirPausa(codigo);
    }

    @Test
    void cierreNormalEntregaPremioPersistidoYLiberaLaPartida() {
        cerrarNormalmente();
        assertEquals(0, partidas.obtenerPartida(codigo).getIndiceTurnoActual());
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(0)));
    }

    private void cerrarNormalmente() {
        Trivia trivia = gestor.iniciar(codigo, DificultadRuleta.FACIL);
        gestor.responder(codigo, jugadores.get(0), 1, 0);
        gestor.responder(codigo, jugadores.get(1), 1, 1);

        assertTrue(trivia.estaFinalizada());
        assertTrue(trivia.estaPremioEntregado(), "El cierre normal debe confirmar el premio");
        assertEquals(1100, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(gestor.estaBloqueandoPartida(codigo));
    }

    @Test
    void cierreNormalDeQuienYaTiroContinuaElTurnoUnaSolaVez() throws Exception {
        new TransactionTemplate(transacciones).executeWithoutResult(tx ->
                sesiones.getCurrentSession().find(Jugador.class, jugadores.get(0)).setTiroEnTurno(true));
        cerrarNormalmente();
        comprobarAvanceUnicoYTiradaSiguiente();
    }

    private void comprobarAvanceUnicoYTiradaSiguiente() throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (partidas.obtenerPartida(codigo).getIndiceTurnoActual() == 0 && System.nanoTime() < limite) {
            Thread.sleep(10);
        }
        assertEquals(1, partidas.obtenerPartida(codigo).getIndiceTurnoActual());
        assertEquals(1, partidas.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
        assertFalse(partidas.obtenerJugadorPorId(jugadores.get(1)).getTiroEnTurno());
        partidas.reanudarDespuesDeTrivia(codigo);
        assertEquals(1, partidas.obtenerPartida(codigo).getIndiceTurnoActual());
        assertEquals(1, partidas.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(1)));
    }

    @Test
    void cierrePorTiempoConGanadorEntregaPremioYPermiteTirar() {
        cerrarPorTiempoConGanador();
        assertEquals(0, partidas.obtenerPartida(codigo).getIndiceTurnoActual());
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(0)));
    }

    private void cerrarPorTiempoConGanador() {
        Trivia trivia = gestor.iniciar(codigo, DificultadRuleta.FACIL);
        gestor.responder(codigo, jugadores.get(0), 1, 0);
        trivia.setInicioRonda(Instant.now().minusSeconds(31));
        ArgumentCaptor<Runnable> cierre = ArgumentCaptor.forClass(Runnable.class);
        verify(planificador).schedule(cierre.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));
        cierre.getValue().run();
        cierre.getValue().run();

        assertTrue(trivia.estaPremioEntregado());
        assertEquals(1100, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(gestor.estaBloqueandoPartida(codigo));
    }

    @Test
    void cierrePorTiempoDeQuienYaTiroContinuaElTurnoUnaSolaVez() throws InterruptedException {
        new TransactionTemplate(transacciones).executeWithoutResult(tx ->
                sesiones.getCurrentSession().find(Jugador.class, jugadores.get(0)).setTiroEnTurno(true));
        cerrarPorTiempoConGanador();
        comprobarAvanceUnicoYTiradaSiguiente();
    }

    @Test
    void premioDificilPendienteImpideTirarHastaResolverlo() {
        Trivia trivia = gestor.iniciar(codigo, DificultadRuleta.DIFICIL);
        gestor.responder(codigo, jugadores.get(0), 1, 0);
        gestor.responder(codigo, jugadores.get(1), 1, 1);

        assertFalse(trivia.estaPremioEntregado());
        assertTrue(gestor.estaBloqueandoPartida(codigo));
        assertThrows(IllegalStateException.class, () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        gestor.elegirPremio(codigo, jugadores.get(0), GestorTriviaPartida.PREMIO_PLATA, null);
        assertTrue(trivia.estaPremioEntregado());
        assertEquals(1500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertThrows(com.tallerwebi.dominio.excepcion.AccionTriviaInvalidaException.class,
                () -> gestor.elegirPremio(codigo, jugadores.get(0), GestorTriviaPartida.PREMIO_PLATA, null));
        assertEquals(1500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(gestor.estaBloqueandoPartida(codigo));
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(0)));
    }
}
