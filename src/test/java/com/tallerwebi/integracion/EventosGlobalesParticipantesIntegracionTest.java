package com.tallerwebi.integracion;

import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.FaseEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.observadores.Observador;
import com.tallerwebi.dominio.observadores.ObservadorReanudarPartida;
import com.tallerwebi.dominio.observadores.ObservadorTurnoAbandono;
import com.tallerwebi.dominio.observadores.ObservadorVictoria;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioCasilleroImpl;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioEventoImpl;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl;
import com.tallerwebi.dominio.servicios.ServicioJugadaImpl;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import com.tallerwebi.dominio.servicios.ServicioOfertaPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioDeudaPendienteImpl;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPropiedadImpl;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.presentacion.ControladorPartida;
import com.tallerwebi.presentacion.ControladorPropiedadesPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorEventoGlobalWebSocket;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Usa Hibernate y los servicios reales; el catálogo, los dados y el reloj se controlan en la prueba. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EventosGlobalesParticipantesIntegracionTest.Config.class)
public class EventosGlobalesParticipantesIntegracionTest {
    @Configuration
    @Import({com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl.class,
            com.tallerwebi.infraestructura.RepositorioFinalizarPartidaImpl.class, ServicioEventoImpl.class, ServicioPartidaImpl.class, ServicioTurnoImpl.class,
            ServicioParticipacionImpl.class, ServicioGestionQuiebraImpl.class, ServicioEconomiaImpl.class,
            ServicioPropiedadImpl.class, ServicioCasilleroImpl.class, ServicioOfertaPropiedadImpl.class,
            ServicioJugadaImpl.class, ServicioMovimientoImpl.class,
            RepositorioPartidaImpl.class, RepositorioPropiedadImpl.class, RepositorioCasilleroImpl.class,
            RepositorioDeudaPendienteImpl.class, ObservadorReanudarPartida.class,
            ObservadorTurnoAbandono.class, ObservadorVictoria.class,
            com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra.class,
            com.tallerwebi.dominio.observadores.ObservadorVictoriaQuiebra.class,
            ObservadorEventoGlobalWebSocket.class, NotificadorPartida.class,
            ControladorPartida.class, ControladorEventoGlobal.class, ControladorPropiedadesPartida.class})
    static class Config extends HibernateTestConfig {
        @Bean @Override public DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:hsqldb:mem:eventos_participantes", "sa", "");
        }
        @Bean public RepositorioEvento catalogo() { return mock(RepositorioEvento.class); }
        @Bean public ServicioDado dados() { return mock(ServicioDado.class); }
        @Bean public ServicioInicioPartida inicio() { return mock(ServicioInicioPartida.class); }
        @Bean public ServicioEventoCasillero eventosCasillero() { return mock(ServicioEventoCasillero.class); }
        @Bean public GestorTemporizadorPartida temporizador() { return mock(GestorTemporizadorPartida.class); }
        @Bean public SimpMessagingTemplate mensajeria() { return mock(SimpMessagingTemplate.class); }
        @Bean public PublicadorEventos<EventoJugadorAbandonado> abandonos(
                List<Observador<EventoJugadorAbandonado>> observadores) {
            return new PublicadorEventos<>(observadores);
        }
        @Bean public PublicadorEventos<EventoJugadorQuebrado> quiebras(
                List<Observador<EventoJugadorQuebrado>> observadores) {
            return new PublicadorEventos<>(observadores);
        }
        @Bean public java.util.function.Consumer<EventoJugadorQuebrado> notificacionQuiebra(NotificadorPartida notificador) {
            return evento -> notificador.notificarQuiebraPorWebSocket(evento.partida(), evento.jugador().getId());
        }
        @Bean public PublicadorEventos<EventoTurnoCambiado> turnos(NotificadorPartida notificador) {
            return new PublicadorEventos<>(List.of(new ObservadorTurnoWebSocket(notificador)));
        }
    }

    @Autowired private com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida finales;
    @Autowired private ServicioEvento eventos;
    @Autowired private ServicioPartida partidas;
    @Autowired private RepositorioPartida repositorio;
    @Autowired private RepositorioEvento catalogo;
    @Autowired private ServicioDado dados;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private SimpMessagingTemplate mensajeria;
    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager gestorTransacciones;
    @Autowired private ControladorPartida controlador;
    @Autowired private ControladorPropiedadesPartida controladorOfertas;
    @Autowired private ServicioOfertaPropiedad ofertas;
    @Autowired private ServicioGestionQuiebra quiebras;
    @Autowired private PublicadorEventos<EventoJugadorAbandonado> abandonos;
    private TransactionTemplate transaccion;
    private Partida partida;
    private final Map<String, Runnable> vencimientos = new HashMap<>();

    @BeforeEach
    public void preparar() {
        transaccion = new TransactionTemplate(gestorTransacciones);
        reset(catalogo, dados, temporizador, mensajeria);
        vencimientos.clear();
        doAnswer(invocacion -> {
            vencimientos.put(invocacion.getArgument(0), invocacion.getArgument(2));
            return null;
        }).when(temporizador).programarTemporizador(anyString(), anyInt(), any(Runnable.class));
        when(temporizador.programarTemporizadorSiNoExiste(anyString(), eq(30), any())).thenAnswer(invocacion -> {
            vencimientos.put(invocacion.getArgument(0), invocacion.getArgument(2));
            return true;
        });
        when(temporizador.obtenerSegundosRestantes(anyString())).thenReturn(30);
        when(temporizador.obtenerSegundosRestantes(startsWith("evento-global-empate:"))).thenReturn(8);
    }

    private void dadaUnaPartida(Integer cantidad) {
        partida = transaccion.execute(estado -> {
            String codigo = UUID.randomUUID().toString();
            Usuario creador = new Usuario();
            creador.setUsername(codigo);
            creador.setEmail(codigo + "@test.local");
            sesiones.getCurrentSession().persist(creador);
            Partida nueva = new Partida();
            nueva.setCodigoUnico(codigo);
            nueva.setCreador(creador);
            nueva.setEstado(EstadoPartida.EN_CURSO);
            nueva.setRondaActual(10);
            List<Jugador> jugadores = new ArrayList<>();
            for (Integer orden = 0; orden < cantidad; orden++) {
                Jugador jugador = new Jugador();
                jugador.setApodo(orden == 0 ? "prueba_local" : orden == cantidad - 1 ? "test123" : "Jugador" + orden);
                jugador.setOrden(orden);
                jugador.setPosicionActual(12);
                jugador.setDinero(1500);
                jugadores.add(jugador);
            }
            nueva.setJugadores(jugadores);
            repositorio.guardarPartida(nueva);
            return nueva;
        });
    }

    private EventoGlobal evento(TipoEventoGlobal tipo) {
        EventoGlobal evento = new EventoGlobal();
        evento.setId(tipo.ordinal() + 1);
        evento.setNombre(tipo.name());
        evento.setDescripcion("Evento de prueba " + tipo);
        evento.setTipoEventoGlobal(tipo);
        evento.setTipoEfecto(switch (tipo) {
            case ACCIDENTE_SARMIENTO -> TipoEfecto.PERDER_TURNO;
            case FIAT_600 -> TipoEfecto.GANAR_PLATA;
            case ROBO -> TipoEfecto.PERDER_PLATA;
            case PIQUETE -> TipoEfecto.VOLVER_INICIO;
            default -> TipoEfecto.MOVER_CASILLEROS;
        });
        evento.setValor(switch (tipo) {
            case ACCIDENTE_SARMIENTO -> 1;
            case FIAT_600 -> 400;
            case ROBO -> 1000;
            case INUNDACION -> -3;
            default -> 0;
        });
        return evento;
    }

    private String codigo() { return partida.getCodigoUnico(); }
    private Long id(Integer orden) { return partida.getJugadores().get(orden).getId(); }
    private Jugador jugador(Integer orden) { return transaccion.execute(estado -> repositorio.buscarJugadorPorId(id(orden))); }
    private EstadoEventoGlobal estado() { return eventos.consultarEstadoEventoGlobal(codigo()); }
    private void cuandoSeActiva(TipoEventoGlobal tipo) throws Exception {
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(tipo));
        eventos.verificarActivacionEventoGlobal(partidas.obtenerPartida(codigo()));
    }

    // La semilla sólo fija el seleccionado del escenario; la selección y la consecuencia son reales.
    private void dadoQueSeraSeleccionado(Integer orden) {
        List<Jugador> jugadores = partidas.obtenerPartida(codigo()).getJugadores();
        Integer indice = 0;
        while (!jugadores.get(indice).getId().equals(id(orden))) indice++;
        long semilla = 0;
        while (new Random(semilla).nextInt(jugadores.size()) != indice) semilla++;
        Object servicioReal = AopTestUtils.getTargetObject(eventos); //ke
        Random aleatorio = (Random) ReflectionTestUtils.getField(servicioReal, "random");
        assertNotNull(aleatorio);
        aleatorio.setSeed(semilla);
    }

    private void cuandoTiran(int... sumas) throws Exception {
        ResultadoTirada[] resultados = Arrays.stream(sumas)
                .mapToObj(suma -> new ResultadoTirada(suma / 2, suma - suma / 2)).toArray(ResultadoTirada[]::new);
        when(dados.tirarDados()).thenReturn(resultados[0], Arrays.copyOfRange(resultados, 1, resultados.length));
        for (Long participante : estado().participantes()) eventos.tirarDadosEventoGlobal(codigo(), participante);
    }

    private void cuandoConfirmanLosActivos() {
        for (Jugador jugador : partidas.obtenerPartida(codigo()).getJugadores()) {
            if (!jugador.getAbandono() && !jugador.getEnBancarrota()) eventos.confirmarContinuacion(codigo(), jugador.getId());
        }
    }

    private RespuestaMovimientoDTO cuandoTiraNormal(int orden) throws Exception {
        return cuandoTiraNormal(orden, new ResultadoTirada(4, 4));
    }

    private RespuestaMovimientoDTO cuandoTiraNormal(int orden, ResultadoTirada tirada) throws Exception {
        when(dados.tirarDados()).thenReturn(tirada);
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", jugador(orden).getApodo());
        return controlador.tirarDados(codigo(), id(orden), solicitud);
    }

    @Test
    public void dadoQueTest123TerminoAntesDeSarmientoEnDiezCuandoSeReanudaEntoncesRepiteSuTurnoYDespuesPuedeJugarPruebaLocal() throws Exception {
        dadaUnaPartida(2);
        transaccion.executeWithoutResult(estado -> {
            Partida actual = repositorio.buscarPartidaActivaPorCodigoUnico(codigo());
            actual.setRondaActual(9);
            actual.setTurnosFinalizadosEnRonda(1);
            actual.setIndiceTurnoActual(1);
                    dadasLasOportunidadesAnteriores(actual);
        });
        dadoQueSeraSeleccionado(0);
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.ACCIDENTE_SARMIENTO));

        cuandoTiraNormal(1);

        assertEquals(10, estado().rondaActivacion());
        assertEquals(id(0), estado().idJugadorElegido());
        assertEquals(id(1), partidas.obtenerJugadorActual(codigo()).getId());
        assertFalse(jugador(0).getPierdeTurno());
        assertThrows(ResponseStatusException.class, () -> cuandoTiraNormal(0));
        cuandoConfirmanLosActivos();
        assertEquals(id(1), partidas.obtenerJugadorActual(codigo()).getId());
        assertThrows(ResponseStatusException.class, () -> cuandoTiraNormal(0));
        RespuestaMovimientoDTO siguiente = cuandoTiraNormal(1);
        assertEquals(id(0), siguiente.getTurnoSiguiente().getIdJugadorActual());
        assertFalse(jugador(0).getPierdeTurno());
        cuandoTiraNormal(0);
        assertEquals(20, jugador(0).getPosicionActual());
        verify(dados, times(3)).tirarDados();
    }

    @Test
    public void dadoQueElSeleccionadoAbandonaDuranteLasConfirmacionesCuandoReanudanEntoncesNoRecibeMovimientoYHayOtroTurnoValido() throws Exception {
        for (Integer cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.INUNDACION, TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.PIQUETE)) {
                dadaUnaPartida(cantidad);
                cuandoSeActiva(tipo);
                cuandoTiran(cantidad == 3 ? new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6, 8}
                        : new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6, 8, 7});
                assertEquals(id(0), estado().idJugadorElegido());

                partidas.abandonarPartida(codigo(), id(0));
                cuandoConfirmanLosActivos();

                assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
                assertEquals(12, jugador(0).getPosicionActual());
                assertNotNull(partidas.obtenerJugadorActual(codigo()));
                assertEquals(id(1), partidas.obtenerJugadorActual(codigo()).getId());
                assertEquals(EstadoPartida.EN_CURSO, partidas.obtenerPartida(codigo()).getEstado());
                for (Integer orden = 1; orden < cantidad; orden++) assertEquals(12, jugador(orden).getPosicionActual());
            }
        }
    }

    @Test
    public void dadoQueTodosLosEmpatadosAbandonanCuandoQuedanDosActivosEntoncesFinalizaElEventoSinReasignarConsecuencia() throws Exception {
        dadaUnaPartida(4);
        cuandoSeActiva(TipoEventoGlobal.TRAMITES_AFIP);
        cuandoTiran(10, 10, 3, 4);
        assertEquals(FaseEventoGlobal.EMPATE, estado().fase());
        transaccion.executeWithoutResult(estado -> vencimientos.get("evento-global-empate:" + codigo()).run());
        assertEquals(FaseEventoGlobal.DESEMPATE, estado().fase());

        partidas.abandonarPartida(codigo(), id(0));
        partidas.abandonarPartida(codigo(), id(1));

        assertEquals(FaseEventoGlobal.FINALIZADO, estado().fase());
        assertNull(estado().idJugadorElegido());
        assertEquals(List.of(id(2), id(3)), estado().participantes());
        cuandoConfirmanLosActivos();
        assertNull(estado());
        assertNotNull(partidas.obtenerJugadorActual(codigo()));
        for (Integer orden = 0; orden < 4; orden++) assertEquals(12, jugador(orden).getPosicionActual());
    }

    @Test
    public void dadoQueUnAbandonoDejaUnSoloActivoCuandoFinalizaLaPartidaEntoncesCancelaElEventoYSusTemporizadores() throws Exception {
        for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.ACCIDENTE_SARMIENTO)) {
            dadaUnaPartida(2);
            cuandoSeActiva(tipo);
            List<Runnable> relojesAnteriores = new ArrayList<>(vencimientos.values());

            partidas.abandonarPartida(codigo(), id(1));

            assertEquals(EstadoPartida.FINALIZADA, transaccion.execute(estado -> sesiones.getCurrentSession()
                    .get(Partida.class, partida.getId()).getEstado()));
            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            for (Runnable reloj : relojesAnteriores) assertDoesNotThrow(() -> transaccion.executeWithoutResult(estado -> reloj.run()));
            verify(temporizador, never()).programarTemporizadorSiNoExiste(eq(codigo()), anyInt(), any());
        }
    }

    @Test
    public void dadosTresYCuatroJugadoresCuandoSarmientoAfectaCadaOrdenEntoncesPierdeUnaOportunidadYVuelveAJugar() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (int afectado = 0; afectado < cantidad; afectado++) {
                dadaUnaPartida(cantidad);
                transaccion.executeWithoutResult(estado -> {
                    Partida actual = repositorio.buscarPartidaActivaPorCodigoUnico(codigo());
                    actual.setRondaActual(9);
                    actual.setTurnosFinalizadosEnRonda(cantidad - 1);
                    actual.setIndiceTurnoActual(cantidad - 1);
                            dadasLasOportunidadesAnteriores(actual);
        });
                dadoQueSeraSeleccionado(afectado);
                when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.ACCIDENTE_SARMIENTO));
                partidas.iniciarTemporizadorTurnoSiNoExiste(codigo());
                Runnable anterior = vencimientos.get(codigo());
                assertNotNull(anterior);
                cuandoTiraNormal(cantidad - 1);
                assertEquals(id(afectado), estado().idJugadorElegido());
                cuandoConfirmanLosActivos();
                int esperado = afectado == 0 ? 1 : 0;
                assertEquals(id(esperado), partidas.obtenerJugadorActual(codigo()).getId());
                int oportunidades = cantidad - 1 + afectado;
                for (int paso = 0; paso < oportunidades; paso++) {
                    assertNotEquals(id(afectado), partidas.obtenerJugadorActual(codigo()).getId());
                    final int penalizado = afectado;
                    assertThrows(ResponseStatusException.class, () -> cuandoTiraNormal(penalizado));
                    transaccion.executeWithoutResult(estado -> anterior.run());
                    assertEquals(id(esperado), partidas.obtenerJugadorActual(codigo()).getId());
                    cuandoTiraNormal(esperado);
                    esperado = (esperado + 1) % cantidad;
                    if (paso < afectado && esperado == afectado) esperado = (esperado + 1) % cantidad;
                }
                assertEquals(id(afectado), partidas.obtenerJugadorActual(codigo()).getId());
                assertFalse(jugador(afectado).getPierdeTurno());
                assertFalse(jugador(afectado).getTiroEnTurno());
                cuandoTiraNormal(afectado);
                assertEquals(28, jugador(cantidad - 1).getPosicionActual());
            }
        }
    }

    @Test
    public void dadosDosTresYCuatroActivosCuandoResuelvenLosSeisEventosEntoncesPersistenLasConsecuenciasUnaVez() throws Exception {
        for (int cantidad : List.of(2, 3, 4)) {
            for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
                dadaUnaPartida(cantidad);
                dadoQueSeraSeleccionado(0);
                transaccion.executeWithoutResult(estado -> repositorio.buscarJugadorPorId(id(cantidad - 1)).setPosicionActual(25));
                cuandoSeActiva(tipo);
                if (esEventoConDados(tipo)) cuandoTiran(cantidad == 2 ? new int[]{12, 3}
                        : cantidad == 3 ? new int[]{12, 3, 6} : new int[]{12, 3, 6, 8});
                EstadoEventoGlobal resultado = estado();
                int elegido = tipo == TipoEventoGlobal.PIQUETE ? 1 : 0;
                assertEquals(cantidad, resultado.participantes().size());
                assertEquals(id(elegido), resultado.idJugadorElegido());
                assertEquals(esEventoConDados(tipo) ? cantidad : 0, resultado.tiradas().size());
                assertEquals(tipo == TipoEventoGlobal.FIAT_600 ? 1900 : tipo == TipoEventoGlobal.ROBO ? 500 : 1500,
                        jugador(0).getDinero());
                if (tipo == TipoEventoGlobal.ACCIDENTE_SARMIENTO) assertTrue(jugador(0).getPierdeTurno());
                for (int orden = 0; orden < cantidad; orden++) {
                    eventos.confirmarContinuacion(codigo(), id(orden));
                    if (orden < cantidad - 1) {
                        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));
                        assertEquals(orden + 1, estado().confirmados().size());
                        assertEquals(orden + 1, java.util.Set.copyOf(estado().confirmados()).size());
                        if (esEventoConDados(tipo)) assertEquals(elegido == cantidad - 1 ? 25 : 12, jugador(elegido).getPosicionActual());
                    }
                }
                assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
                assertNull(estado());
                int destino = switch (tipo) {
                    case INUNDACION -> 9;
                    case PIQUETE -> 0;
                    case TRAMITES_AFIP -> 25;
                    default -> 12;
                };
                assertEquals(destino, jugador(elegido).getPosicionActual());
                assertEquals(tipo == TipoEventoGlobal.FIAT_600 ? 1900 : tipo == TipoEventoGlobal.ROBO ? 500 : 1500,
                        jugador(0).getDinero());
                assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo(), id(0)));
                assertEquals(destino, jugador(elegido).getPosicionActual());
                assertNotNull(partidas.obtenerJugadorActual(codigo()));
            }
        }
    }

    @Test
    public void dadosTresYCuatroActivosCuandoEmpatanDosOTresYSeRepiteElEmpateEntoncesSoloDesempatanLosHabilitados() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (int empatados : List.of(2, 3)) {
                for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE)) {
                    dadaUnaPartida(cantidad);
                    cuandoSeActiva(tipo);
                    Runnable relojInicial = vencimientos.get("evento-global:" + codigo());
                    int[] sumas = new int[cantidad];
                    for (int orden = 0; orden < cantidad; orden++) sumas[orden] = orden < empatados
                            ? tipo == TipoEventoGlobal.PIQUETE ? 2 : 12 : 6;
                    cuandoTiran(sumas);
                    assertEquals(FaseEventoGlobal.EMPATE, estado().fase());
                    assertEquals(empatados, estado().participantes().size());
                    transaccion.executeWithoutResult(estado -> relojInicial.run());
                    assertEquals(FaseEventoGlobal.EMPATE, estado().fase());
                    transaccion.executeWithoutResult(estado -> vencimientos.get("evento-global-empate:" + codigo()).run());
                    assertEquals(FaseEventoGlobal.DESEMPATE, estado().fase());
                    if (empatados < cantidad) {
                        assertThrows(com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException.class,
                                () -> eventos.tirarDadosEventoGlobal(codigo(), id(cantidad - 1)));
                    }
                    cuandoTiran(empatados == 2 ? new int[]{6, 6} : new int[]{6, 6, 6});
                    assertEquals(FaseEventoGlobal.EMPATE, estado().fase());
                    assertEquals(2, estado().etapa());
                    transaccion.executeWithoutResult(estado -> vencimientos.get("evento-global-empate:" + codigo()).run());
                    cuandoTiran(empatados == 2 ? new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6}
                            : new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6, 8});
                    assertEquals(FaseEventoGlobal.FINALIZADO, estado().fase());
                    assertEquals(id(0), estado().idJugadorElegido());
                    cuandoConfirmanLosActivos();
                    assertNull(estado());
                    assertNotNull(partidas.obtenerJugadorActual(codigo()));
                }
            }
        }
    }

    @Test
    public void dadosTresYCuatroActivosCuandoVenceLaTiradaEntoncesElServidorCompletaSoloLosDadosPendientes() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE)) {
                dadaUnaPartida(cantidad);
                cuandoSeActiva(tipo);
                Runnable reloj = vencimientos.get("evento-global:" + codigo());
                when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 1), new ResultadoTirada(3, 4),
                        new ResultadoTirada(4, 5), new ResultadoTirada(5, 6));
                clearInvocations(dados);
                eventos.tirarDadosEventoGlobal(codigo(), id(0));

                transaccion.executeWithoutResult(estado -> reloj.run());
                transaccion.executeWithoutResult(estado -> reloj.run());

                assertEquals(FaseEventoGlobal.FINALIZADO, estado().fase());
                assertEquals(cantidad, estado().tiradas().size());
                assertEquals(2, estado().tiradas().get(id(0)));
                assertEquals(tipo == TipoEventoGlobal.PIQUETE ? id(0) : id(cantidad - 1), estado().idJugadorElegido());
                verify(dados, times(cantidad)).tirarDados();
                verify(temporizador).programarTemporizador(eq("evento-global:" + codigo()), eq(30), any());
                cuandoConfirmanLosActivos();
                assertNull(estado());
            }
        }
    }

    @Test
    public void dadoQueUnJugadorAbandonoAntesCuandoSeActivanLosSeisEventosEntoncesNoParticipaNiEsSeleccionado() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
                dadaUnaPartida(cantidad);
                partidas.abandonarPartida(codigo(), id(cantidad - 1));
                cuandoSeActiva(tipo);
                assertFalse(estado().participantes().contains(id(cantidad - 1)));
                if (esEventoConDados(tipo)) cuandoTiran(cantidad == 3 ? new int[]{12, 3} : new int[]{12, 3, 6});
                assertNotEquals(id(cantidad - 1), estado().idJugadorElegido());
                assertEquals(cantidad - 1, estado().participantes().size());
                assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo(), id(cantidad - 1)));
                cuandoConfirmanLosActivos();
                assertNull(estado());
                assertEquals(1500, jugador(cantidad - 1).getDinero());
                assertEquals(12, jugador(cantidad - 1).getPosicionActual());
                assertFalse(jugador(cantidad - 1).getPierdeTurno());
            }
        }
    }

    @Test
    public void dadoQueAbandonaQuienNoTiroCuandoLosDemasYaTiraronEntoncesResuelveSinEsperarSusDados() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE)) {
                dadaUnaPartida(cantidad);
                cuandoSeActiva(tipo);
                clearInvocations(dados);
                when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 1), new ResultadoTirada(3, 4), new ResultadoTirada(5, 6));
                for (int orden = 0; orden < cantidad - 1; orden++) eventos.tirarDadosEventoGlobal(codigo(), id(orden));

                partidas.abandonarPartida(codigo(), id(cantidad - 1));

                assertEquals(FaseEventoGlobal.FINALIZADO, estado().fase());
                assertEquals(cantidad - 1, estado().tiradas().size());
                assertFalse(estado().participantes().contains(id(cantidad - 1)));
                verify(dados, times(cantidad - 1)).tirarDados();
                cuandoConfirmanLosActivos();
                assertNull(estado());
            }
        }
    }

    @Test
    public void dadoQueUnEmpatadoAbandonaCuandoLosRestantesDesempatanEntoncesNoConservaTiradasPendientesDelAbandonado() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            dadaUnaPartida(cantidad);
            cuandoSeActiva(TipoEventoGlobal.INUNDACION);
            cuandoTiran(cantidad == 3 ? new int[]{10, 10, 3} : new int[]{10, 10, 10, 3});
            transaccion.executeWithoutResult(estado -> vencimientos.get("evento-global-empate:" + codigo()).run());

            partidas.abandonarPartida(codigo(), id(1));

            assertFalse(estado().participantes().contains(id(1)));
            assertThrows(com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException.class,
                    () -> eventos.tirarDadosEventoGlobal(codigo(), id(1)));
            cuandoTiran(cantidad == 3 ? new int[]{12} : new int[]{12, 8});
            assertEquals(FaseEventoGlobal.FINALIZADO, estado().fase());
            assertEquals(id(0), estado().idJugadorElegido());
            assertFalse(estado().tiradas().containsKey(id(1)));
            cuandoConfirmanLosActivos();
            assertEquals(9, jugador(0).getPosicionActual());
            assertNull(estado());
        }
    }

    @Test
    public void dadoQueSoloFaltaConfirmarAlQueAbandonaCuandoLoNotificaEntoncesReanudaSinEsperarlo() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            dadaUnaPartida(cantidad);
            dadoQueSeraSeleccionado(0);
            cuandoSeActiva(TipoEventoGlobal.FIAT_600);
            for (int orden = 0; orden < cantidad - 1; orden++) eventos.confirmarContinuacion(codigo(), id(orden));
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));

            partidas.abandonarPartida(codigo(), id(cantidad - 1));

            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            assertNull(estado());
            assertEquals(1900, jugador(0).getDinero());
            assertNotNull(partidas.obtenerJugadorActual(codigo()));
            verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo() + "/evento-global/reanudado"), any(Object.class));
        }
    }

    @Test
    public void dadoQueElElegidoYaRecibioDineroOSarmientoCuandoAbandonaEntoncesNoDeshaceNiTransfiereLaConsecuencia() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.FIAT_600, TipoEventoGlobal.ROBO, TipoEventoGlobal.ACCIDENTE_SARMIENTO)) {
                dadaUnaPartida(cantidad);
                dadoQueSeraSeleccionado(0);
                cuandoSeActiva(tipo);
                int saldoEsperado = tipo == TipoEventoGlobal.FIAT_600 ? 1900 : tipo == TipoEventoGlobal.ROBO ? 500 : 1500;

                partidas.abandonarPartida(codigo(), id(0));

                assertTrue(estado().detalle().contains("después de registrarse la consecuencia"));
                assertEquals(saldoEsperado, jugador(0).getDinero());
                for (int orden = 1; orden < cantidad; orden++) {
                    assertEquals(1500, jugador(orden).getDinero());
                    assertFalse(jugador(orden).getPierdeTurno());
                }
                cuandoConfirmanLosActivos();
                assertNull(estado());
                assertNotNull(partidas.obtenerJugadorActual(codigo()));
                assertEquals(saldoEsperado, jugador(0).getDinero());
            }
        }
    }

    @Test
    public void dadosTresYCuatroActivosCuandoRoboGeneraDeudaYVendenEntoncesPersisteElCobroSinDuplicarlo() throws Exception {
        for (int cantidad : List.of(3, 4)) {
            dadaUnaPartida(cantidad);
            transaccion.executeWithoutResult(estado -> {
                Jugador vendedor = repositorio.buscarJugadorPorId(id(0));
                vendedor.setDinero(300);
                Propiedad propiedad = new Propiedad(null, "Propiedad para resolver robo", 2000, 50, vendedor, null);
                CasilleroPropiedad casillero = new CasilleroPropiedad(null, 1, propiedad.getNombre(), "Prueba", propiedad);
                casillero.setPartidaId(partida.getId());
                sesiones.getCurrentSession().persist(casillero);
            });
            dadoQueSeraSeleccionado(0);
            cuandoSeActiva(TipoEventoGlobal.ROBO);
            assertEquals(id(0), estado().idJugadorElegido());
            assertEquals(1000, quiebras.obtenerDeudaPendiente(codigo(), id(0)).getMontoPendiente());
            cuandoConfirmanLosActivos();
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            String oferta = transaccion.execute(estado -> ofertas.crearOferta(partidas.obtenerPartida(codigo()), 1,
                    id(0), id(1), 700).id());
            MockHttpServletRequest comprador = new MockHttpServletRequest();
            comprador.getSession().setAttribute("NOMBRE", jugador(1).getApodo());

            transaccion.executeWithoutResult(estado -> assertEquals(200, controladorOfertas.responderOferta(codigo(),
                    oferta, Map.of("aceptar", true), comprador).getStatusCode().value()));

            assertEquals(0, jugador(0).getDinero());
            assertEquals(800, jugador(1).getDinero());
            assertNull(quiebras.obtenerDeudaPendiente(codigo(), id(0)));
            assertEquals(id(1), transaccion.execute(estado -> sesiones.getCurrentSession()
                    .createQuery("FROM CasilleroPropiedad WHERE partidaId = :partida AND posicion = 1", CasilleroPropiedad.class)
                    .setParameter("partida", partida.getId()).uniqueResult().getPropiedad().getPropietario().getId()));
            // La consulta autoriza la reanudación tras cobrar; la reserva se libera al confirmar esa transacción.
            eventos.consultarEstadoEventoGlobal(codigo());
            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            assertNull(estado());
            transaccion.executeWithoutResult(estado -> assertEquals(409, controladorOfertas.responderOferta(codigo(),
                    oferta, Map.of("aceptar", true), comprador).getStatusCode().value()));
            assertEquals(0, jugador(0).getDinero());
            assertEquals(800, jugador(1).getDinero());
        }
    }

    @Test
    public void dadosTresYCuatroActivosCuandoVencenSesentaSegundosEntoncesReanudanYAplicanCadaMovimientoUnaVez() throws Exception {
        List<Partida> preparadas = new ArrayList<>();
        Map<String, TipoEventoGlobal> tipos = new HashMap<>();
        Map<String, Runnable> relojes = new HashMap<>();
        for (int cantidad : List.of(3, 4)) {
            for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE)) {
                dadaUnaPartida(cantidad);
                transaccion.executeWithoutResult(estado -> repositorio.buscarJugadorPorId(id(cantidad - 1)).setPosicionActual(25));
                cuandoSeActiva(tipo);
                cuandoTiran(cantidad == 3 ? new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6, 8}
                        : new int[]{tipo == TipoEventoGlobal.PIQUETE ? 2 : 12, 6, 8, 7});
                eventos.confirmarContinuacion(codigo(), id(0));
                assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));
                assertEquals(12, jugador(0).getPosicionActual());
                verify(temporizador).programarTemporizador(eq("evento-global-resultado:" + codigo()), eq(60), any());
                preparadas.add(partida);
                tipos.put(codigo(), tipo);
                relojes.put(codigo(), vencimientos.get("evento-global-resultado:" + codigo()));
            }
        }
        Thread.sleep(60_050L);

        for (Partida preparada : preparadas) {
            partida = preparada;
            transaccion.executeWithoutResult(estado -> relojes.get(codigo()).run());
            transaccion.executeWithoutResult(estado -> relojes.get(codigo()).run());

            assertNull(estado());
            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            assertEquals(switch (tipos.get(codigo())) {
                case INUNDACION -> 9;
                case PIQUETE -> 0;
                default -> 25;
            }, jugador(0).getPosicionActual());
            assertEquals(1500, jugador(0).getDinero());
            assertNotNull(partidas.obtenerJugadorActual(codigo()));
            verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo() + "/evento-global/reanudado"), any(Object.class));
        }
    }

    private Boolean esEventoConDados(TipoEventoGlobal tipo) {
        return List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE).contains(tipo);
    }

    @Test
    public void dadoQueSeConfiguranLosAbandonosCuandoSeRegistranObservadoresEntoncesIncluyeAlServicioGlobal() {
        assertInstanceOf(Observador.class, eventos);
        Object registrados = ReflectionTestUtils.getField(abandonos, "observadores");
        assertTrue(((java.util.Set<?>) registrados).stream().anyMatch(observador -> observador instanceof ServicioEvento),
                () -> "Observadores de abandono registrados: " + registrados + "; servicio: " + eventos.getClass());
    }

    @Test
    public void dadoQueElAbandonoSeRevierteCuandoQuedabaUnActivoEntoncesConservaLaPartidaYElEventoOriginal() throws Exception {
        for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.FIAT_600)) {
            dadaUnaPartida(2);
            cuandoSeActiva(tipo);
            transaccion.executeWithoutResult(estado -> {
                try { partidas.abandonarPartida(codigo(), id(1)); }
                catch (Exception exception) { throw new IllegalStateException(exception); }
                assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));
                estado.setRollbackOnly();
            });

            assertEquals(EstadoPartida.EN_CURSO, partidas.obtenerPartida(codigo()).getEstado());
            assertFalse(jugador(1).getAbandono());
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            assertEquals(2, estado().participantes().size());
            if (esEventoConDados(tipo)) cuandoTiran(12, 3);
            cuandoConfirmanLosActivos();
            assertNull(estado());
            assertNotNull(partidas.obtenerJugadorActual(codigo()));
        }
    }

    private void dadasLasOportunidadesAnteriores(Partida preparada) {
        if (preparada == null || preparada.getJugadores() == null) return;
        int pendientes = java.util.Objects.requireNonNullElse(preparada.getTurnosFinalizadosEnRonda(), 0);
        int ronda = java.util.Objects.requireNonNullElse(preparada.getRondaActual(), 1);
        int actual = java.util.Objects.requireNonNullElse(preparada.getIndiceTurnoActual(), 0);
        for (Jugador jugador : preparada.getJugadores()) {
            if (pendientes > 0 && jugador.getOrden() != null && jugador.getOrden() != actual
                    && !jugador.getAbandono() && !jugador.getEnBancarrota()) {
                jugador.setUltimaRondaConOportunidad(ronda);
                pendientes--;
            }
        }
    }

    @Test
    public void dadoQueSarmientoPenalizaAPrueba3YTest123AbandonaCuandoPruebaLocalCaeEnCarcelEntoncesPierdeUnaOportunidadYElRelojAnteriorNoAvanza() throws Exception {
        dadaUnaPartida(3);
        transaccion.executeWithoutResult(estado -> {
            Jugador local = repositorio.buscarJugadorPorId(id(0));
            local.setPosicionActual(0);
            repositorio.buscarJugadorPorId(id(1)).setApodo("Prueba3");
        });
        partidas.iniciarTemporizadorTurnoSiNoExiste(codigo());
        Runnable relojAnterior = vencimientos.get(codigo());
        assertNotNull(relojAnterior);
        dadoQueSeraSeleccionado(1);
        cuandoSeActiva(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        assertEquals(id(1), estado().idJugadorElegido());
        assertTrue(jugador(1).getPierdeTurno());

        partidas.abandonarPartida(codigo(), id(2));
        cuandoConfirmanLosActivos();
        assertEquals(id(0), partidas.obtenerJugadorActual(codigo()).getId());
        assertTrue(jugador(1).getPierdeTurno());
        assertThrows(ResponseStatusException.class, () -> cuandoTiraNormal(1));
        RespuestaMovimientoDTO carcel = cuandoTiraNormal(0);

        assertEquals(id(0), carcel.getTurnoSiguiente().getIdJugadorActual());
        assertTrue(jugador(0).getEstadoCarcel().getEncarcelado());
        assertEquals(8, jugador(0).getPosicionActual());
        assertFalse(jugador(1).getPierdeTurno());
        assertEquals(10, jugador(1).getUltimaRondaConOportunidad());
        assertEquals(11, partidas.obtenerPartida(codigo()).getRondaActual());
        assertThrows(ResponseStatusException.class, () -> cuandoTiraNormal(1));
        transaccion.executeWithoutResult(estado -> relojAnterior.run());
        assertEquals(id(0), partidas.obtenerJugadorActual(codigo()).getId());
        assertEquals(0, jugador(0).getEstadoCarcel().getIntentosSalirCarcel());
        verify(dados, times(1)).tirarDados();

        RespuestaMovimientoDTO liberacion = cuandoTiraNormal(0, new ResultadoTirada(6, 6));
        assertFalse(jugador(0).getEstadoCarcel().getEncarcelado());
        assertEquals(id(1), liberacion.getTurnoSiguiente().getIdJugadorActual());
        cuandoTiraNormal(1);
        assertEquals(20, jugador(1).getPosicionActual());
        assertEquals(11, jugador(1).getUltimaRondaConOportunidad());
        verify(dados, times(3)).tirarDados();
    }

    @Test
    public void dadoQuePiqueteTieneUnMovimientoReservadoCuandoAbandonanHastaQuedarUnActivoEntoncesConservaElRankingYCancelaLaConsecuencia() throws Exception {
        for (int cantidad : List.of(2, 3, 4)) {
            dadaUnaPartida(cantidad);
            cuandoSeActiva(TipoEventoGlobal.PIQUETE);
            cuandoTiran(cantidad == 2 ? new int[]{2, 12} : cantidad == 3 ? new int[]{2, 10, 12} : new int[]{2, 8, 10, 12});
            List<Runnable> anteriores = new ArrayList<>(vencimientos.values());
            for (int orden = cantidad - 1; orden > 0; orden--) partidas.abandonarPartida(codigo(), id(orden));

            Partida finalizada = partidas.obtenerPartidaParaVisualizacion(codigo());
            assertEquals(EstadoPartida.FINALIZADA, finalizada.getEstado());
            assertEquals(12, jugador(0).getPosicionActual());
            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
            assertNull(estado());
            for (Runnable anterior : anteriores) transaccion.executeWithoutResult(estado -> anterior.run());
            assertEquals(12, jugador(0).getPosicionActual());
            var ranking = finales.consultar(codigo(), jugador(0).getApodo());
            assertEquals(cantidad, ranking.resultados().size());
            assertEquals(id(0), ranking.resultados().get(0).idJugador());
            assertTrue(ranking.resultados().get(0).ganador());
            assertEquals(1500, ranking.resultados().get(0).patrimonioTotal());
            assertEquals(cantidad - 1, ranking.resultados().stream().filter(resultado ->
                    resultado.estadoParticipante() == com.tallerwebi.dominio.enums.EstadoParticipante.ABANDONADO).count());
        }
    }

    @Test
    public void dadoQuePiqueteEsperaConfirmacionesCuandoOtroJugadorQuiebraYQuedaUnActivoEntoncesFinalizaSinAplicarElMovimiento() throws Exception {
        dadaUnaPartida(2);
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        cuandoTiran(2, 12);

        quiebras.procesarMulta(codigo(), jugador(1), 3200, "FIANZA_CARCEL");

        assertTrue(jugador(1).getEnBancarrota());
        assertEquals(EstadoPartida.FINALIZADA, partidas.obtenerPartidaParaVisualizacion(codigo()).getEstado());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
        assertNull(estado());
        assertEquals(12, jugador(0).getPosicionActual());
    }

    @Test
    public void dadoQueLosActivosYaTuvieronSuOportunidadCuandoElSeleccionadoAbandonaDuranteLaPausaEntoncesReanudaEnLaRondaSiguiente() throws Exception {
        dadaUnaPartida(3);
        transaccion.executeWithoutResult(estado -> {
            Partida actual = repositorio.buscarPartidaActivaPorCodigoUnico(codigo());
            actual.setIndiceTurnoActual(2);
            actual.setTurnosFinalizadosEnRonda(2);
            for (Jugador jugador : actual.getJugadores()) jugador.setUltimaRondaConOportunidad(jugador.getOrden() == 2 ? 9 : 10);
        });
        cuandoSeActiva(TipoEventoGlobal.FIAT_600);
        partidas.abandonarPartida(codigo(), id(2));

        cuandoConfirmanLosActivos();

        assertEquals(11, partidas.obtenerPartida(codigo()).getRondaActual());
        assertEquals(0, partidas.obtenerPartida(codigo()).getTurnosFinalizadosEnRonda());
        assertEquals(id(0), partidas.obtenerJugadorActual(codigo()).getId());
        assertEquals(10, jugador(0).getUltimaRondaConOportunidad());
        assertEquals(10, jugador(1).getUltimaRondaConOportunidad());
        cuandoTiraNormal(0);
        assertEquals(11, partidas.obtenerPartida(codigo()).getRondaActual());
        assertEquals(1, partidas.obtenerPartida(codigo()).getTurnosFinalizadosEnRonda());
        assertEquals(id(1), partidas.obtenerJugadorActual(codigo()).getId());
    }

    @Test
    public void dadoQueRoboProvocaLaUltimaQuiebraCuandoSeAplicaEntoncesFinalizaYConservaLaDeudaEnElRankingSinDejarUnEventoActivo() throws Exception {
        dadaUnaPartida(2);
        transaccion.executeWithoutResult(estado -> repositorio.buscarJugadorPorId(id(1)).setDinero(300));
        dadoQueSeraSeleccionado(1);

        cuandoSeActiva(TipoEventoGlobal.ROBO);

        assertEquals(EstadoPartida.FINALIZADA, partidas.obtenerPartidaParaVisualizacion(codigo()).getEstado());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
        assertNull(estado());
        assertTrue(jugador(1).getEnBancarrota());
        var resultados = finales.consultar(codigo(), jugador(0).getApodo()).resultados();
        assertEquals(2, resultados.size());
        assertTrue(resultados.get(0).ganador());
        assertEquals(id(0), resultados.get(0).idJugador());
        assertEquals(300, resultados.get(1).dineroFinal());
        assertEquals(1000, resultados.get(1).deudaPendiente());
        assertEquals(-700, resultados.get(1).patrimonioTotal());
    }

    @Test
    public void dadoQueElUltimoTurnoActivaRoboYProvocaLaUltimaQuiebraCuandoFinalizaEntoncesConfirmaLosResultadosSinBuscarOtroTurno() throws Exception {
        dadaUnaPartida(2);
        transaccion.executeWithoutResult(estado -> {
            Partida actual = repositorio.buscarPartidaActivaPorCodigoUnico(codigo());
            actual.setRondaActual(4);
            actual.setIndiceTurnoActual(1);
            actual.setTurnosFinalizadosEnRonda(1);
            repositorio.buscarJugadorPorId(id(0)).setUltimaRondaConOportunidad(4);
            repositorio.buscarJugadorPorId(id(1)).setUltimaRondaConOportunidad(3);
            repositorio.buscarJugadorPorId(id(0)).setDinero(300);
        });
        dadoQueSeraSeleccionado(0);
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.ROBO));

        RespuestaMovimientoDTO respuesta = cuandoTiraNormal(1);

        assertNull(respuesta.getTurnoSiguiente());
        assertEquals(EstadoPartida.FINALIZADA, partidas.obtenerPartidaParaVisualizacion(codigo()).getEstado());
        assertTrue(jugador(0).getEnBancarrota());
        assertNull(estado());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo()));
        var ranking = finales.consultar(codigo(), jugador(1).getApodo()).resultados();
        assertEquals(2, ranking.size());
        assertEquals(id(1), ranking.get(0).idJugador());
        assertTrue(ranking.get(0).ganador());
        assertEquals(1000, ranking.get(1).deudaPendiente());
        assertEquals(-700, ranking.get(1).patrimonioTotal());
        verify(dados, times(1)).tirarDados();
    }
}
