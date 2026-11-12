package com.tallerwebi.integracion;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import com.tallerwebi.dominio.servicios.ServicioJugadaImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.infraestructura.RepositorioPartida;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.dominio.observadores.ObservadorVictoriaQuiebra;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.ControladorPartida;
import com.tallerwebi.presentacion.ControladorGestionQuiebra;
import com.tallerwebi.presentacion.ControladorPropiedadesPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EfectosPartidaIntegracionTest.Config.class)
class EfectosPartidaIntegracionTest {
    private final ServicioEconomia economia = org.mockito.Mockito.spy(new ServicioEconomiaImpl());
    private final com.tallerwebi.infraestructura.RepositorioPartida consultasMovimiento = org.mockito.Mockito.mock(com.tallerwebi.infraestructura.RepositorioPartida.class);
    private final ServicioMovimiento movimiento = org.mockito.Mockito.spy(new ServicioMovimientoImpl(economia,
            consultasMovimiento, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()));

    @Configuration
    @Import({
            com.tallerwebi.dominio.servicios.ServicioParticipacionImpl.class,
            ServicioPartidaImpl.class, ServicioEconomiaImpl.class, ServicioMovimientoImpl.class, ServicioJugadaImpl.class,
            ServicioTurnoImpl.class,
            ServicioEventoCasilleroImpl.class,
            ServicioCasilleroImpl.class,
            ServicioPropiedadImpl.class,
            ServicioGestionQuiebraImpl.class,
            ServicioFinalizacionPartidaImpl.class,
            RepositorioFinalizarPartidaImpl.class,
            RepositorioPartidaImpl.class, RepositorioUsuarioImpl.class,
            RepositorioCasilleroImpl.class,
            RepositorioPropiedadImpl.class,
            RepositorioEventoCasilleroImpl.class,
            RepositorioDeudaPendienteImpl.class
    })
    static class Config extends HibernateInfraestructuraTestConfig {
        @Bean
        @Override
        public DataSource dataSource() {
            DriverManagerDataSource fuente = new DriverManagerDataSource();
            fuente.setDriverClassName("org.hsqldb.jdbcDriver");
            fuente.setUrl("jdbc:hsqldb:mem:efectos_partida");
            fuente.setUsername("sa");
            fuente.setPassword("");
            return fuente;
        }

        @Bean
        ServicioDado servicioDado() { return mock(ServicioDado.class); }
        @Bean
        GestorRuletaPartida gestorRuletaPartida() { return mock(GestorRuletaPartida.class); }
        @Bean
        ServicioInicioPartida servicioInicioPartida() { return mock(ServicioInicioPartida.class); }
        @Bean
        GestorTemporizadorPartida gestorTemporizadorPartida() { return mock(GestorTemporizadorPartida.class); }

        @Bean
        PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono() { return new PublicadorEventos<>(List.of()); }

        @Bean
        ObservadorTurnoQuiebra observadorTurnoQuiebra(
                ServicioPartida servicioPartida) {
            return new ObservadorTurnoQuiebra(servicioPartida, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        }
        @Bean
        PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra(
                ObservadorTurnoQuiebra observadorTurnoQuiebra,
                ObservadorVictoriaQuiebra observadorVictoriaQuiebra) {

            return new PublicadorEventos<>(List.of(observadorTurnoQuiebra, observadorVictoriaQuiebra));
        }
        @Bean
        ObservadorVictoria observadorVictoria(
                ServicioFinalizacionPartida finalizacion) {
            return new ObservadorVictoria(finalizacion);
        }

        @Bean
        ObservadorVictoriaQuiebra observadorVictoriaQuiebra(
                ObservadorVictoria observadorVictoria, SimpMessagingTemplate mensajeriaQuiebra) {
            return new ObservadorVictoriaQuiebra(observadorVictoria,
                    new com.tallerwebi.config.ObservadoresConfig()
                            .notificacionQuiebra(new NotificadorPartida(mensajeriaQuiebra)));
        }

        @Bean
        SimpMessagingTemplate mensajeriaQuiebra() {
            return mock(SimpMessagingTemplate.class);
        }
    }

    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioPartida servicioPartida;
    @Autowired private ServicioJugada servicioJugada;
    @Autowired private ServicioCasillero servicioCasillero;
    @Autowired private ServicioPropiedad servicioPropiedad;
    @Autowired private ServicioEventoCasillero servicioEvento;
    @Autowired private ServicioDado servicioDado;
    @Autowired private GestorRuletaPartida gestorRuleta;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private ServicioGestionQuiebra servicioGestionQuiebra;
    @Autowired private PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra;
    private TransactionTemplate transaccion;
    private ControladorPartida controlador;
    private ControladorPropiedadesPartida propiedades;
    private String codigo;
    private Long idJugador;
    private Long idPropietario;
    @Autowired private SimpMessagingTemplate notificador;

