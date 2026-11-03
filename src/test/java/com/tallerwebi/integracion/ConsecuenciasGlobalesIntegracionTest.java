package com.tallerwebi.integracion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.enums.FaseEventoGlobal;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.observadores.ObservadorReanudarPartida;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioCasilleroImpl;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioEventoImpl;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl;
import com.tallerwebi.dominio.servicios.ServicioOfertaPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import com.tallerwebi.infraestructura.RepositorioCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioDeudaPendienteImpl;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPropiedadImpl;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.presentacion.ControladorPropiedadesPartida;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.presentacion.ControladorGestionQuiebra;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorEventoGlobalWebSocket;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorTurnoWebSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba consecuencias y cobros reales con Hibernate; sólo se fuerzan el evento y los dados. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ConsecuenciasGlobalesIntegracionTest.Config.class)
public class ConsecuenciasGlobalesIntegracionTest {
    @Configuration
    @Import({com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl.class,
            com.tallerwebi.infraestructura.RepositorioFinalizarPartidaImpl.class, ServicioEventoImpl.class, ServicioPartidaImpl.class, ServicioTurnoImpl.class,
            ServicioParticipacionImpl.class, ServicioGestionQuiebraImpl.class, ServicioEconomiaImpl.class,
            ServicioPropiedadImpl.class, ServicioCasilleroImpl.class, ServicioOfertaPropiedadImpl.class,
            RepositorioPartidaImpl.class, RepositorioPropiedadImpl.class, RepositorioCasilleroImpl.class,
            RepositorioDeudaPendienteImpl.class, ObservadorReanudarPartida.class,
            ObservadorEventoGlobalWebSocket.class, ControladorPropiedadesPartida.class})
    static class Config extends HibernateTestConfig {
        @Bean @Override public DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:hsqldb:mem:consecuencias_globales", "sa", "");
        }
        @Bean public RepositorioEvento catalogo() { return mock(RepositorioEvento.class); }
        @Bean public ServicioDado dados() { return mock(ServicioDado.class); }
        @Bean public ServicioInicioPartida inicio() { return mock(ServicioInicioPartida.class); }
        @Bean public GestorTemporizadorPartida temporizador() { return mock(GestorTemporizadorPartida.class); }
        @Bean public SimpMessagingTemplate mensajeria() { return mock(SimpMessagingTemplate.class); }
        @Bean public PublicadorEventos<EventoJugadorAbandonado> abandonos() {
            return new PublicadorEventos<>(List.of());
        }
        @Bean public PublicadorEventos<EventoJugadorQuebrado> quiebras() {
            return new PublicadorEventos<>(List.of());
        }
        @Bean public PublicadorEventos<EventoTurnoCambiado> turnos(SimpMessagingTemplate mensajeria) {
            return new PublicadorEventos<>(List.of(new ObservadorTurnoWebSocket(new NotificadorPartida(mensajeria))));
        }
    }

    @Autowired private ServicioEvento eventos;
    @Autowired private ServicioPartida servicioPartida;
    @Autowired private ServicioTurno turnos;
    @Autowired private ServicioGestionQuiebra quiebras;
    @Autowired private ServicioOfertaPropiedad ofertas;
    @Autowired private ControladorPropiedadesPartida controladorOfertas;
    @Autowired private ControladorPropiedadesPartida controladorPropiedades;
    @Autowired private RepositorioPartida partidas;
    @Autowired private RepositorioEvento catalogo;
    @Autowired private SessionFactory sesiones;
    @Autowired private ServicioDado dados;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private SimpMessagingTemplate mensajeria;
    @Autowired private PlatformTransactionManager transacciones;
    private TransactionTemplate transaccion;
    private Partida partida;
    private final Map<String, Runnable> vencimientos = new HashMap<>();

    @BeforeEach
    public void preparar() {
        transaccion = new TransactionTemplate(transacciones);
        reset(catalogo, dados, temporizador, mensajeria);
        vencimientos.clear();
        doAnswer(invocacion -> {
            vencimientos.put(invocacion.getArgument(0), invocacion.getArgument(2));
            return null;
        }).when(temporizador).programarTemporizador(anyString(), anyInt(), any(Runnable.class));
        when(temporizador.obtenerSegundosRestantes(anyString())).thenReturn(30);
        when(temporizador.programarTemporizadorSiNoExiste(anyString(), anyInt(), any(Runnable.class))).thenReturn(true);
    }

    private void dadaUnaPartida(Integer saldo, boolean conPropiedades) {
        dadaUnaPartida(saldo, conPropiedades, 4);
    }

    private void dadaUnaPartida(Integer saldo, boolean conPropiedades, int cantidad) {
        transaccion.executeWithoutResult(estado -> {
            String codigo = UUID.randomUUID().toString();
            Usuario creador = new Usuario();
            creador.setUsername(codigo);
            creador.setEmail(codigo + "@test.local");
            sesiones.getCurrentSession().persist(creador);
            partida = new Partida();
            partida.setCodigoUnico(codigo);
            partida.setCreador(creador);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(5);
            List<Jugador> jugadores = new ArrayList<>();
            for (int numero = 0; numero < cantidad; numero++) {
                Jugador jugador = new Jugador();
                jugador.setApodo("Jugador" + numero);
                jugador.setOrden(numero);
                jugador.setDinero(saldo);
                jugador.setPosicionActual(20 + numero);
                jugadores.add(jugador);
            }
            partida.setJugadores(jugadores);
            partidas.guardarPartida(partida);
            if (conPropiedades) {
                for (Jugador jugador : jugadores) {
                    Propiedad propiedad = new Propiedad(null, "Propiedad " + jugador.getOrden(), 2000, 50, jugador, null);
                    CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicionPropiedad(jugador),
                            propiedad.getNombre(), "Propiedad de prueba", propiedad);
                    casillero.setPartidaId(partida.getId());
                    sesiones.getCurrentSession().persist(casillero);
                }
            }
        });
    }

    private EventoGlobal evento(TipoEventoGlobal tipo) {
        EventoGlobal evento = new EventoGlobal();
        evento.setId(tipo.ordinal() + 1);
        evento.setDescripcion("Evento global de prueba " + tipo);
        evento.setTipoEventoGlobal(tipo);
        evento.setTipoEfecto(switch (tipo) {
            case ROBO -> TipoEfecto.PERDER_PLATA;
            case FIAT_600 -> TipoEfecto.GANAR_PLATA;
            case ACCIDENTE_SARMIENTO -> TipoEfecto.PERDER_TURNO;
            case PIQUETE -> TipoEfecto.VOLVER_INICIO;
            default -> TipoEfecto.MOVER_CASILLEROS;
        });
        evento.setValor(switch (tipo) {
            case ROBO -> 1000;
            case FIAT_600 -> 400;
            case INUNDACION -> -3;
            case ACCIDENTE_SARMIENTO -> 1;
            default -> 0;
        });
        return evento;
    }

    private EstadoEventoGlobal cuandoSeActiva(TipoEventoGlobal tipo) throws Exception {
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(tipo));
        eventos.verificarActivacionEventoGlobal(partida);
        return eventos.consultarEstadoEventoGlobal(partida.getCodigoUnico());
    }

    private Jugador jugadorPersistido(Long id) {
        return transaccion.execute(estado -> partidas.buscarJugadorPorId(id));
    }

    private void cuandoConfirmanTodos() {
        for (Jugador jugador : servicioPartida.obtenerPartida(partida.getCodigoUnico()).getJugadores()) {
            if (!Boolean.TRUE.equals(jugador.getEnBancarrota()) && !Boolean.TRUE.equals(jugador.getAbandono())) {
                eventos.confirmarContinuacion(partida.getCodigoUnico(), jugador.getId());
            }
        }
    }

    @Test
    public void dadoQueTienenMilQuinientosCuandoOcurreRoboEntoncesPersisteQuinientosSinDeudaNiDobleCobro() throws Exception {
        dadaUnaPartida(1500, false);
        EstadoEventoGlobal resultado = cuandoSeActiva(TipoEventoGlobal.ROBO);
        Long elegido = resultado.idJugadorElegido();
        assertNotNull(elegido);
        assertEquals(500, jugadorPersistido(elegido).getDinero());
        assertEquals(500, resultado.saldos().get(elegido));
        assertNull(quiebras.obtenerDeudaPendiente(partida.getCodigoUnico(), elegido));

        eventos.verificarActivacionEventoGlobal(partida);
        cuandoConfirmanTodos();

        assertEquals(500, jugadorPersistido(elegido).getDinero());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(partida.getCodigoUnico()));
        verify(catalogo, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueTienenMilCuandoSaleElFititoEntoncesPersisteMilCuatrocientosUnaSolaVez() throws Exception {
        dadaUnaPartida(1000, false);
        EstadoEventoGlobal resultado = cuandoSeActiva(TipoEventoGlobal.FIAT_600);
        Long elegido = resultado.idJugadorElegido();
        assertNotNull(elegido);
        assertEquals(1400, jugadorPersistido(elegido).getDinero());
        assertEquals(1400, resultado.saldos().get(elegido));
        cuandoConfirmanTodos();
        eventos.verificarActivacionEventoGlobal(partida);

        assertEquals(1400, jugadorPersistido(elegido).getDinero());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(partida.getCodigoUnico()));
    }

    @Test
    public void dadoQueRoboGeneraDeudaCuandoVendeAlBancoEntoncesCobraAutomaticamenteYPermiteReanudar() throws Exception {
        dadaUnaPartida(300, true);
        EstadoEventoGlobal resultado = cuandoSeActiva(TipoEventoGlobal.ROBO);
        Jugador elegido = jugadorPersistido(resultado.idJugadorElegido());
        String codigo = partida.getCodigoUnico();
        DeudaPendiente deuda = quiebras.obtenerDeudaPendiente(codigo, elegido.getId());
        assertEquals(1000, deuda.getMontoPendiente());
        assertEquals("EVENTO_GLOBAL_ROBO", deuda.getMotivo());
        assertEquals(300, elegido.getDinero());
        cuandoConfirmanTodos();
        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        Integer indice = servicioPartida.obtenerPartida(codigo).getIndiceTurnoActual();
        servicioPartida.avanzarTurno(codigo);
        assertEquals(indice, servicioPartida.obtenerPartida(codigo).getIndiceTurnoActual());

        transaccion.executeWithoutResult(estado -> {
            try {
                assertEquals(200, controladorPropiedades.venderPropiedad(codigo, posicionPropiedad(elegido),
                        solicitud(elegido)).getStatusCode().value());
            } catch (Exception exception) { throw new IllegalStateException(exception); }
        });

        assertEquals(300, jugadorPersistido(elegido.getId()).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, elegido.getId()));
        assertFalse(quiebras.intentarResolverDeuda(codigo, elegido.getId()));
        eventos.consultarEstadoEventoGlobal(codigo);
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertNull(eventos.consultarEstadoEventoGlobal(codigo));
        assertEquals(300, jugadorPersistido(elegido.getId()).getDinero());
    }

    @Test
    public void dadoQueRoboGeneraDeudaCuandoAceptanSuOfertaEntoncesSeCobraElImporteSinDuplicarlo() throws Exception {
        dadaUnaPartida(300, true);
        EstadoEventoGlobal resultado = cuandoSeActiva(TipoEventoGlobal.ROBO);
        Jugador vendedor = jugadorPersistido(resultado.idJugadorElegido());
        Jugador comprador = partida.getJugadores().stream().filter(j -> !j.getId().equals(vendedor.getId())).findFirst().orElseThrow();
        transaccion.executeWithoutResult(estado -> partidas.buscarJugadorPorId(comprador.getId()).setDinero(1500));
        String codigo = partida.getCodigoUnico();
        String idOferta = transaccion.execute(estado -> ofertas.crearOferta(servicioPartida.obtenerPartida(codigo),
                posicionPropiedad(vendedor), vendedor.getId(), comprador.getId(), 700).id());

        transaccion.executeWithoutResult(estado -> assertEquals(200, controladorOfertas.responderOferta(codigo,
                idOferta, Map.of("aceptar", true), solicitud(comprador)).getStatusCode().value()));

        assertEquals(0, jugadorPersistido(vendedor.getId()).getDinero());
        assertEquals(800, jugadorPersistido(comprador.getId()).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, vendedor.getId()));
        assertEquals(0, eventos.consultarEstadoEventoGlobal(codigo).saldos().get(vendedor.getId()));
        transaccion.executeWithoutResult(estado -> assertEquals(409, controladorOfertas.responderOferta(codigo,
                idOferta, Map.of("aceptar", true), solicitud(comprador)).getStatusCode().value()));
        assertEquals(0, jugadorPersistido(vendedor.getId()).getDinero());
        cuandoConfirmanTodos();
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
    }

    private MockHttpServletRequest solicitud(Jugador jugador) {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", jugador.getApodo());
        return solicitud;
    }

    @Test
    public void dadoQueRoboGeneraDeudaCuandoOtroJugadorCompraPorSetecientosEntoncesPersisteCeroYActualizaLaAdvertencia() throws Exception {
        dadaUnaPartida(300, true, 2);
        EstadoEventoGlobal inicial = cuandoSeActiva(TipoEventoGlobal.ROBO);
        Jugador vendedor = jugadorPersistido(inicial.idJugadorElegido());
        Jugador comprador = partida.getJugadores().stream().filter(j -> !j.getId().equals(vendedor.getId())).findFirst().orElseThrow();
        String codigo = partida.getCodigoUnico();
        transaccion.executeWithoutResult(estado -> partidas.buscarJugadorPorId(comprador.getId()).setDinero(1500));
        assertTrue(inicial.detalle().contains("Necesita vender propiedades"));
        assertTrue(inicial.deudaRoboPendiente());
        assertEquals(1000, quiebras.obtenerDeudaPendiente(codigo, vendedor.getId()).getMontoPendiente());
        String idOferta = transaccion.execute(estado -> ofertas.crearOferta(servicioPartida.obtenerPartida(codigo),
                posicionPropiedad(vendedor), vendedor.getId(), comprador.getId(), 700).id());

        transaccion.executeWithoutResult(estado -> assertEquals(200, controladorOfertas.responderOferta(codigo,
                idOferta, Map.of("aceptar", true), solicitud(comprador)).getStatusCode().value()));

        assertEquals(0, jugadorPersistido(vendedor.getId()).getDinero());
        assertEquals(800, jugadorPersistido(comprador.getId()).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, vendedor.getId()));
        assertFalse(quiebras.intentarResolverDeuda(codigo, vendedor.getId()));
        Long propietario = transaccion.execute(estado -> sesiones.getCurrentSession().createQuery(
                "FROM CasilleroPropiedad c WHERE c.partidaId = :partida AND c.posicion = :posicion", CasilleroPropiedad.class)
                .setParameter("partida", partida.getId()).setParameter("posicion", posicionPropiedad(vendedor))
                .getSingleResult().getPropiedad().getPropietario().getId());
        assertEquals(comprador.getId(), propietario);
        EstadoEventoGlobal actualizado = eventos.consultarEstadoEventoGlobal(codigo);
        assertFalse(actualizado.deudaRoboPendiente());
        assertEquals(0, actualizado.saldos().get(vendedor.getId()));
        assertTrue(actualizado.detalle().contains("saldó su deuda por robo"));
        assertFalse(actualizado.detalle().contains("Necesita vender propiedades"));
        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        cuandoConfirmanTodos();
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertEquals(0, jugadorPersistido(vendedor.getId()).getDinero());
        assertEquals(800, jugadorPersistido(comprador.getId()).getDinero());
    }

    private Integer posicionPropiedad(Jugador jugador) {
        return List.of(1, 2, 4, 5).get(jugador.getOrden());
    }

    @Test
    public void dadoQueDosJugadoresSacanCincoYNueveCuandoResuelvenPiqueteEntoncesElPrimeroVuelveAInicio() throws Exception {
        verificarPiqueteConTiradas(List.of(5, 9), 0);
    }

    @Test
    public void dadoQueTresJugadoresSacanDiezCuatroYSieteCuandoResuelvenPiqueteEntoncesElSegundoVuelveAInicio() throws Exception {
        verificarPiqueteConTiradas(List.of(10, 4, 7), 1);
    }

    @Test
    public void dadoQueCuatroJugadoresSacanOchoTresOnceYSeisCuandoResuelvenPiqueteEntoncesElSegundoVuelveAInicio() throws Exception {
        verificarPiqueteConTiradas(List.of(8, 3, 11, 6), 1);
    }

    private void prepararTiradas(List<Integer> sumas) {
        ResultadoTirada[] resultados = sumas.stream().map(suma -> new ResultadoTirada(suma / 2, suma - suma / 2))
                .toArray(ResultadoTirada[]::new);
        when(dados.tirarDados()).thenReturn(resultados[0], Arrays.copyOfRange(resultados, 1, resultados.length));
    }

    private void verificarPiqueteConTiradas(List<Integer> sumas, int indiceElegido) throws Exception {
        dadaUnaPartida(1000, false, sumas.size());
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        prepararTiradas(sumas);
        String codigo = partida.getCodigoUnico();
        for (Jugador jugador : partida.getJugadores()) eventos.tirarDadosEventoGlobal(codigo, jugador.getId());
        Long elegido = partida.getJugadores().get(indiceElegido).getId();
        EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(codigo);
        assertEquals(FaseEventoGlobal.FINALIZADO, resultado.fase());
        assertEquals(elegido, resultado.idJugadorElegido());
        assertEquals(sumas.size(), resultado.tiradas().size());
        for (int indice = 0; indice < sumas.size() - 1; indice++) {
            Long id = partida.getJugadores().get(indice).getId();
            eventos.confirmarContinuacion(codigo, id);
            eventos.confirmarContinuacion(codigo, id);
            assertEquals(20 + indiceElegido, jugadorPersistido(elegido).getPosicionActual());
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        }

        eventos.confirmarContinuacion(codigo, partida.getJugadores().get(sumas.size() - 1).getId());

        assertEquals(0, jugadorPersistido(elegido).getPosicionActual());
        for (Jugador jugador : partida.getJugadores()) {
            assertEquals(1000, jugadorPersistido(jugador.getId()).getDinero());
            if (!jugador.getId().equals(elegido)) assertEquals(jugador.getPosicionActual(), jugadorPersistido(jugador.getId()).getPosicionActual());
        }
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo, elegido));
        assertEquals(0, jugadorPersistido(elegido).getPosicionActual());
        verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
    }

    @Test
    public void dadoQueDosEmpatanConLaMenorCuandoDesempatanPiqueteEntoncesSoloEllosTiranYElMenorVuelveAInicio() throws Exception {
        dadaUnaPartida(1000, false);
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        prepararTiradas(List.of(3, 3, 11, 6));
        String codigo = partida.getCodigoUnico();
        for (Jugador jugador : partida.getJugadores()) eventos.tirarDadosEventoGlobal(codigo, jugador.getId());
        EstadoEventoGlobal empate = eventos.consultarEstadoEventoGlobal(codigo);
        assertEquals(FaseEventoGlobal.EMPATE, empate.fase());
        assertEquals(java.util.Set.of(partida.getJugadores().get(0).getId(), partida.getJugadores().get(1).getId()),
                java.util.Set.copyOf(empate.participantes()));
        verify(temporizador).programarTemporizador(eq("evento-global-empate:" + codigo), eq(8), any(Runnable.class));

        transaccion.executeWithoutResult(estado -> vencimientos.get("evento-global-empate:" + codigo).run());
        assertThrows(TiradaDadoInvalidaException.class,
                () -> eventos.tirarDadosEventoGlobal(codigo, partida.getJugadores().get(2).getId()));
        prepararTiradas(List.of(7, 4));
        eventos.tirarDadosEventoGlobal(codigo, partida.getJugadores().get(0).getId());
        assertEquals(FaseEventoGlobal.DESEMPATE, eventos.consultarEstadoEventoGlobal(codigo).fase());
        eventos.tirarDadosEventoGlobal(codigo, partida.getJugadores().get(1).getId());

        Long elegido = partida.getJugadores().get(1).getId();
        assertEquals(elegido, eventos.consultarEstadoEventoGlobal(codigo).idJugadorElegido());
        assertEquals(21, jugadorPersistido(elegido).getPosicionActual());
        cuandoConfirmanTodos();
        assertEquals(0, jugadorPersistido(elegido).getPosicionActual());
        assertEquals(1000, jugadorPersistido(elegido).getDinero());
    }

    @Test
    public void dadoQuePiqueteEstaEnCursoCuandoSeRecuperaPorRestEntoncesConservaLasTiradasYRechazaDuplicarlas() throws Exception {
        dadaUnaPartida(1000, false, 2);
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        prepararTiradas(List.of(5, 9));
        String codigo = partida.getCodigoUnico();
        Long primero = partida.getJugadores().get(0).getId();
        eventos.tirarDadosEventoGlobal(codigo, primero);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorEventoGlobal(servicioPartida, eventos,
                new ServicioParticipacionImpl())).build();
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("NOMBRE", partida.getJugadores().get(0).getApodo());
        MvcResult respuesta = mvc.perform(MockMvcRequestBuilders.get("/partida/" + codigo + "/evento-global")
                .session(sesion)).andReturn();
        assertEquals(200, respuesta.getResponse().getStatus());
        assertEquals(5, new ObjectMapper().readTree(respuesta.getResponse().getContentAsString())
                .get("tiradas").get(primero.toString()).intValue());
        assertThrows(TiradaDadoInvalidaException.class, () -> eventos.tirarDadosEventoGlobal(codigo, primero));
        eventos.tirarDadosEventoGlobal(codigo, partida.getJugadores().get(1).getId());
        assertEquals(primero, eventos.consultarEstadoEventoGlobal(codigo).idJugadorElegido());
        cuandoConfirmanTodos();
        assertEquals(0, jugadorPersistido(primero).getPosicionActual());
        verify(dados, times(2)).tirarDados();
    }

    @Test
    public void dadoQueFaltaUnaConfirmacionDePiqueteCuandoVencenSesentaSegundosEntoncesPersisteInicioSinRepetirElMovimiento() throws Exception {
        dadaUnaPartida(1000, false, 2);
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        prepararTiradas(List.of(5, 9));
        String codigo = partida.getCodigoUnico();
        Long elegido = partida.getJugadores().get(0).getId();
        eventos.tirarDadosEventoGlobal(codigo, elegido);
        eventos.tirarDadosEventoGlobal(codigo, partida.getJugadores().get(1).getId());
        eventos.confirmarContinuacion(codigo, elegido);
        Runnable vencimiento = vencimientos.get("evento-global-resultado:" + codigo);
        verify(temporizador).programarTemporizador(eq("evento-global-resultado:" + codigo), eq(60), any(Runnable.class));
        assertEquals(20, jugadorPersistido(elegido).getPosicionActual());
        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));

        Thread.sleep(60_050L);
        transaccion.executeWithoutResult(estado -> vencimiento.run());
        transaccion.executeWithoutResult(estado -> vencimiento.run());

        assertEquals(0, jugadorPersistido(elegido).getPosicionActual());
        assertEquals(1000, jugadorPersistido(elegido).getDinero());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertNull(eventos.consultarEstadoEventoGlobal(codigo));
        verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
    }

    @Test
    public void dadoQueHayAbandonoYQuiebraCuandoSeResuelvePiqueteEntoncesSoloParticipanYConfirmanLosActivos() throws Exception {
        dadaUnaPartida(1000, false);
        Long primero = partida.getJugadores().get(0).getId();
        Long segundo = partida.getJugadores().get(1).getId();
        Long abandonado = partida.getJugadores().get(2).getId();
        Long quebrado = partida.getJugadores().get(3).getId();
        transaccion.executeWithoutResult(estado -> {
            partidas.buscarJugadorPorId(abandonado).setAbandono(true);
            partidas.buscarJugadorPorId(quebrado).setEnBancarrota(true);
            partida = partidas.buscarPartidaActivaPorCodigoUnico(partida.getCodigoUnico());
        });
        cuandoSeActiva(TipoEventoGlobal.PIQUETE);
        prepararTiradas(List.of(5, 9));
        String codigo = partida.getCodigoUnico();
        assertEquals(2, eventos.consultarEstadoEventoGlobal(codigo).participantes().size());
        assertThrows(TiradaDadoInvalidaException.class,
                () -> eventos.tirarDadosEventoGlobal(codigo, abandonado));
        eventos.tirarDadosEventoGlobal(codigo, primero);
        eventos.tirarDadosEventoGlobal(codigo, segundo);
        assertEquals(primero, eventos.consultarEstadoEventoGlobal(codigo).idJugadorElegido());

        cuandoConfirmanTodos();

        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertEquals(0, jugadorPersistido(primero).getPosicionActual());
        assertEquals(22, jugadorPersistido(abandonado).getPosicionActual());
        assertEquals(23, jugadorPersistido(quebrado).getPosicionActual());
        verify(dados, times(2)).tirarDados();
    }

    @Test
    public void dadoQueNoTieneBienesCuandoRoboLoQuiebraEntoncesPersisteLaQuiebraYElResultadoReal() throws Exception {
        dadaUnaPartida(300, false);
        cuandoSeActiva(TipoEventoGlobal.ROBO);
        Jugador quebrado = servicioPartida.obtenerPartida(partida.getCodigoUnico()).getJugadores().stream()
                .filter(j -> Boolean.TRUE.equals(j.getEnBancarrota())).findFirst().orElseThrow();
        assertTrue(jugadorPersistido(quebrado.getId()).getEnBancarrota());
        assertNull(quiebras.obtenerDeudaPendiente(partida.getCodigoUnico(), quebrado.getId()));

        EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(partida.getCodigoUnico());

        assertEquals(quebrado.getId(), resultado.idJugadorElegido());
        assertTrue(resultado.detalle().contains("quiebra"));
        assertFalse(resultado.detalle().contains("abandonó"));
        cuandoConfirmanTodos();
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(partida.getCodigoUnico()));
    }

    @Test
    public void dadoQueSarmientoPenalizaCuandoSeReanudaEntoncesSalteaExactamenteSuProximoTurno() throws Exception {
        dadaUnaPartida(1000, false);
        EstadoEventoGlobal resultado = cuandoSeActiva(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        Long elegido = resultado.idJugadorElegido();
        assertTrue(jugadorPersistido(elegido).getPierdeTurno());
        transaccion.executeWithoutResult(estado -> partidas.buscarPartidaActivaPorCodigoUnico(partida.getCodigoUnico())
                .setIndiceTurnoActual(jugadorPersistido(elegido).getOrden()));

        cuandoConfirmanTodos();

        assertFalse(jugadorPersistido(elegido).getPierdeTurno());
        assertFalse(elegido.equals(servicioPartida.obtenerJugadorActual(partida.getCodigoUnico()).getId()));
        verify(temporizador).programarTemporizadorSiNoExiste(eq(partida.getCodigoUnico()), eq(30), any(Runnable.class));
        for (int numero = 0; numero < 3; numero++) servicioPartida.avanzarTurno(partida.getCodigoUnico());
        assertEquals(elegido, servicioPartida.obtenerJugadorActual(partida.getCodigoUnico()).getId());
        assertFalse(jugadorPersistido(elegido).getPierdeTurno());
    }

    @Test
    public void dadoQueInundacionCruzaInicioCuandoConfirmanEntoncesPersisteVeintiochoSinPremio() throws Exception {
        dadaUnaPartida(1000, false);
        Long elegido = partida.getJugadores().get(0).getId();
        transaccion.executeWithoutResult(estado -> partidas.buscarJugadorPorId(elegido).setPosicionActual(1));
        cuandoSeActiva(TipoEventoGlobal.INUNDACION);
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(3, 2));
        for (Jugador jugador : partida.getJugadores()) eventos.tirarDadosEventoGlobal(partida.getCodigoUnico(), jugador.getId());
        assertEquals(1, jugadorPersistido(elegido).getPosicionActual());

        cuandoConfirmanTodos();

        assertEquals(28, jugadorPersistido(elegido).getPosicionActual());
        assertEquals(1000, jugadorPersistido(elegido).getDinero());
        assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(partida.getCodigoUnico(), elegido));
        assertEquals(28, jugadorPersistido(elegido).getPosicionActual());
    }

    @Test
    public void dadoQuePruebaLocalSacaOchoYTest123CuatroCuandoInundacionSeActivaEnCincoEntoncesRetrocedeDeRamosMejiaAColectivo620UnaVez() throws Exception {
        dadaUnaPartida(1000, false, 2);
        Long idTest123 = partida.getJugadores().get(0).getId();
        Long idPruebaLocal = partida.getJugadores().get(1).getId();
        String codigo = partida.getCodigoUnico();
        transaccion.executeWithoutResult(estado -> {
            Partida actual = partidas.buscarPartidaActivaPorCodigoUnico(codigo);
            actual.setRondaActual(4);
            actual.setIndiceTurnoActual(1);
            actual.setTurnosFinalizadosEnRonda(1);
            Jugador test123 = partidas.buscarJugadorPorId(idTest123);
            test123.setApodo("test123");
            test123.setUltimaRondaConOportunidad(4);
            Jugador pruebaLocal = partidas.buscarJugadorPorId(idPruebaLocal);
            pruebaLocal.setApodo("prueba_local");
            pruebaLocal.setUltimaRondaConOportunidad(3);
            pruebaLocal.setPosicionActual(27);
        });
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.INUNDACION));
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 3), new ResultadoTirada(4, 4));

        servicioPartida.avanzarTurno(codigo);
        assertEquals(5, eventos.consultarEstadoEventoGlobal(codigo).rondaActivacion());
        eventos.tirarDadosEventoGlobal(codigo, idTest123);
        eventos.tirarDadosEventoGlobal(codigo, idPruebaLocal);
        EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(codigo);
        assertEquals(idPruebaLocal, resultado.idJugadorElegido());
        assertEquals(Map.of(idTest123, 4, idPruebaLocal, 8), resultado.tiradas());
        assertEquals(27, resultado.posiciones().get(idPruebaLocal));
        assertTrue(resultado.detalle().contains("del casillero 27 al 24"));
        assertEquals(27, jugadorPersistido(idPruebaLocal).getPosicionActual());
        eventos.confirmarContinuacion(codigo, idTest123);
        eventos.confirmarContinuacion(codigo, idTest123);
        assertEquals(27, jugadorPersistido(idPruebaLocal).getPosicionActual());
        Runnable vencimientoAnterior = vencimientos.get("evento-global-resultado:" + codigo);
        eventos.confirmarContinuacion(codigo, idPruebaLocal);
        vencimientoAnterior.run();

        assertEquals(24, jugadorPersistido(idPruebaLocal).getPosicionActual());
        assertEquals(20, jugadorPersistido(idTest123).getPosicionActual());
        assertEquals(1000, jugadorPersistido(idPruebaLocal).getDinero());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo, idPruebaLocal));
        assertEquals(24, jugadorPersistido(idPruebaLocal).getPosicionActual());
        verify(dados, times(2)).tirarDados();
        verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
    }

    @Test
    public void dadoQueInundacionComienzaEnCeroUnoODosCuandoConfirmanEntoncesPersisteElRetrocesoCircularSinPremio() throws Exception {
        for (Map.Entry<Integer, Integer> caso : Map.of(0, 27, 1, 28, 2, 29).entrySet()) {
            dadaUnaPartida(1000, false, 2);
            Long afectado = partida.getJugadores().get(1).getId();
            transaccion.executeWithoutResult(estado -> partidas.buscarJugadorPorId(afectado).setPosicionActual(caso.getKey()));
            cuandoSeActiva(TipoEventoGlobal.INUNDACION);
            when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 3), new ResultadoTirada(4, 4));
            for (Jugador jugador : partida.getJugadores()) eventos.tirarDadosEventoGlobal(partida.getCodigoUnico(), jugador.getId());
            assertEquals(caso.getKey(), jugadorPersistido(afectado).getPosicionActual());

            cuandoConfirmanTodos();

            assertEquals(caso.getValue(), jugadorPersistido(afectado).getPosicionActual());
            assertEquals(1000, jugadorPersistido(afectado).getDinero());
            assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(partida.getCodigoUnico(), afectado));
            assertEquals(caso.getValue(), jugadorPersistido(afectado).getPosicionActual());
        }
    }

    @Test
    public void dadoQueInundacionParteDeInicioEnRondaTreintaYCincoCuandoGanaConOchoEntoncesPersisteRamosMejiaUnaVez() throws Exception {
        dadaUnaPartida(1000, false, 2);
        Long elegido = partida.getJugadores().get(0).getId();
        transaccion.executeWithoutResult(estado -> {
            Partida actual = partidas.buscarPartidaActivaPorCodigoUnico(partida.getCodigoUnico());
            actual.setRondaActual(34);
            actual.setTurnosFinalizadosEnRonda(1);
            partidas.buscarJugadorPorId(elegido).setPosicionActual(0);
                    dadasLasOportunidadesAnteriores(actual);
        });
        when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.INUNDACION));
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(4, 4), new ResultadoTirada(1, 2));
        String codigo = partida.getCodigoUnico();

        servicioPartida.avanzarTurno(codigo);
        for (Jugador jugador : partida.getJugadores()) eventos.tirarDadosEventoGlobal(codigo, jugador.getId());

        EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(codigo);
        assertEquals(35, resultado.rondaActivacion());
        assertEquals(elegido, resultado.idJugadorElegido());
        assertEquals(8, resultado.tiradas().get(elegido));
        assertEquals(3, resultado.tiradas().get(partida.getJugadores().get(1).getId()));
        assertEquals(0, jugadorPersistido(elegido).getPosicionActual());
        cuandoConfirmanTodos();
        assertEquals(27, jugadorPersistido(elegido).getPosicionActual());
        assertEquals(1000, jugadorPersistido(elegido).getDinero());
        assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo, elegido));
        assertEquals(27, jugadorPersistido(elegido).getPosicionActual());
    }

    @Test
    public void dadosDosTresYCuatroJugadoresCuandoSarmientoSeRepiteEnVeinticincoYTreintaEntoncesPersisteUnSoloTurnoPerdido() throws Exception {
        for (int cantidad : List.of(2, 3, 4)) {
            dadaUnaPartida(1000, false, cantidad);
            String codigo = partida.getCodigoUnico();
            for (int ronda : List.of(25, 30)) {
                transaccion.executeWithoutResult(estado -> {
                    Partida actual = partidas.buscarPartidaActivaPorCodigoUnico(codigo);
                    actual.setRondaActual(ronda - 1);
                    actual.setTurnosFinalizadosEnRonda(cantidad - 1);
                    actual.setIndiceTurnoActual(cantidad - 1);
                            dadasLasOportunidadesAnteriores(actual);
        });
                when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.ACCIDENTE_SARMIENTO));

                servicioPartida.avanzarTurno(codigo);

                EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(codigo);
                Long elegido = resultado.idJugadorElegido();
                int ordenElegido = jugadorPersistido(elegido).getOrden();
                int ordenReanudado = ordenElegido == 0 ? 1 : 0;
                assertEquals(ronda, resultado.rondaActivacion());
                assertEquals(ordenReanudado, servicioPartida.obtenerJugadorActual(codigo).getOrden());
                assertEquals(ordenElegido != 0, jugadorPersistido(elegido).getPierdeTurno());
                int posicion = jugadorPersistido(elegido).getPosicionActual();
                cuandoConfirmanTodos();
                assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
                assertNull(eventos.consultarEstadoEventoGlobal(codigo));
                assertEquals(ordenReanudado, servicioPartida.obtenerJugadorActual(codigo).getOrden());
                int oportunidades = cantidad - 1 + ordenElegido;
                for (int numero = 0; numero < oportunidades; numero++) {
                    if (numero < oportunidades - 1) {
                        assertFalse(elegido.equals(servicioPartida.obtenerJugadorActual(codigo).getId()));
                    }
                    servicioPartida.avanzarTurno(codigo);
                }
                assertEquals(elegido, servicioPartida.obtenerJugadorActual(codigo).getId());
                assertFalse(jugadorPersistido(elegido).getPierdeTurno());
                assertFalse(jugadorPersistido(elegido).getTiroEnTurno());
                assertEquals(posicion, jugadorPersistido(elegido).getPosicionActual());
            }
        }
        verify(dados, org.mockito.Mockito.never()).tirarDados();
    }

    @Test
    public void dadoQueAvanzanLasRondasCuandoLleganACincoDiezQuinceYVeinteEntoncesResuelveCuatroEventos() throws Exception {
        dadaUnaPartida(1500, false);
        String codigo = partida.getCodigoUnico();
        for (int ronda : List.of(5, 10, 15, 20)) {
            transaccion.executeWithoutResult(estado -> {
                Partida actual = partidas.buscarPartidaActivaPorCodigoUnico(codigo);
                actual.setRondaActual(ronda - 1);
                actual.setTurnosFinalizadosEnRonda(3);
                partida = actual;
                        dadasLasOportunidadesAnteriores(actual);
        });
            when(catalogo.obtenerEventoGlobalAleatorio()).thenReturn(evento(TipoEventoGlobal.FIAT_600));
            servicioPartida.avanzarTurno(codigo);
            assertEquals(ronda, eventos.consultarEstadoEventoGlobal(codigo).rondaActivacion());
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
            cuandoConfirmanTodos();
            assertNull(eventos.consultarEstadoEventoGlobal(codigo));
            assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        }
        verify(catalogo, times(4)).obtenerEventoGlobalAleatorio();
    }

    /** Navegador real, cuatro sesiones y consecuencias persistidas; HTTP y STOMP usan un puente controlado. */
    @Test
    public void dadosCuatroJugadoresCuandoResuelvenCadaEventoEntoncesTodosVenLaConsecuenciaPersistidaYElTurnoCorrecto() throws Exception {
        Path ejecutable = Path.of("C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(ejecutable), "Microsoft Edge no está instalado");
        Path capturas = Path.of("target/eventos-globales-visual");
        Files.createDirectories(capturas);
        ObjectMapper json = new ObjectMapper();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ControladorEventoGlobal(servicioPartida, eventos, new ServicioParticipacionImpl()),
                new ControladorGestionQuiebra(servicioPartida, quiebras, new ServicioParticipacionImpl())).build();
        try (Playwright playwright = Playwright.create();
                Browser navegador = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setExecutablePath(ejecutable).setHeadless(true))) {
            for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
                dadaUnaPartida(tipo == TipoEventoGlobal.ROBO ? 1500 : 1000, false);
                List<Map<String, String>> avisos = new ArrayList<>();
                doAnswer(invocacion -> {
                    avisos.add(Map.of("canal", invocacion.getArgument(0),
                            "cuerpo", json.writeValueAsString(invocacion.getArgument(1))));
                    return null;
                }).when(mensajeria).convertAndSend(anyString(), any(Object.class));
                cuandoSeActiva(tipo);
                when(dados.tirarDados()).thenReturn(tipo == TipoEventoGlobal.PIQUETE
                        ? new ResultadoTirada(1, 1) : new ResultadoTirada(6, 6), new ResultadoTirada(3, 2));
                List<BrowserContext> contextos = new ArrayList<>();
                List<Page> paginas = new ArrayList<>();
                try {
                    for (Jugador jugador : partida.getJugadores()) {
                        BrowserContext contexto = navegador.newContext();
                        contextos.add(contexto);
                        Page pagina = contexto.newPage();
                        paginas.add(pagina);
                        MockHttpSession sesion = new MockHttpSession();
                        sesion.setAttribute("NOMBRE", jugador.getApodo());
                        pagina.route("**/*", ruta -> responderCliente(ruta, mvc, sesion, json));
                        pagina.navigate("http://monopoly.test/spring/partida?codigo=" + partida.getCodigoUnico());
                        pagina.addStyleTag(new Page.AddStyleTagOptions().setPath(Path.of("src/main/webapp/resources/core/css/partida.css")));
                        pagina.evaluate("""
                                ([id, jugadores]) => {
                                    window.usuarioSesionId = id; window.turnoActualId = jugadores[0].id;
                                    window.dineroUsuarioSesion = jugadores.find(j => j.id === id).dinero;
                                    document.querySelector('.jugadores').innerHTML = '';
                                    document.querySelectorAll('.ficha').forEach(f => f.remove());
                                    jugadores.forEach(j => {
                                        const tarjeta = document.createElement('article');
                                        tarjeta.className = 'jugador'; tarjeta.id = j.id;
                                        tarjeta.innerHTML = '<h3 class="nombre-jugador">' + j.apodo + '</h3><span class="dinero">$' + j.dinero + '</span>';
                                        document.querySelector('.jugadores').appendChild(tarjeta);
                                        const ficha = document.createElement('div'); ficha.className = 'ficha ficha-rojo';
                                        ficha.dataset.jugadorId = j.id;
                                        document.querySelector('[data-casillero="' + j.posicion + '"] .fichas-en-casillero').appendChild(ficha);
                                    });
                                    window.suscripcionesPrueba = {}; window.SockJS = function () {};
                                    window.Stomp = {over: () => ({connected: true, send: () => {},
                                        connect: (cabeceras, conectado) => conectado(),
                                        subscribe: (canal, receptor) => { window.suscripcionesPrueba[canal] = receptor; }})};
                                }
                                """, List.of(jugador.getId().toString(), partida.getJugadores().stream()
                                .map(j -> Map.of("id", j.getId().toString(), "apodo", j.getApodo(),
                                        "posicion", j.getPosicionActual(), "dinero", j.getDinero())).toList()));
                        pagina.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/webapp/resources/core/js/partida.js")));
                        pagina.evaluate("posicionesInicialesSincronizadas = true; conectarWebSocketTurnos('/spring', codigoPartida)");
                        pagina.waitForFunction("estadoEventoGlobal !== null");
                        assertFalse(pagina.locator("#btn-tirar").isVisible());
                    }
                    boolean conDados = List.of(TipoEventoGlobal.INUNDACION, TipoEventoGlobal.PIQUETE,
                            TipoEventoGlobal.TRAMITES_AFIP).contains(tipo);
                    if (conDados) {
                        for (Page pagina : paginas) {
                            pagina.waitForResponse(r -> r.url().endsWith("/evento-global/tirar"),
                                    () -> pagina.locator("#btn-tirar-evento-global").click());
                            distribuirAvisos(avisos, paginas);
                        }
                    }
                    EstadoEventoGlobal resultado = eventos.consultarEstadoEventoGlobal(partida.getCodigoUnico());
                    Long elegido = resultado.idJugadorElegido();
                    if (tipo == TipoEventoGlobal.ACCIDENTE_SARMIENTO) {
                        assertTrue(jugadorPersistido(elegido).getPierdeTurno());
                        transaccion.executeWithoutResult(estado -> partidas.buscarPartidaActivaPorCodigoUnico(partida.getCodigoUnico())
                                .setIndiceTurnoActual(jugadorPersistido(elegido).getOrden()));
                    }
                    for (Page pagina : paginas) {
                        pagina.waitForFunction("estadoEventoGlobal?.fase === 'FINALIZADO'");
                        assertTrue(pagina.locator("#modal-evento-global").isVisible());
                        assertFalse(pagina.locator(".dados").isVisible());
                        if (conDados) assertEquals("20", posicionEnPantalla(pagina, elegido));
                    }
                    paginas.get(0).waitForFunction("!document.getElementById('btn-continuar-partida-global').hidden");
                    paginas.get(0).screenshot(new Page.ScreenshotOptions().setFullPage(true)
                            .setPath(capturas.resolve(tipo + "-resultado-4-jugadores.png")));
                    for (int indice = 0; indice < paginas.size(); indice++) {
                        Page pagina = paginas.get(indice);
                        pagina.waitForResponse(r -> r.url().endsWith("/evento-global/continuar"),
                                () -> pagina.locator("#btn-continuar-partida-global").click());
                        distribuirAvisos(avisos, paginas);
                        if (indice < paginas.size() - 1) {
                            assertTrue(eventos.verificarSiHayEventoGlobalActivo(partida.getCodigoUnico()));
                            for (Page cliente : paginas) assertTrue(cliente.locator("#modal-evento-global").isVisible());
                            if (conDados) assertEquals(20, jugadorPersistido(elegido).getPosicionActual());
                        }
                    }
                    for (Page pagina : paginas) {
                        pagina.waitForFunction("estadoEventoGlobal === null");
                        assertTrue(pagina.locator("#btn-tirar").isVisible());
                        assertTrue(pagina.locator(".dados").isVisible());
                        Jugador persistido = jugadorPersistido(elegido);
                        assertEquals(persistido.getPosicionActual().toString(), posicionEnPantalla(pagina, elegido));
                        assertEquals(persistido.getDinero().toString(), pagina.locator("[id='" + elegido + "'] .dinero")
                                .textContent().replaceAll("[^0-9]", ""));
                        Long idActual = servicioPartida.obtenerJugadorActual(partida.getCodigoUnico()).getId();
                        pagina.waitForFunction("id => String(window.turnoActualId) === id", idActual.toString());
                        if (tipo == TipoEventoGlobal.ACCIDENTE_SARMIENTO) {
                            assertFalse(elegido.equals(idActual));
                            assertFalse(persistido.getPierdeTurno());
                        }
                    }
                    Integer posicionEsperada = switch (tipo) {
                        case INUNDACION -> 17;
                        case PIQUETE -> 0;
                        case TRAMITES_AFIP -> 23;
                        default -> jugadorPersistido(elegido).getPosicionActual();
                    };
                    assertEquals(posicionEsperada, jugadorPersistido(elegido).getPosicionActual());
                    assertEquals(tipo == TipoEventoGlobal.ROBO ? 500 : tipo == TipoEventoGlobal.FIAT_600 ? 1400 : 1000,
                            jugadorPersistido(elegido).getDinero().intValue());
                    assertFalse(eventos.verificarSiHayEventoGlobalActivo(partida.getCodigoUnico()));
                    paginas.get(0).screenshot(new Page.ScreenshotOptions().setFullPage(true)
                            .setPath(capturas.resolve(tipo + "-reanudado-4-jugadores.png")));
                } finally {
                    contextos.forEach(BrowserContext::close);
                }
            }
        }
    }

    private String posicionEnPantalla(Page pagina, Long idJugador) {
        return (String) pagina.locator(".ficha[data-jugador-id='" + idJugador + "']")
                .evaluate("ficha => ficha.closest('.casillero').dataset.casillero");
    }

    private void distribuirAvisos(List<Map<String, String>> avisos, List<Page> paginas) {
        while (!avisos.isEmpty()) {
            Map<String, String> aviso = avisos.remove(0);
            for (Page pagina : paginas) pagina.evaluate(
                    "aviso => window.suscripcionesPrueba[aviso.canal]?.({body: aviso.cuerpo})", aviso);
        }
    }

    private void responderCliente(Route ruta, MockMvc mvc, MockHttpSession sesion, ObjectMapper json) {
        String direccion = ruta.request().url();
        try {
            if (direccion.startsWith("https://fonts.googleapis.com/")) {
                ruta.fulfill(new Route.FulfillOptions().setContentType("text/css").setBody(""));
            } else if (direccion.contains("/evento-global") || direccion.contains("/deuda-pendiente")) {
                String destino = URI.create(direccion).getPath().substring("/spring".length());
                MockHttpServletRequestBuilder solicitud = "POST".equals(ruta.request().method())
                        ? MockMvcRequestBuilders.post(destino) : MockMvcRequestBuilders.get(destino);
                MvcResult resultado = transaccion.execute(estado -> {
                    try { return mvc.perform(solicitud.session(sesion)).andReturn(); }
                    catch (Exception exception) { throw new IllegalStateException(exception); }
                });
                ruta.fulfill(new Route.FulfillOptions().setStatus(resultado.getResponse().getStatus())
                        .setContentType("application/json").setBody(resultado.getResponse().getContentAsString()));
            } else if (direccion.contains("/turno")) {
                Jugador actual = servicioPartida.obtenerJugadorActual(partida.getCodigoUnico());
                ruta.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(json.writeValueAsString(
                        Map.of("idJugadorActual", actual.getId(), "apodoJugadorActual", actual.getApodo(),
                                "duracionSegundos", 30, "rondaActual", servicioPartida.obtenerPartida(partida.getCodigoUnico()).getRondaActual(),
                                "turnosFinalizadosEnRonda", 0))));
            } else if (direccion.contains("/partida?")) {
                String plantilla = Files.readString(Path.of("src/main/webapp/WEB-INF/views/thymeleaf/partida.html"))
                        .replaceAll("(?s)<script\\b[^>]*>.*?</script>", "").replaceAll("<link\\b[^>]*>", "");
                ruta.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(plantilla));
            } else {
                ruta.fulfill(new Route.FulfillOptions().setStatus(404).setBody(""));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Falló el puente del navegador de prueba", exception);
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
}
