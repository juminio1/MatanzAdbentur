package com.tallerwebi.integracion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.eventos.*;
import com.tallerwebi.dominio.observadores.*;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.*;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EventosGlobalesIntegracionTest.Config.class)
class EventosGlobalesIntegracionTest {
    @Configuration
    @Import({ServicioPartidaImpl.class, ServicioTurnoImpl.class, ServicioParticipacionImpl.class,
            ServicioEventoImpl.class, AplicadorConsecuenciaEventoGlobal.class,
            ServicioEconomiaImpl.class, ServicioMovimientoImpl.class, ServicioGestionQuiebraImpl.class,
            ServicioPropiedadImpl.class, ServicioCasilleroImpl.class, ServicioFinalizacionPartidaImpl.class,
            ServicioJugadaImpl.class, ServicioEventoCasilleroImpl.class,
            RepositorioPartidaImpl.class, RepositorioEventoGlobalImpl.class, RepositorioDeudaPendienteImpl.class,
            RepositorioPropiedadImpl.class, RepositorioCasilleroImpl.class, RepositorioFinalizarPartidaImpl.class,
            RepositorioEventoCasilleroImpl.class,
            ObservadorTurnoQuiebra.class, ObservadorTurnoAbandono.class, ObservadorVictoria.class})
    static class Config extends HibernateInfraestructuraTestConfig {
        @Bean @Override public DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:hsqldb:mem:eventos_integracion", "sa", "");
        }
        @Bean ServicioDado dados() { return mock(ServicioDado.class); }
        @Bean ServicioInicioPartida inicio() { return mock(ServicioInicioPartida.class); }
        @Bean GestorTemporizadorPartida temporizador() { return mock(GestorTemporizadorPartida.class); }
        @Bean SimpMessagingTemplate mensajeria() { return mock(SimpMessagingTemplate.class); }
        @Bean NotificadorPartida notificador(SimpMessagingTemplate mensajeria) { return new NotificadorPartida(mensajeria); }
        @Bean PublicadorEventos<EventoTurnoCambiado> turnos() { return new PublicadorEventos<>(List.of()); }
        @Bean PublicadorEventos<EventoJugadorQuebrado> quiebras(ObservadorTurnoQuiebra turno, ObservadorVictoria victoria) {
            return new PublicadorEventos<>(List.of(turno, evento -> victoria.verificarVictoria(evento.partida())));
        }
        @Bean PublicadorEventos<EventoJugadorAbandonado> abandonos(ObservadorTurnoAbandono turno, ObservadorVictoria victoria) {
            return new PublicadorEventos<>(List.of(turno, victoria));
        }
    }

    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioEvento eventos;
    @Autowired private ServicioPartida partidas;
    @Autowired private ServicioGestionQuiebra quiebras;
    @Autowired private ServicioJugada jugadas;
    @Autowired private ServicioPropiedad propiedades;
    @Autowired private ServicioDado dados;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private SimpMessagingTemplate mensajeria;
    private TransactionTemplate transaccion;
    private String codigo;
    private List<Long> jugadores;
    private RandomGenerator aleatorio;

    @BeforeEach
    void preparar() {
        transaccion = new TransactionTemplate(transacciones);
        codigo = UUID.randomUUID().toString();
        reset(dados, temporizador, mensajeria);
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        aleatorio = mock(RandomGenerator.class);
        Object servicioReal = AopTestUtils.getTargetObject(eventos);
        ReflectionTestUtils.setField(servicioReal, "aleatorio", aleatorio);
        transaccion.executeWithoutResult(estado -> {
            var sesion = sesiones.getCurrentSession();
            if (sesion.createQuery("FROM EventoGlobal", EventoGlobal.class).list().isEmpty()) {
                for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
                    EventoGlobal evento = new EventoGlobal();
                    evento.setNombre(tipo.name());
                    evento.setDescripcion(tipo.name());
                    evento.setTipoEventoGlobal(tipo);
                    evento.setTipoEfecto(TipoEfectoEventoGlobal.MOVER_CASILLEROS);
                    evento.setValor(new int[]{0, -3, 0, 1000, 400, 1}[tipo.ordinal()]);
                    sesion.persist(evento);
                }
            }
            Usuario creador = new Usuario();
            creador.setEmail(codigo + "@test.com");
            creador.setUsername(codigo);
            sesion.persist(creador);
            Partida partida = new Partida();
            partida.setCreador(creador);
            partida.setCodigoUnico(codigo);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(4);
            partida.setIndiceTurnoActual(2);
            for (int indice = 0; indice < 3; indice++) {
                Jugador jugador = new Jugador();
                jugador.setOrden(indice);
                jugador.setApodo("Participante " + indice);
                jugador.setDinero(2000);
                jugador.setPosicionActual(1 + indice * 6);
                jugador.setUltimaRondaConOportunidad(indice == 2 ? 3 : 4);
                partida.getJugadores().add(jugador);
            }
            sesion.persist(partida);
            jugadores = partida.getJugadores().stream().map(Jugador::getId).toList();
        });
    }

    @Test
    void dadoQueUnaInundacionCruzaInicioCuandoSeRecargaElEstadoDaComoResultadoPosicionYTiradasPersistidas() {
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        assertEquals(28, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        Map<String, Object> recargado = eventos.consultar(codigo, jugadores.get(1));
        assertEquals(3, ((Map<?, ?>) recargado.get("tiradas")).size());
        assertEquals(true, recargado.get("resuelto"));
        assertThrows(IllegalStateException.class, () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        confirmarTodos();
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertEquals(28, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
    }

    @Test
    void dadoQueHayUnEventoActivoCuandoSeIntentaComprarDaComoResultadoUnaAccionRechazadaSinCambiosEconomicos() {
        iniciar(TipoEventoGlobal.FIAT_600);
        assertThrows(com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException.class,
                () -> propiedades.prepararCompra(codigo, 1));
        assertEquals(2400, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
    }

    @Test
    void dadoQueHayRoboSinDineroCuandoSeVendeUnaPropiedadDaComoResultadoUnaUnicaCancelacionDeLaDeuda() {
        cambiarDinero(jugadores.get(0), 100);
        Long propiedad = transaccion.execute(estado -> {
            Propiedad terreno = new Propiedad();
            terreno.setNombre("Terreno para pagar el robo");
            terreno.setPrecioCompra(2000);
            terreno.setPrecioAlquiler(100);
            terreno.setPropietario(sesiones.getCurrentSession().find(Jugador.class, jugadores.get(0)));
            sesiones.getCurrentSession().persist(terreno);
            return terreno.getId();
        });
        iniciar(TipoEventoGlobal.ROBO);
        assertEquals("ROBO_EVENTO_GLOBAL", quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)).getMotivo());
        confirmarTodos();
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertTrue(quiebras.venderPropiedadYResolverDeuda(codigo, jugadores.get(0), propiedad));
        assertEquals(100, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        assertFalse(quiebras.intentarResolverDeuda(codigo, jugadores.get(0)));
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueElRobadoQuiebraCuandoLosOtrosConfirmanDaComoResultadoSuExclusionYLaReanudacionCorrecta() {
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        confirmarTodos();
        quiebras.declararQuiebra(codigo, jugadores.get(0));
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEnBancarrota());
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueSarmientoAfectaAlProximoCuandoSeReanudaDaComoResultadoUnTurnoOmitidoYLuegoUnaTiradaPermitida() throws Exception {
        iniciar(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        confirmarTodos();
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        partidas.avanzarTurno(codigo);
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(0), partidas.obtenerJugadorActual(codigo).getId());
        assertFalse(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        assertEquals(6, partidas.obtenerPartida(codigo).getRondaActual());
    }

    @Test
    void dadoQueSarmientoAfectaAlJugadorPendienteCuandoConfirmanDaComoResultadoUnaOportunidadConsumida() {
        iniciar(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        confirmarTodos();
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertEquals(5, partidas.obtenerJugadorPorId(jugadores.get(0)).getUltimaRondaConOportunidad());
        assertFalse(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(2), partidas.obtenerJugadorActual(codigo).getId());
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(0), partidas.obtenerJugadorActual(codigo).getId());
    }

    @Test
    void dadoQueQuedaUnaPenalizacionPendienteCuandoSeIntentaTirarDaComoResultadoAccionRechazada() {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = sesiones.getCurrentSession().createQuery(
                    "FROM Partida WHERE codigoUnico = :codigo", Partida.class)
                    .setParameter("codigo", codigo).getSingleResult();
            partida.setIndiceTurnoActual(0);
            partida.getJugadores().get(0).setPierdeTurno(true);
        });
        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        verify(dados, never()).tirarDados();
    }

    @Test
    void dadoQueLaQuiebraDelRobadoDejaUnSoloActivoCuandoFinalizaLaPartidaDaComoResultadoLaLimpiezaDelEvento() throws Exception {
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        partidas.abandonarPartida(codigo, jugadores.get(2));
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        quiebras.declararQuiebra(codigo, jugadores.get(0));
        Partida finalizada = partidas.obtenerPartidaParaVisualizacion(codigo);
        assertEquals(EstadoPartida.FINALIZADA, finalizada.getEstado());
        assertFalse(finalizada.getEventoGlobal().estaActivo());
        verify(temporizador, never()).programarTemporizador(eq(codigo), anyInt(), any());
        verify(temporizador, atLeastOnce()).cancelarTemporizadorEvento(eq("evento:" + codigo), anyLong());
    }

    @Test
    void dadoQueUnJugadorAbandonaSinTirarCuandoLosRestantesTiranDaComoResultadoQueNoSeLoEspera() throws Exception {
        iniciar(TipoEventoGlobal.INUNDACION);
        partidas.abandonarPartida(codigo, jugadores.get(2));
        eventos.tirar(codigo, jugadores.get(0), 5, 1);
        eventos.tirar(codigo, jugadores.get(1), 5, 1);
        assertEquals(true, eventos.consultar(codigo, jugadores.get(0)).get("resuelto"));
        assertThrows(SecurityException.class, () -> eventos.tirar(codigo, jugadores.get(2), 5, 1));
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueDosPeticionesRepitenUnaTiradaCuandoSeEjecutanSimultaneamenteDaComoResultadoUnSoloRegistro() throws Exception {
        iniciar(TipoEventoGlobal.INUNDACION);
        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Boolean> tirar = () -> {
                inicio.await();
                try { eventos.tirar(codigo, jugadores.get(0), 5, 1); return true; }
                catch (IllegalStateException esperado) { return false; }
            };
            Future<Boolean> primera = ejecutor.submit(tirar);
            Future<Boolean> segunda = ejecutor.submit(tirar);
            inicio.countDown();
            assertNotEquals(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, ((Map<?, ?>) eventos.consultar(codigo, jugadores.get(0)).get("tiradas")).size());
        verify(dados, times(1)).tirarDados();
    }

    @Test
    void dadoQueLaActivacionSeRevierteCuandoSeDeshaceLaTransaccionDaComoResultadoNingunEfectoNiNotificacion() {
        when(aleatorio.nextInt(6)).thenReturn(TipoEventoGlobal.FIAT_600.ordinal());
        transaccion.executeWithoutResult(estado -> {
            partidas.avanzarTurno(codigo);
            verifyNoInteractions(mensajeria, temporizador);
            estado.setRollbackOnly();
        });
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        verifyNoInteractions(mensajeria, temporizador);
    }

    @Test
    void dadoQueSeDuplicaLaUltimaConfirmacionCuandoSeProcesaEnParaleloDaComoResultadoUnaSolaReanudacion() throws Exception {
        iniciar(TipoEventoGlobal.FIAT_600);
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Map<String, Object>> confirmar = () -> {
                inicio.await();
                return eventos.confirmar(codigo, jugadores.get(2), 5);
            };
            var primera = ejecutor.submit(confirmar);
            var segunda = ejecutor.submit(confirmar);
            inicio.countDown();
            assertEquals(false, primera.get(10, TimeUnit.SECONDS).get("activo"));
            assertEquals(false, segunda.get(10, TimeUnit.SECONDS).get("activo"));
        }
        assertEquals(2400, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        verify(temporizador, times(1)).programarTemporizador(eq(codigo), anyInt(), any());
    }

    @Test
    void dadoQueAlguienIntentaUsarOtroIdCuandoTiraDaComoResultadoQueSoloCuentaLaSesionAutenticada() {
        iniciar(TipoEventoGlobal.INUNDACION);
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("id", 99999L);
        solicitud.getSession().setAttribute("NOMBRE", "Participante 0");
        solicitud.addParameter("idJugador", jugadores.get(1).toString());
        controlador.tirar(codigo, 5, 1, solicitud);
        var tiradas = partidas.obtenerPartida(codigo).getEventoGlobal().getTiradas();
        assertTrue(tiradas.containsKey("1:" + jugadores.get(0)));
        assertFalse(tiradas.containsKey("1:" + jugadores.get(1)));
        solicitud.getSession().setAttribute("NOMBRE", "Intruso");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controlador.consultar(codigo, solicitud));
    }

    private void iniciar(TipoEventoGlobal tipo) {
        when(aleatorio.nextInt(6)).thenReturn(tipo.ordinal());
        partidas.avanzarTurno(codigo);
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertEquals(tipo, partidas.obtenerPartida(codigo).getEventoGlobal().getEvento().getTipoEventoGlobal());
        if (!tipo.requiereDados()) {
            assertEquals(jugadores.get(0), partidas.obtenerPartida(codigo).getEventoGlobal().getSeleccionado());
        }
    }
    private void tirarTodos() { jugadores.forEach(jugador -> eventos.tirar(codigo, jugador, 5, 1)); }
    private void confirmarTodos() { jugadores.forEach(jugador -> eventos.confirmar(codigo, jugador, 5)); }
    private void cambiarDinero(Long jugador, int dinero) {
        transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession().find(Jugador.class, jugador).setDinero(dinero));
    }
}