    @BeforeEach
    void preparar() {        var partidaMovimiento = new com.tallerwebi.dominio.entidades.Partida();
        partidaMovimiento.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        org.mockito.Mockito.when(consultasMovimiento.buscarPartidaPorJugador(org.mockito.ArgumentMatchers.any())).thenReturn(partidaMovimiento);
        transaccion = new TransactionTemplate(transacciones);
        reset(notificador);
        reset(gestorRuleta);
        reset(temporizador);
        when(temporizador.obtenerSegundosRestantes(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        controlador = new ControladorPartida(servicioPartida,
                notificador,
                servicioCasillero,
                servicioPropiedad,
                new NotificadorPartida(notificador),
                servicioJugada, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        var participacion = new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl();
        propiedades = new ControladorPropiedadesPartida(servicioPartida, notificador, servicioCasillero,
                servicioPropiedad, servicioGestionQuiebra, participacion,
                new com.tallerwebi.dominio.servicios.ServicioOfertaPropiedadImpl(servicioPartida, servicioCasillero,
                        servicioPropiedad, participacion));
        codigo = UUID.randomUUID().toString();
        transaccion.executeWithoutResult(estado -> {
            sesiones.getCurrentSession().createMutationQuery("DELETE FROM EventoCasillero").executeUpdate();
            Usuario creador = new Usuario();
            creador.setUsername(codigo);
            creador.setEmail(codigo + "@test.com");
            creador.setPassword("prueba");
            sesiones.getCurrentSession().persist(creador);
            Jugador jugador = crearJugador("Ana", 0);
            Jugador propietario = crearJugador("Beto", 1);
            Partida partida = new Partida();
            partida.setCreador(creador);
            partida.setCodigoUnico(codigo);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setJugadores(List.of(jugador, propietario));
            sesiones.getCurrentSession().persist(partida);
            idJugador = jugador.getId();
            idPropietario = propietario.getId();
            sesiones.getCurrentSession().persist(new CasilleroEvento(null, 3, "Eventos", "Eventos", null, null));
        });
        when(servicioDado.tirarDados()).thenReturn(new ResultadoTirada(1, 1));
    }

    // Ubica al jugador antes de un casillero de trivia para probar su caída al tirar los dados.
    private void ubicarJugadorAntesDeTrivia(Long id, int posicion) {
        //Simula una jugada que llega a trivia, pero cuya transacción se cancela.
        transaccion.executeWithoutResult(tx ->
                sesiones.getCurrentSession().find(Jugador.class, id).setPosicionActual(posicion));
    }

    private void verificarActivacionAlConfirmarCaida(int destino) {
        ubicarJugadorAntesDeTrivia(idJugador, destino - 2);
        transaccion.executeWithoutResult(tx -> {
            var resultado = assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));
            assertEquals(destino, resultado.nuevaPosicion());
            verifyNoInteractions(gestorRuleta);
        });

        var activacion = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(gestorRuleta, times(1)).activarRuleta(eq(codigo), activacion.capture(), eq(idJugador));
        assertNotNull(activacion.getValue());
        assertFalse(activacion.getValue().isBlank());
        org.mockito.Mockito.verifyNoMoreInteractions(gestorRuleta);
    }

    @Test
    void caerEnDiezActivaUnaRuletaSoloDespuesDelCommit() {
        verificarActivacionAlConfirmarCaida(10);
    }

    @Test
    void caerEnVeinteActivaUnaRuletaSoloDespuesDelCommit() {
        verificarActivacionAlConfirmarCaida(20);
    }

    @Test
    void terminarFueraDeTriviaNoActivaRuleta() {
        var resultado = assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));

        assertEquals(2, resultado.nuevaPosicion());
        verifyNoInteractions(gestorRuleta);
    }

