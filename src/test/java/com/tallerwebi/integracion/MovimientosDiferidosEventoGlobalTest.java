package com.tallerwebi.integracion;

import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.servicios.ServicioEventoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPartidaImpl;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorEventoGlobalWebSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifica commits y rollback reales de Hibernate sin utilizar PostgreSQL. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MovimientosDiferidosEventoGlobalTest.Config.class)
public class MovimientosDiferidosEventoGlobalTest {
    @Configuration
    @EnableTransactionManagement
    @Import({ServicioEventoImpl.class, RepositorioPartidaImpl.class, ObservadorEventoGlobalWebSocket.class})
    static class Config extends HibernateTestConfig {
        @Bean
        @Override
        public DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:hsqldb:mem:movimientos_globales", "sa", "");
        }

        @Bean public RepositorioEvento repositorioEvento() { return mock(RepositorioEvento.class); }
        @Bean public ServicioDado servicioDado() { return mock(ServicioDado.class); }
        @Bean public ServicioGestionQuiebra quiebras() { return mock(ServicioGestionQuiebra.class); }
        @Bean public GestorTemporizadorPartida temporizador() { return mock(GestorTemporizadorPartida.class); }
        @Bean public SimpMessagingTemplate mensajeria() { return mock(SimpMessagingTemplate.class); }
    }

    @Autowired private ServicioEvento eventos;
    @Autowired private ServicioDado dados;
    @Autowired private ServicioGestionQuiebra quiebras;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private SimpMessagingTemplate mensajeria;
    @Autowired private SessionFactory sesiones;
    @Autowired private RepositorioPartida partidas;
    @Autowired private PlatformTransactionManager transacciones;
    private TransactionTemplate transaccion;
    private Partida partida;
    private Long idAna;
    private Long idBeto;
    private final Map<String, Runnable> vencimientos = new HashMap<>();

    @BeforeEach
    public void preparar() {
        transaccion = new TransactionTemplate(transacciones);
        reset(dados, temporizador, mensajeria, quiebras);
        when(temporizador.obtenerSegundosRestantes(anyString())).thenReturn(30);
        doAnswer(invocacion -> {
            vencimientos.put(invocacion.getArgument(0), invocacion.getArgument(2));
            return null;
        }).when(temporizador).programarTemporizador(anyString(), anyInt(), any(Runnable.class));
    }

    private void dadoUnEventoResuelto(TipoEventoGlobal tipo) throws Exception {
        dadoUnEventoResuelto(tipo, 2);
    }

    private void dadoUnEventoResuelto(TipoEventoGlobal tipo, int cantidad) throws Exception {
        String codigo = UUID.randomUUID().toString();
        transaccion.executeWithoutResult(estado -> {
            Usuario usuario = new Usuario();
            usuario.setEmail(codigo + "@test.local");
            usuario.setUsername(codigo);
            sesiones.getCurrentSession().persist(usuario);
            Jugador ana = new Jugador();
            ana.setApodo("Ana");
            ana.setPosicionActual(20);
            Jugador beto = new Jugador();
            beto.setApodo("Beto");
            beto.setPosicionActual(25);
            partida = new Partida();
            partida.setCodigoUnico(codigo);
            partida.setRondaActual(5);
            partida.setCreador(usuario);
            List<Jugador> jugadores = new ArrayList<>(List.of(ana, beto));
            for (int numero = 3; numero <= cantidad; numero++) {
                Jugador jugador = new Jugador();
                jugador.setApodo("Jugador" + numero);
                jugador.setPosicionActual(25);
                jugadores.add(jugador);
            }
            partida.setJugadores(jugadores);
            partidas.guardarPartida(partida);
            idAna = ana.getId();
            idBeto = beto.getId();
        });
        EventoGlobal evento = new EventoGlobal();
        evento.setId(1);
        evento.setTipoEventoGlobal(tipo);
        evento.setTipoEfecto(tipo == TipoEventoGlobal.PIQUETE ? TipoEfecto.VOLVER_INICIO : TipoEfecto.MOVER_CASILLEROS);
        evento.setValor(tipo == TipoEventoGlobal.INUNDACION ? -3 : 0);
        evento.setDescripcion("Movimiento diferido de prueba");
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(4, 3), new ResultadoTirada(3, 3));
        if (tipo == TipoEventoGlobal.PIQUETE) {
            when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 2), new ResultadoTirada(3, 3));
        }
        eventos.iniciarEventoGlobal(partida, evento);
        for (Jugador jugador : partida.getJugadores()) {
            eventos.tirarDadosEventoGlobal(codigo, jugador.getId());
        }
    }

    private Integer posicionPersistida() {
        return transaccion.execute(estado -> partidas.buscarJugadorPorId(idAna).getPosicionActual());
    }

    @Test
    public void dadoQueInundacionEsperaCuandoTodosConfirmanEntoncesPersisteTresCasillerosAtrasUnaSolaVez() throws Exception {
        dadoUnEventoResuelto(TipoEventoGlobal.INUNDACION);
        String codigo = partida.getCodigoUnico();
        assertEquals(20, posicionPersistida());
        eventos.confirmarContinuacion(codigo, idAna);
        eventos.confirmarContinuacion(codigo, idAna);
        assertEquals(List.of(idAna), eventos.consultarEstadoEventoGlobal(codigo).confirmados());
        assertEquals(20, posicionPersistida());
        verify(mensajeria, never()).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));

        transaccion.executeWithoutResult(estado -> {
            eventos.confirmarContinuacion(codigo, idBeto);
            assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        });

        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        assertEquals(17, posicionPersistida());
        assertThrows(IllegalStateException.class, () -> eventos.confirmarContinuacion(codigo, idBeto));
        assertEquals(17, posicionPersistida());
        verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
    }

    @Test
    public void dadoQueElCommitFallaCuandoSeReintentaLaConfirmacionEntoncesConservaElMovimientoPendiente() throws Exception {
        dadoUnEventoResuelto(TipoEventoGlobal.INUNDACION);
        String codigo = partida.getCodigoUnico();
        eventos.confirmarContinuacion(codigo, idAna);

        transaccion.executeWithoutResult(estado -> {
            eventos.confirmarContinuacion(codigo, idBeto);
            estado.setRollbackOnly();
        });

        assertEquals(20, posicionPersistida());
        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        verify(mensajeria, never()).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
        eventos.confirmarContinuacion(codigo, idBeto);
        assertEquals(17, posicionPersistida());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
    }

    @Test
    public void dadoQueAfipYPiqueteEsperanCuandoTodosConfirmanEntoncesConservanSusDestinosPersistidos() throws Exception {
        for (TipoEventoGlobal tipo : List.of(TipoEventoGlobal.TRAMITES_AFIP, TipoEventoGlobal.PIQUETE)) {
            dadoUnEventoResuelto(tipo);
            assertEquals(20, posicionPersistida());

            eventos.confirmarContinuacion(partida.getCodigoUnico(), idAna);
            eventos.confirmarContinuacion(partida.getCodigoUnico(), idBeto);

            assertEquals(tipo == TipoEventoGlobal.PIQUETE ? 0 : 25, posicionPersistida());
        }
    }

    @Test
    public void dadoQueFaltaUnaConfirmacionCuandoVencenLosSesentaSegundosEntoncesPersisteElRetrocesoUnaSolaVez() throws Exception {
        dadoUnEventoResuelto(TipoEventoGlobal.INUNDACION);
        String codigo = partida.getCodigoUnico();
        eventos.confirmarContinuacion(codigo, idAna);
        Runnable vencimiento = vencimientos.get("evento-global-resultado:" + codigo);
        verify(temporizador).programarTemporizador(eq("evento-global-resultado:" + codigo), eq(60), any(Runnable.class));

        Thread.sleep(60_050L);
        when(quiebras.obtenerDeudaPendiente(codigo, idAna)).thenReturn(new DeudaPendiente());
        transaccion.executeWithoutResult(estado -> vencimiento.run());
        assertEquals(20, posicionPersistida());
        assertTrue(eventos.verificarSiHayEventoGlobalActivo(codigo));
        when(quiebras.obtenerDeudaPendiente(codigo, idAna)).thenReturn(null);
        transaccion.executeWithoutResult(estado -> vencimiento.run());
        transaccion.executeWithoutResult(estado -> vencimiento.run());

        assertEquals(17, posicionPersistida());
        assertFalse(eventos.verificarSiHayEventoGlobalActivo(codigo));
        verify(mensajeria, times(1)).convertAndSend(eq("/topic/partida/" + codigo + "/evento-global/reanudado"), any(Object.class));
    }

    /** Usa navegador real y sesiones separadas; MockMvc y un puente STOMP controlado sustituyen la red. */
    @Test
    public void dadoQueDosYCuatroSesionesConfirmanCuandoSeReanudaEntoncesTodasVenElRetrocesoYLosDadosNormales() throws Exception {
        Path navegador = Path.of("C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(navegador), "Microsoft Edge no está instalado");
        Path capturas = Path.of("target/eventos-globales-visual");
        Files.createDirectories(capturas);
        ObjectMapper json = new ObjectMapper();
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setExecutablePath(navegador).setHeadless(true))) {
            for (int cantidad : List.of(2, 4)) {
                dadoUnEventoResuelto(TipoEventoGlobal.INUNDACION, cantidad);
                ServicioPartida servicioPartida = new ServicioPartidaImpl(partidas, mock(ServicioInicioPartida.class),
                        temporizador, new ServicioTurnoImpl(new ServicioParticipacionImpl()), null, null, eventos);
                MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorEventoGlobal(
                        servicioPartida, eventos, new ServicioParticipacionImpl())).build();
                List<Map<String, String>> avisos = new ArrayList<>();
                doAnswer(invocacion -> {
                    avisos.add(Map.of("canal", invocacion.getArgument(0),
                            "cuerpo", json.writeValueAsString(invocacion.getArgument(1))));
                    return null;
                }).when(mensajeria).convertAndSend(anyString(), any(Object.class));
                List<BrowserContext> contextos = new ArrayList<>();
                List<Page> paginas = new ArrayList<>();
                try {
                    for (Jugador jugador : partida.getJugadores()) {
                        BrowserContext contexto = browser.newContext();
                        contextos.add(contexto);
                        Page pagina = contexto.newPage();
                        paginas.add(pagina);
                        MockHttpSession sesion = new MockHttpSession();
                        sesion.setAttribute("NOMBRE", jugador.getApodo());
                        pagina.route("**/*", ruta -> responderNavegador(ruta, mvc, sesion));
                        pagina.navigate("http://monopoly.test/spring/partida?codigo=" + partida.getCodigoUnico());
                        pagina.addStyleTag(new Page.AddStyleTagOptions().setPath(Path.of("src/main/webapp/resources/core/css/partida.css")));
                        pagina.evaluate("""
                                ([id, jugadores]) => {
                                    window.usuarioSesionId = id;
                                    window.turnoActualId = jugadores[0].id;
                                    window.dineroUsuarioSesion = 1000;
                                    document.querySelector('.jugadores').innerHTML = '';
                                    document.querySelectorAll('.ficha').forEach(ficha => ficha.remove());
                                    jugadores.forEach(jugador => {
                                        const tarjeta = document.createElement('article');
                                        tarjeta.className = 'jugador'; tarjeta.id = jugador.id;
                                        tarjeta.innerHTML = '<h3 class="nombre-jugador">' + jugador.apodo + '</h3><span class="dinero">$1000</span>';
                                        document.querySelector('.jugadores').appendChild(tarjeta);
                                        const ficha = document.createElement('div'); ficha.className = 'ficha ficha-rojo';
                                        ficha.dataset.jugadorId = jugador.id;
                                        document.querySelector('[data-casillero="' + jugador.posicion + '"] .fichas-en-casillero').appendChild(ficha);
                                    });
                                    window.suscripcionesPrueba = {};
                                    window.SockJS = function () {};
                                    window.Stomp = {over: () => ({connected: true, send: () => {},
                                        connect: (cabeceras, conectado) => conectado(),
                                        subscribe: (canal, receptor) => { window.suscripcionesPrueba[canal] = receptor; }})};
                                }
                                """, List.of(jugador.getId().toString(), partida.getJugadores().stream()
                                        .map(actual -> Map.of("id", actual.getId().toString(), "apodo", actual.getApodo(), "posicion", actual.getPosicionActual())).toList()));
                        pagina.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/webapp/resources/core/js/partida.js")));
                        pagina.evaluate("posicionesInicialesSincronizadas = true; conectarWebSocketTurnos('/spring', codigoPartida)");
                        pagina.waitForFunction("estadoEventoGlobal?.fase === 'FINALIZADO'");
                        assertFalse(pagina.locator("#btn-tirar").isVisible());
                        assertFalse(pagina.locator(".dados").isVisible());
                    }
                    for (int indice = 0; indice < cantidad; indice++) {
                        Page pagina = paginas.get(indice);
                        pagina.waitForResponse(respuesta -> respuesta.url().endsWith("/evento-global/continuar"),
                                () -> pagina.locator("#btn-continuar-partida-global").click());
                        while (!avisos.isEmpty()) {
                            Map<String, String> aviso = avisos.remove(0);
                            for (Page cliente : paginas) {
                                cliente.evaluate("aviso => window.suscripcionesPrueba[aviso.canal]?.({body: aviso.cuerpo})", aviso);
                            }
                        }
                        if (indice < cantidad - 1) {
                            assertEquals(20, posicionPersistida());
                            for (Page cliente : paginas) {
                                assertTrue(cliente.locator("#modal-evento-global").isVisible());
                                assertFalse(cliente.locator("#btn-tirar").isVisible());
                                assertEquals("PAUSA", cliente.locator("#cronometro-nro").textContent());
                            }
                            if (indice == 0) {
                                paginas.get(0).screenshot(new Page.ScreenshotOptions().setFullPage(true)
                                        .setPath(capturas.resolve(cantidad + "-jugadores-esperando.png")));
                            }
                        }
                    }
                    for (Page pagina : paginas) {
                        pagina.waitForFunction("estadoEventoGlobal === null");
                        assertTrue(pagina.locator("#btn-tirar").isVisible());
                        assertTrue(pagina.locator(".dados").isVisible());
                        assertEquals("17", pagina.locator(".ficha[data-jugador-id='" + idAna + "']")
                                .evaluate("ficha => ficha.closest('.casillero').dataset.casillero"));
                    }
                    assertEquals(17, posicionPersistida());
                    paginas.get(0).screenshot(new Page.ScreenshotOptions().setFullPage(true)
                            .setPath(capturas.resolve(cantidad + "-jugadores-reanudados.png")));
                } finally {
                    contextos.forEach(BrowserContext::close);
                }
            }
        }
    }

    private void responderNavegador(Route ruta, MockMvc mvc, MockHttpSession sesion) {
        String direccion = ruta.request().url();
        try {
            if (direccion.startsWith("https://fonts.googleapis.com/")) {
                ruta.fulfill(new Route.FulfillOptions().setContentType("text/css").setBody(""));
            } else if (direccion.contains("/evento-global")) {
                String destino = URI.create(direccion).getPath().substring("/spring".length());
                MockHttpServletRequestBuilder solicitud = "POST".equals(ruta.request().method())
                        ? MockMvcRequestBuilders.post(destino) : MockMvcRequestBuilders.get(destino);
                MvcResult resultado = transaccion.execute(estado -> {
                    try { return mvc.perform(solicitud.session(sesion)).andReturn(); }
                    catch (Exception exception) { throw new IllegalStateException(exception); }
                });
                ruta.fulfill(new Route.FulfillOptions().setStatus(resultado.getResponse().getStatus())
                        .setContentType("application/json").setBody(resultado.getResponse().getContentAsString()));
            } else if (direccion.contains("/deuda-pendiente")) {
                ruta.fulfill(new Route.FulfillOptions().setContentType("application/json")
                        .setBody("{\"tieneDeudaPendiente\":false,\"montoPendiente\":null,\"motivo\":null,\"acreedor\":null}"));
            } else if (direccion.contains("/turno")) {
                ruta.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody("{\"idJugadorActual\":" + idAna
                        + ",\"duracionSegundos\":30,\"rondaActual\":5,\"turnosFinalizadosEnRonda\":0}"));
            } else if (direccion.contains("/partida?")) {
                String plantilla = Files.readString(Path.of("src/main/webapp/WEB-INF/views/thymeleaf/partida.html"))
                        .replaceAll("(?s)<script\\b[^>]*>.*?</script>", "").replaceAll("<link\\b[^>]*>", "");
                ruta.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(plantilla));
            } else {
                ruta.fulfill(new Route.FulfillOptions().setStatus(404).setBody(""));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Falló la comunicación del navegador de prueba", exception);
        }
    }
}