    //Simula una jugada que llega a trivia, pero cuya transacción se cancela
    @Test
    void rollbackDeCaidaEnTriviaNoActivaRuleta() {
        ubicarJugadorAntesDeTrivia(idJugador, 8);
        transaccion.executeWithoutResult(tx -> {
            var resultado = assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));
            assertEquals(10, resultado.nuevaPosicion());
            verifyNoInteractions(gestorRuleta);
            tx.setRollbackOnly();
        });

        assertEquals(8, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verifyNoInteractions(gestorRuleta);
    }

    @Test
    void repetirLaTiradaDelMismoTurnoNoActivaOtraRuleta() {
        ubicarJugadorAntesDeTrivia(idJugador, 8);
        assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));

        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> servicioJugada.tirarYMover(codigo, idJugador));

        verify(gestorRuleta, times(1)).activarRuleta(eq(codigo), any(String.class), eq(idJugador));
        org.mockito.Mockito.verifyNoMoreInteractions(gestorRuleta);
        assertEquals(10, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
    }

    @Test
    void dosCaidasValidasGeneranIdentificacionesDeActivacionDistintas() {
        ubicarJugadorAntesDeTrivia(idJugador, 8);
        ubicarJugadorAntesDeTrivia(idPropietario, 18);
        assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));
        servicioPartida.avanzarTurno(codigo);
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());

        assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idPropietario));

        var primera = org.mockito.ArgumentCaptor.forClass(String.class);
        var segunda = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(gestorRuleta).activarRuleta(eq(codigo), primera.capture(), eq(idJugador));
        verify(gestorRuleta).activarRuleta(eq(codigo), segunda.capture(), eq(idPropietario));
        assertFalse(primera.getValue().isBlank());
        assertFalse(segunda.getValue().isBlank());
        assertNotEquals(primera.getValue(), segunda.getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(gestorRuleta);
    }

    @Test
    void unaTarjetaQueTerminaEnTriviaActivaRuletaPorLaPosicionFinalEfectiva() {
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 10);

        var resultado = assertDoesNotThrow(() -> servicioJugada.tirarYMover(codigo, idJugador));

        assertEquals(10, resultado.nuevaPosicion());
        verify(gestorRuleta, times(1)).activarRuleta(eq(codigo), any(String.class), eq(idJugador));
        org.mockito.Mockito.verifyNoMoreInteractions(gestorRuleta);
    }

    @Test
    void tiradaManualConPlazoVencidoSeRechazaAntesDeGenerarDados() {
        when(temporizador.obtenerSegundosRestantes(codigo)).thenReturn(0);
        org.mockito.Mockito.clearInvocations(servicioDado);

        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> servicioJugada.tirarYMover(codigo, idJugador));

        verifyNoInteractions(servicioDado);
        assertEquals(0, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());
    }

    @Test
    void vencimientoSinTirarNoEjecutaDadosEventosNiAlquileres() {
        cargarEvento(TipoEfectoEvento.SUMA, 500, null, null);
        cargarPropiedadConDueno();
        Runnable vencimiento = capturarVencimiento();
        org.mockito.Mockito.clearInvocations(servicioDado, notificador);

        transaccion.executeWithoutResult(tx -> vencimiento.run());
        transaccion.executeWithoutResult(tx -> vencimiento.run());

        verifyNoInteractions(servicioDado);
        assertEquals(1, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idPropietario).getDinero());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertEquals(1, servicioPartida.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
        verify(notificador, never()).convertAndSend(eq("/topic/partida/" + codigo), any(Object.class));
    }

    @Test
    void vencimientoYTiradaManualConcurrentesConPlazoCeroNoGeneranMovimiento() throws Exception {
        Runnable vencimiento = capturarVencimiento();
        org.mockito.Mockito.clearInvocations(servicioDado);
        var inicio = new java.util.concurrent.CyclicBarrier(2);
        var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var manual = ejecutor.submit(() -> {
                inicio.await(10, java.util.concurrent.TimeUnit.SECONDS);
                assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                        () -> servicioJugada.tirarYMover(codigo, idJugador));
                return null;
            });
            var plazo = ejecutor.submit(() -> {
                inicio.await(10, java.util.concurrent.TimeUnit.SECONDS);
                transaccion.executeWithoutResult(tx -> vencimiento.run());
                return null;
            });
            manual.get(15, java.util.concurrent.TimeUnit.SECONDS);
            plazo.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        verifyNoInteractions(servicioDado);
        assertEquals(0, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertEquals(1, servicioPartida.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
    }

    @Test
    void tiradaEnCursoYVencimientoConcurrentesMuevenUnaVezYDejanLaCompraSinAceptar() throws Exception {
        cargarPropiedadDisponible(2);
        transaccion.executeWithoutResult(tx ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(0));
        Runnable vencimiento = capturarVencimiento();
        var restantes = new java.util.concurrent.atomic.AtomicInteger(60);
        when(temporizador.obtenerSegundosRestantes(codigo)).thenAnswer(invocacion -> restantes.get());
        var dadosGenerados = new java.util.concurrent.CountDownLatch(1);
        var liberarDados = new java.util.concurrent.CountDownLatch(1);
        var callbackIniciado = new java.util.concurrent.CountDownLatch(1);
        when(servicioDado.tirarDados()).thenAnswer(invocacion -> {
            dadosGenerados.countDown();
            assertTrue(liberarDados.await(10, java.util.concurrent.TimeUnit.SECONDS));
            return new ResultadoTirada(1, 1);
        });
        org.mockito.Mockito.clearInvocations(servicioDado);
        var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var manual = ejecutor.submit(() -> servicioJugada.tirarYMover(codigo, idJugador));
            assertTrue(dadosGenerados.await(10, java.util.concurrent.TimeUnit.SECONDS));
            restantes.set(0);
            var plazo = ejecutor.submit(() -> {
                callbackIniciado.countDown();
                transaccion.executeWithoutResult(tx -> vencimiento.run());
            });
            assertTrue(callbackIniciado.await(10, java.util.concurrent.TimeUnit.SECONDS));
            liberarDados.countDown();
            assertEquals(2, manual.get(15, java.util.concurrent.TimeUnit.SECONDS).nuevaPosicion());
            plazo.get(15, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            liberarDados.countDown();
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        verify(servicioDado, times(1)).tirarDados();
        assertEquals(2, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertEquals(1, servicioPartida.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
        var partida = servicioPartida.obtenerPartida(codigo);
        assertNull(servicioCasillero.buscarCasilleroPropiedad(partida, 2).getPropiedad().getPropietario());
        transaccion.executeWithoutResult(tx -> vencimiento.run());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
    }

    private Runnable capturarVencimiento() {
        org.mockito.Mockito.clearInvocations(temporizador);
        when(temporizador.programarTemporizadorSiNoExiste(eq(codigo), eq(60), any(Runnable.class)))
                .thenReturn(true);
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigo);
        var callback = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(temporizador).programarTemporizadorSiNoExiste(eq(codigo), eq(60), callback.capture());
        when(temporizador.obtenerSegundosRestantes(codigo)).thenReturn(0);
        return callback.getValue();
    }

    @Test
    void consultarRelojNoAbreConexionesYConsultarPausaUsaUnaSolaConsulta() {
        var estadisticas = sesiones.getStatistics();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            servicioPartida.obtenerSegundosRestantesTurno(codigo);
            assertEquals(0, estadisticas.getConnectCount(), "El reloj se consulta en memoria");
            assertFalse(servicioPartida.turnoPausadoPorDeuda(codigo));
            assertEquals(1, estadisticas.getPrepareStatementCount(), "No consultar por cada jugador");
        } finally {
            estadisticas.setStatisticsEnabled(habilitadas);
        }
    }

    @Test
    void medirConsultasDeUnaTiradaConPropiedadDisponible() throws Exception {
        cargarPropiedadDisponible(2);
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(0));
        var solicitud = solicitudJugador(codigo, idJugador);
        var estadisticas = sesiones.getStatistics();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            var resultado = controlador.tirarDados(codigo, idJugador, solicitud);
            assertEquals(2, resultado.getNuevaPosicion());
            System.out.println("COSTO_TIRADA consultas=" + estadisticas.getPrepareStatementCount()
                    + " conexiones=" + estadisticas.getConnectCount());
        } finally {
            estadisticas.setStatisticsEnabled(habilitadas);
        }
    }

    @Test
    void noPuedeRepetirLaTiradaMientrasDecideLaCompraPeroPuedeEnSuSiguienteTurno() throws Exception {
        cargarPropiedadDisponible(2);
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(0));
        clearInvocations(servicioDado);
        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo, idJugador));
        assertTrue(servicioPartida.obtenerJugadorPorId(idJugador).getTiroEnTurno());
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo, idJugador)));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(servicioDado, times(1)).tirarDados();
        assertEquals(2, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());

        servicioPartida.avanzarTurno(codigo);
        servicioPartida.avanzarTurno(codigo);
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getTiroEnTurno());
        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo, idJugador));
        verify(servicioDado, times(2)).tirarDados();
    }

    @Test
    void unaSesionAjenaNoPuedeTirarNiComprarAunqueEnvieElIdentificadorDelJugadorActual() {
        cargarPropiedadDisponible(2);
        MockHttpServletRequest request = solicitudJugador(codigo, idPropietario);
        request.getSession().setAttribute("id", idJugador);
        clearInvocations(servicioDado);

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controlador.tirarDados(codigo, idJugador, request));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, propiedades.comprarPropiedad(codigo, 2, request).getStatusCode());
        verify(servicioDado, never()).tirarDados();
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertNull(propiedades.obtenerInfoPropiedad(codigo, 2).getBody().get("propietarioId"));
    }

    @Test
    void comprarYTirarRequierenSesionAutenticada() {
        cargarPropiedadDisponible(2);
        var request = new MockHttpServletRequest();
        var compra = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> propiedades.comprarPropiedad(codigo, 2, request));
        var tirada = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controlador.tirarDados(codigo, idJugador, request));
        assertEquals(HttpStatus.UNAUTHORIZED, compra.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, tirada.getStatusCode());
    }

    @Test
    void avanzarTurnoUsaElJugadorDeSesionAunqueSuIdDeUsuarioSeaDiferente() {
        MockHttpServletRequest request = solicitudJugador(codigo, idJugador);
        request.getSession().setAttribute("id", idJugador + 1000);
        var respuesta = controlador.avanzarTurno(codigo, request);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
    }

    @Test
    void unEventoDeDosVueltasNoOtorgaPremioAunqueTermineEnLaMismaPosicion() throws Exception {
        cargarEvento(TipoEfectoEvento.AVANZA_CASILLEROS, null, 60, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        assertEquals(3, resultado.getNuevaPosicion());
        assertEquals(0, resultado.getDineroPorInicio());
        assertEquals(2000, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2000, resultado.getSaldos().stream()
                .filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @Test
    void unaJugadaFallidaRevierteMovimientoYPremioSinNotificar() {
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(28));
        when(servicioDado.tirarDados()).thenReturn(new ResultadoTirada(2, 3));
        reset(temporizador, notificador);
        when(temporizador.obtenerSegundosRestantes(codigo)).thenReturn(null);

        assertThrows(com.tallerwebi.dominio.excepcion.EventoCasilleroNoEncontradoException.class,
                () -> controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador)));

        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        assertEquals(28, jugador.getPosicionActual());
        assertEquals(2000, jugador.getDinero());
        assertFalse(Boolean.TRUE.equals(jugador.getTiroEnTurno()));
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());
        verifyNoInteractions(notificador);
        verify(temporizador).obtenerSegundosRestantes(codigo);
        org.mockito.Mockito.verifyNoMoreInteractions(temporizador);
    }

    @Test
    void rollbackDeJugadaNoPublicaMovimientoNiProgramaElSiguienteTurno() {
        when(servicioDado.tirarDados()).thenReturn(new ResultadoTirada(3, 5));
        reset(temporizador, notificador);
        when(temporizador.obtenerSegundosRestantes(codigo)).thenReturn(null);
        transaccion.executeWithoutResult(estado -> {
            try {
                controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            verifyNoInteractions(notificador);
            verify(temporizador, never()).programarTemporizador(any(), any(), any());
            estado.setRollbackOnly();
        });
        assertEquals(0, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());
        verifyNoInteractions(notificador);
        verify(temporizador, never()).programarTemporizador(any(), any(), any());
        verify(temporizador, never()).cancelarTemporizador(any());
    }

    private Jugador crearJugador(String apodo, Integer orden) {
        Jugador jugador = new Jugador();
        jugador.setApodo(apodo);
        jugador.setOrden(orden);
        jugador.setDinero(2000);
        return jugador;
    }

    @Test
    void quiebraVoluntariaNotificaLaVictoriaDespuesDelCommit() {
        registrarDeudaVoluntaria();
        transaccion.executeWithoutResult(estado -> {
            var respuesta = controladorQuiebra().declararQuiebra(codigo, sesion(idJugador));
            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verifyNoInteractions(notificador);
        });
        assertNull(servicioPartida.obtenerPartida(codigo));
        verify(notificador).convertAndSend("/topic/partida/" + codigo + "/abandono",
                Map.of("motivo", "QUIEBRA", "idJugador", idJugador, "apodo", "Ana",
                        "partidaFinalizada", true, "idGanador", idPropietario));
    }

    @Test
    void quiebraVoluntariaConTresJugadoresContinuaSinAnunciarVictoria() {
        prepararCantidadJugadores(3);
        registrarDeudaVoluntaria();

        assertEquals(HttpStatus.OK, controladorQuiebra().declararQuiebra(codigo, sesion(idJugador)).getStatusCode());

        assertEquals(EstadoPartida.EN_CURSO, servicioPartida.obtenerPartida(codigo).getEstado());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        verify(notificador).convertAndSend("/topic/partida/" + codigo + "/abandono",
                Map.of("motivo", "QUIEBRA", "idJugador", idJugador, "apodo", "Ana", "partidaFinalizada", false));
    }

    @Test
    void rollbackDeQuiebraVoluntariaNoEnviaNotificacion() {
        registrarDeudaVoluntaria();
        com.tallerwebi.dominio.observadores.Observador<EventoJugadorQuebrado> falloCommit = evento ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("Fallo al confirmar");
                    }
                });
        publicadorQuiebra.suscribir(falloCommit);
        try {
            assertThrows(IllegalStateException.class,
                    () -> controladorQuiebra().declararQuiebra(codigo, sesion(idJugador)));
        } finally {
            publicadorQuiebra.desuscribir(falloCommit);
        }

        verifyNoInteractions(notificador);
        assertEquals(EstadoPartida.EN_CURSO, servicioPartida.obtenerPartida(codigo).getEstado());
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());
        assertNotNull(servicioGestionQuiebra.obtenerDeudaPendiente(codigo, idJugador));
    }

    private void registrarDeudaVoluntaria() {
        servicioGestionQuiebra.registrarDeuda(codigo, servicioPartida.obtenerJugadorPorId(idJugador),
                servicioPartida.obtenerJugadorPorId(idPropietario), 3000, "ALQUILER");
    }

    private ControladorGestionQuiebra controladorQuiebra() {
        return new ControladorGestionQuiebra(servicioPartida, servicioGestionQuiebra, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }

    @Test
    void alquilerImpagableSinPropiedadesNotificaLaQuiebraSoloDespuesDelCommit() {
        cargarPropiedadConDueno();
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setDinero(3));

        transaccion.executeWithoutResult(estado -> {
            try {
                controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
            } catch (Exception excepcion) {
                throw new IllegalStateException(excepcion);
            }
            verify(notificador, never()).convertAndSend(
                    eq("/topic/partida/" + codigo + "/abandono"), any(Map.class));
        });

        assertTrue(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());
        assertNull(servicioPartida.obtenerPartida(codigo));
        assertNull(servicioGestionQuiebra.obtenerDeudaPendiente(codigo, idJugador));
        verify(notificador).convertAndSend("/topic/partida/" + codigo + "/abandono",
                Map.of("motivo", "QUIEBRA", "idJugador", idJugador, "apodo", "Ana",
                        "partidaFinalizada", true, "idGanador", idPropietario));
    }

    @Test
    void rollbackDeQuiebraAutomaticaPorEventoNoEnviaNotificacion() {
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);
        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession().find(Jugador.class, idJugador);
            jugador.setPosicionActual(3);
            jugador.setDinero(100);
            servicioEvento.aplicarSiCorresponde(codigo, jugador);
            assertTrue(jugador.getEnBancarrota());
            verifyNoInteractions(notificador);
            estado.setRollbackOnly();
        });

        assertEquals(EstadoPartida.EN_CURSO, servicioPartida.obtenerPartida(codigo).getEstado());
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());
        verifyNoInteractions(notificador);
    }

    private void prepararCantidadJugadores(Integer cantidad) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigo);
            for (Integer orden = 2; orden < cantidad; orden++) {
                Jugador jugador = crearJugador("Jugador " + orden, orden);
                sesiones.getCurrentSession().persist(jugador);
                partida.getJugadores().add(jugador);
            }
        });
    }

    private Long idDelOrden(Integer orden) {
        return servicioPartida.obtenerPartida(codigo).getJugadores().stream()
                .filter(jugador -> orden.equals(jugador.getOrden())).findFirst().orElseThrow().getId();
    }

    private void cargarEvento(TipoEfectoEvento tipo, Integer monto, Integer pasos, Integer destino) {
        transaccion.executeWithoutResult(estado -> {
            sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(1);
            sesiones.getCurrentSession().persist(new EventoCasillero(null, codigo, "Descripción", tipo, monto, pasos, destino));
        });
    }

    @Test
    void queUnEventoRestaRegistreDeudaSiElJugadorTienePropiedades() throws Exception{        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Propiedad propiedad = new Propiedad(
                    null, "Propiedad de Ana", 300, 50, jugador, null);

            CasilleroPropiedad casillero = new CasilleroPropiedad(
                    null, 4, "Propiedad de Ana", "Barrio", propiedad);

            casillero.setPartidaId(
                    servicioPartida.obtenerPartida(codigo).getId());

            sesiones.getCurrentSession().persist(casillero);
        });

        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        Jugador jugadorActualizado =
                servicioPartida.obtenerJugadorPorId(idJugador);

        DeudaPendiente deuda = servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador);

        assertEquals(100, jugadorActualizado.getDinero());
        assertNotNull(deuda);
        assertEquals(200, deuda.getMontoPendiente());
        assertEquals("EVENTO_RESTA", deuda.getMotivo());
        assertFalse(jugadorActualizado.getEnBancarrota());
        verify(notificador, never()).convertAndSend(
                eq("/topic/partida/" + codigo + "/abandono"), any(Map.class));
    }
    @RepeatedTest(3)
    void queElEventoQueDaPlataSeGuardeAlTerminarLaTirada(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.SUMA, 500, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(2500, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2500, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queElEventoQueQuitaPlataSeGuardeAlTerminarLaTirada(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(1800, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(1800, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queElEventoAvanceLaFichaYGuardeElDestino(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_CASILLEROS, null, 2, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(3, resultado.getPosicionCaidaEvento());
        assertEquals(5, resultado.getNuevaPosicion());
        assertEquals(5, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verify(temporizador, never()).programarTemporizador(eq(codigo), any(), any(Runnable.class));
    }

    @RepeatedTest(3)
    void queElEventoLleveAlDestinoIndicado(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 22);
        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(22, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verify(temporizador, never()).programarTemporizador(eq(codigo), any(), any(Runnable.class));
    }

    @RepeatedTest(3)
    void queElEventoSalteeSolamenteElProximoTurno(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.PIERDE_TURNO, null, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertTrue(servicioPartida.obtenerJugadorPorId(idJugador).getPierdeTurno());
        // El jugador confirma la presentación antes de finalizar su turno actual.
        assertEquals(idJugador, resultado.getTurnoSiguiente().getIdJugadorActual());
        servicioPartida.avanzarTurno(codigo);
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        verify(temporizador).cancelarTemporizador(codigo);
        verify(temporizador).programarTemporizador(eq(codigo), eq(60), any(Runnable.class));
        for (Integer orden = 1; orden < cantidad; orden++) {
            assertEquals(idDelOrden(orden), servicioPartida.obtenerJugadorActual(codigo).getId());
            servicioPartida.avanzarTurno(codigo);
        }
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getPierdeTurno());
        for (Integer avance = 1; avance < cantidad; avance++) {
            servicioPartida.avanzarTurno(codigo);
        }
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());
    }

    @RepeatedTest(3)
    void queElAlquilerSeDescuenteYSeAcrediteAlPropietario(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarPropiedadConDueno();
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(1950, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2050, servicioPartida.obtenerJugadorPorId(idPropietario).getDinero());
        assertEquals(50, resultado.getAlquiler().monto());
        assertEquals(idJugador, resultado.getAlquiler().idJugadorPagador());
        assertEquals(1950, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
        assertEquals(2050, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idPropietario))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queUnEventoQueCruzaInicioYTerminaEnPropiedadAjenaInformeElSaldoFinal(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 2);
        cargarPropiedadConDueno();
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));
        assertEquals(2, resultado.getNuevaPosicion());
        assertEquals(0, resultado.getDineroPorInicio());
        assertEquals(50, resultado.getAlquiler().monto());
        assertEquals(1950, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(1950, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @Test
    void dadosYTarjetaCruzanInicioPeroSoloLosDadosOtorganPremio() throws Exception {
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 2);
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(28));
        when(servicioDado.tirarDados()).thenReturn(new ResultadoTirada(2, 3));

        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador,
                solicitudJugador(codigo, idJugador));

        assertEquals(3, resultado.getPosicionCaidaEvento());
        assertEquals(2, resultado.getNuevaPosicion());
        assertEquals(200, resultado.getDineroPorInicio());
        assertEquals(2200, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2200, resultado.getSaldos().stream()
                .filter(saldo -> saldo.idJugador().equals(idJugador)).findFirst().orElseThrow().dinero());
    }

    private void cargarPropiedadConDueno() {
        transaccion.executeWithoutResult(estado -> {
            Jugador propietario = sesiones.getCurrentSession().find(Jugador.class, idPropietario);
            Propiedad propiedad = new Propiedad(null, "Plaza", 300, 50, propietario, null);
            CasilleroPropiedad casillero = new CasilleroPropiedad(null, 2, "Plaza", "Plaza", propiedad);
            casillero.setPartidaId(servicioPartida.obtenerPartida(codigo).getId());
            sesiones.getCurrentSession().persist(casillero);
        });
    }

    @Test
    void queElJugadorQuiebreSiNoPuedePagarUnEventoYNoTienePropiedades() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession()
                        .find(Jugador.class, idJugador)
                        .setDinero(100));

        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);

        assertEquals(100, jugador.getDinero());
        assertTrue(jugador.getEnBancarrota());

        assertTrue(resultado.getSaldos().stream()
                .filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst()
                .orElseThrow()
                .enBancarrota());

        Partida partidaFinalizada = transaccion.execute(estado ->
                sesiones.getCurrentSession()
                        .createQuery(
                                "FROM Partida WHERE codigoUnico = :codigo",
                                Partida.class)
                        .setParameter("codigo", codigo)
                        .getSingleResult());

        assertEquals(EstadoPartida.FINALIZADA, partidaFinalizada.getEstado());
        verify(notificador).convertAndSend("/topic/partida/" + codigo + "/abandono",
                Map.of("motivo", "QUIEBRA", "idJugador", idJugador, "apodo", "Ana",
                        "partidaFinalizada", true, "idGanador", idPropietario));
    }

    @Test
    void queLaPartidaContinueCuandoQuiebraUnJugadorYQuedanDosActivos() throws Exception {
        prepararCantidadJugadores(3);
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession()
                        .find(Jugador.class, idJugador)
                        .setDinero(100));

        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        Jugador jugadorQuebrado = servicioPartida.obtenerJugadorPorId(idJugador);
        Jugador jugadorSiguiente = servicioPartida.obtenerJugadorActual(codigo);

        assertTrue(jugadorQuebrado.getEnBancarrota());
        assertNotNull(jugadorSiguiente);
        assertEquals(idPropietario, jugadorSiguiente.getId());

        Partida partidaActualizada = servicioPartida.obtenerPartida(codigo);
        assertEquals(EstadoPartida.EN_CURSO, partidaActualizada.getEstado());
        verify(notificador).convertAndSend("/topic/partida/" + codigo + "/abandono",
                Map.of("motivo", "QUIEBRA", "idJugador", idJugador, "apodo", "Ana", "partidaFinalizada", false));
    }

    @AfterAll
    static void afterAll() {

    }

    @RepeatedTest(3)
    void queSePuedaComprarYOfrecerPropiedadesACadaUnoDeLosOtrosJugadores(RepetitionInfo repeticion) {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        when(temporizador.obtenerSegundosRestantes(codigo)).thenReturn(60);
        List<Integer> posiciones = List.of(2, 4, 5);
        for (Integer orden = 1; orden < cantidad; orden++) {
            Integer posicion = posiciones.get(orden - 1);
            cargarPropiedadDisponible(posicion);
            // Simula la caída tras tirar, condición necesaria para ofrecer la compra.
            transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador).setTiroEnTurno(true));
            assertEquals(true, propiedades.obtenerInfoPropiedad(codigo, posicion).getBody().get("disponible"));
            var compra = propiedades.comprarPropiedad(codigo, posicion, solicitudJugador(codigo, null));
            assertEquals(HttpStatus.OK, compra.getStatusCode(), () -> String.valueOf(compra.getBody()));
            Long comprador = idDelOrden(orden);
            var oferta = propiedades.crearOferta(codigo, posicion,
                    Map.of("idJugadorComprador", comprador, "precio", 450), sesion(idJugador));
            assertEquals(HttpStatus.OK, oferta.getStatusCode());
            for (Integer destinatario = 0; destinatario < cantidad; destinatario++) {
                List<?> pendientes = (List<?>) propiedades.ofertasPendientes(codigo, sesion(idDelOrden(destinatario))).getBody();
                assertEquals(destinatario.equals(orden) ? 1 : 0, pendientes.size());
            }
            String idOferta = (String) oferta.getBody().get("idOferta");
            var venta = propiedades.responderOferta(codigo, idOferta, Map.of("aceptar", true), sesion(comprador));
            assertEquals(HttpStatus.OK, venta.getStatusCode());
            assertEquals(comprador, venta.getBody().get("propietarioId"));
            assertEquals(1550, servicioPartida.obtenerJugadorPorId(comprador).getDinero());
            assertEquals(2000 + 150 * orden, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        }
    }

    @RepeatedTest(3)
    void queSePuedaVenderUnaPropiedadAlBanco(RepetitionInfo repeticion) {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarPropiedadConDueno();
        var venta = propiedades.venderPropiedad(codigo, 2, sesion(idPropietario));
        assertEquals(HttpStatus.OK, venta.getStatusCode());
        assertEquals(2150, servicioPartida.obtenerJugadorPorId(idPropietario).getDinero());
        assertEquals(true, propiedades.obtenerInfoPropiedad(codigo, 2).getBody().get("disponible"));
    }

    @Test
    void queAlVenderUnaPropiedadSePagueLaDeudaPendiente() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Propiedad propiedad = new Propiedad(
                    null, "Propiedad de Ana", 300, 50, jugador, null);

            CasilleroPropiedad casillero = new CasilleroPropiedad(
                    null, 4, "Propiedad de Ana", "Barrio", propiedad);

            casillero.setPartidaId(
                    servicioPartida.obtenerPartida(codigo).getId());

            sesiones.getCurrentSession().persist(casillero);
        });

        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));

        var respuesta = propiedades.venderPropiedad(
                codigo, 4, sesion(idJugador));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());

        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);

        assertEquals(50, jugador.getDinero());
        assertFalse(jugador.getEnBancarrota());

        assertNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
    }

    @Test
    void queSePuedanVenderVariasPropiedadesParaPagarUnaDeuda() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 400, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Partida partida = servicioPartida.obtenerPartida(codigo);

            for (Integer posicion : List.of(4, 5)) {
                Propiedad propiedad = new Propiedad(
                        null, "Propiedad " + posicion, 300, 50, jugador, null);

                CasilleroPropiedad casillero = new CasilleroPropiedad(
                        null, posicion, "Propiedad " + posicion, "Barrio", propiedad);

                casillero.setPartidaId(partida.getId());
                sesiones.getCurrentSession().persist(casillero);
            }
        });

        controlador.tirarDados(codigo, idJugador, solicitudJugador(codigo,  idJugador));

        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));

        var primeraVenta = propiedades.venderPropiedad(
                codigo, 4, sesion(idJugador));

        assertEquals(HttpStatus.OK, primeraVenta.getStatusCode());
        assertEquals(250, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());

        var segundaVenta = propiedades.venderPropiedad(
                codigo, 5, sesion(idJugador));

        assertEquals(HttpStatus.OK, segundaVenta.getStatusCode());
        assertEquals(0, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());
    }

    private void cargarPropiedadDisponible(Integer posicion) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigo);
            partida.setIndiceTurnoActual(0);
            sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(posicion);
            Propiedad propiedad = new Propiedad(null, "Propiedad " + posicion, 300, 50, null, null);
            CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, propiedad.getNombre(), "Barrio", propiedad);
            casillero.setPartidaId(partida.getId());
            sesiones.getCurrentSession().persist(casillero);
        });
    }

    private MockHttpServletRequest sesion(Long id) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", id);
        return request;
    }
    private org.springframework.mock.web.MockHttpServletRequest solicitudJugador(String codigo, Long idJugador) {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        Partida partida = servicioPartida.obtenerPartida(codigo);
        Jugador jugador = idJugador == null ? servicioPartida.obtenerJugadorActual(codigo) : partida == null || partida.getJugadores() == null ? null : partida.getJugadores().stream()
                .filter(j -> idJugador == null || idJugador.equals(j.getId())).findFirst().orElse(null);
        if (jugador != null && jugador.getApodo() == null) jugador.setApodo("jugador" + jugador.getId());
        request.getSession().setAttribute("NOMBRE", jugador == null ? "inexistente" : jugador.getApodo());
        return request;
    }
}
