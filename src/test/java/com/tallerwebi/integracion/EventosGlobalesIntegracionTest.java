package com.tallerwebi.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioEventoImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.observadores.ObservadorReanudarPartida;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorEventoGlobalWebSocket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EventosGlobalesIntegracionTest.Config.class)
public class EventosGlobalesIntegracionTest {

    @Configuration
    @EnableTransactionManagement
    @Import({ServicioEventoImpl.class, ServicioPartidaImpl.class, ServicioTurnoImpl.class, ServicioParticipacionImpl.class, ControladorEventoGlobal.class, ObservadorEventoGlobalWebSocket.class, ObservadorReanudarPartida.class})
    static class Config {

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUrl("jdbc:hsqldb:mem:eventos_globales_integracion");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public RepositorioPartida repositorioPartida() {
            return mock(RepositorioPartida.class);
        }

        @Bean
        public RepositorioEvento repositorioEvento() {
            return mock(RepositorioEvento.class);
        }

        @Bean
        public ServicioDado servicioDado() {
            return mock(ServicioDado.class);
        }

        @Bean
        public ServicioInicioPartida servicioInicioPartida() {
            return mock(ServicioInicioPartida.class);
        }

        @Bean
        public GestorTemporizadorPartida gestorTemporizador() {
            return mock(GestorTemporizadorPartida.class);
        }

        @Bean
        public SimpMessagingTemplate mensajeriaWebSocket() {
            return mock(SimpMessagingTemplate.class);
        }

        @Bean
        public PublicadorEventos<EventoTurnoCambiado> publicadorTurno() {
            return new PublicadorEventos<>(List.of());
        }

        @Bean
        public PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono() {
            return new PublicadorEventos<>(List.of());
        }
    }

    @Autowired private ServicioEvento servicioEvento;
    @Autowired private ServicioPartida servicioPartida;
    @Autowired private ControladorEventoGlobal controladorEventoGlobal;
    @Autowired private RepositorioEvento repositorioEventoMock;
    @Autowired private RepositorioPartida repositorioPartidaMock;
    @Autowired private ServicioDado servicioDadoMock;
    @Autowired private GestorTemporizadorPartida gestorTemporizadorMock;
    @Autowired private SimpMessagingTemplate mensajeriaWebSocketMock;

    private MockMvc mockMvc;
    private Partida partida;
    private EventoGlobal eventoGlobal;
    private String codigoPartida;
    private String canalEvento;
    private Jugador jugadorUno;
    private Jugador jugadorDos;

    @BeforeEach
    public void init() {
        reset(repositorioEventoMock, repositorioPartidaMock, servicioDadoMock, gestorTemporizadorMock, mensajeriaWebSocketMock);
        mockMvc = MockMvcBuilders.standaloneSetup(controladorEventoGlobal).build();
        codigoPartida = UUID.randomUUID().toString();
        canalEvento = "/topic/partida/" + codigoPartida + "/evento-global";

        jugadorUno = new Jugador();
        jugadorUno.setId(1L);
        jugadorUno.setApodo("Ana");
        jugadorUno.setOrden(0);
        jugadorUno.setPosicionActual(2);

        jugadorDos = new Jugador();
        jugadorDos.setId(2L);
        jugadorDos.setApodo("Beto");
        jugadorDos.setOrden(1);
        jugadorDos.setPosicionActual(7);

        partida = new Partida();
        partida.setCodigoUnico(codigoPartida);
        partida.setJugadores(List.of(jugadorUno, jugadorDos));
        partida.setIndiceTurnoActual(0);

        eventoGlobal = new EventoGlobal();
        eventoGlobal.setId(100L);
        eventoGlobal.setDescripcion("Trámites en AFIP");
        eventoGlobal.setTipoEventoGlobal(TipoEventoGlobal.TRAMITES_AFIP);
        eventoGlobal.setTipoEfecto(TipoEfecto.MOVER_CASILLEROS);
        eventoGlobal.setValor(0);

        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPartida)).thenReturn(partida);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(eventoGlobal);
        when(gestorTemporizadorMock.obtenerSegundosRestantes("evento-global:" + codigoPartida)).thenReturn(15);
        when(gestorTemporizadorMock.programarTemporizadorSiNoExiste(eq(codigoPartida), eq(30), any(Runnable.class))).thenReturn(true);
    }

    @Test
    public void dadoQueSeCompletaLaQuintaRondaCuandoAvanzaElTurnoYDaEventoGlobalActivoSinIniciarTurnoNormal() throws Exception {
        partida.setRondaActual(4);
        partida.setTurnosFinalizadosEnRonda(1);

        servicioPartida.avanzarTurno(codigoPartida);

        assertEquals(5, partida.getRondaActual());
        assertTrue(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        assertEquals(1, partida.getIndiceTurnoActual());
        verify(gestorTemporizadorMock).programarTemporizador(eq("evento-global:" + codigoPartida), eq(15), any(Runnable.class));
        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(codigoPartida), any(Integer.class), any(Runnable.class));
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "INICIADO".equals(estado.fase())));

        String respuesta = mockMvc.perform(get("/partida/{codigoPartida}/evento-global", codigoPartida).sessionAttr("NOMBRE", "Ana"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode contenido = new ObjectMapper().readTree(respuesta);

        assertEquals("INICIADO", contenido.path("fase").asText());
        assertEquals(15, contenido.path("segundosRestantes").asInt());
    }

    @Test
    public void dadoQueHayUnEventoActivoCuandoSeIntentaAvanzarTurnoYDaPartidaPausada() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);
        reset(gestorTemporizadorMock);
        Integer indiceAnterior = partida.getIndiceTurnoActual();
        Integer rondaAnterior = partida.getRondaActual();

        servicioPartida.avanzarTurno(codigoPartida);

        assertEquals(indiceAnterior, partida.getIndiceTurnoActual());
        assertEquals(rondaAnterior, partida.getRondaActual());
        verify(repositorioPartidaMock, never()).guardarPartida(any(Partida.class));
        verify(gestorTemporizadorMock, never()).programarTemporizador(eq(codigoPartida), any(Integer.class), any(Runnable.class));
    }

    @Test
    public void dadoQueElEventoEstaActivoCuandoUnJugadorTiraPorRestYDaResultadoYNotificacionWebSocket() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(2, 3));

        String respuesta = mockMvc.perform(post("/partida/{codigoPartida}/evento-global/tirar", codigoPartida).sessionAttr("NOMBRE", "Ana"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode contenido = new ObjectMapper().readTree(respuesta);

        assertEquals(2, contenido.path("dado1").asInt());
        assertEquals(3, contenido.path("dado2").asInt());
        assertEquals(5, contenido.path("suma").asInt());

        assertTrue(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "TIRADA".equals(estado.fase()) && Integer.valueOf(5).equals(estado.tiradas().get(1L))));
        verify(gestorTemporizadorMock, never()).programarTemporizadorSiNoExiste(eq(codigoPartida), eq(30), any(Runnable.class));
    }

    @Test
    public void dadoQueElJugadorYaTiroCuandoIntentaRepetirLaTiradaYDaConflict() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(2, 4));

        realizarTiradaRest("Ana").andExpect(status().isOk());
        realizarTiradaRest("Ana").andExpect(status().isConflict());

        verify(servicioDadoMock, times(1)).tirarDados();
        assertTrue(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
    }

    @Test
    public void dadoQueNoHayEventoActivoCuandoSeConsultaPorRestYDaNoContent() throws Exception {
        mockMvc.perform(get("/partida/{codigoPartida}/evento-global", codigoPartida).sessionAttr("NOMBRE", "Ana"))
                .andExpect(status().isNoContent());

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueTodosLosJugadoresTiranCuandoSeResuelveElEventoYDaFinalizacionYReanudacion() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 3), new ResultadoTirada(1, 2));

        realizarTiradaRest("Ana").andExpect(status().isOk());
        realizarTiradaRest("Beto").andExpect(status().isOk());

        assertFalse(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        assertEquals(7, jugadorUno.getPosicionActual());
        assertEquals(7, jugadorDos.getPosicionActual());
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "FINALIZADO".equals(estado.fase()) && Long.valueOf(1L).equals(estado.idJugadorElegido())));
        verify(gestorTemporizadorMock).cancelarTemporizador("evento-global:" + codigoPartida);
        verify(gestorTemporizadorMock, times(1)).programarTemporizadorSiNoExiste(eq(codigoPartida), eq(30), any(Runnable.class));
    }

    @Test
    public void dadoQueLosJugadoresEmpatanCuandoVuelvenATirarYDaDesempateYFinalizacion() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(3, 3), new ResultadoTirada(2, 4), new ResultadoTirada(5, 5), new ResultadoTirada(1, 2));

        realizarTiradaRest("Ana").andExpect(status().isOk());
        realizarTiradaRest("Beto").andExpect(status().isOk());

        assertTrue(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        assertEquals(1, servicioEvento.consultarEstadoEventoGlobal(codigoPartida).etapa());
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "DESEMPATE".equals(estado.fase()) && estado.participantes().size() == 2));

        realizarTiradaRest("Ana").andExpect(status().isOk());
        realizarTiradaRest("Beto").andExpect(status().isOk());

        assertFalse(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "FINALIZADO".equals(estado.fase()) && Long.valueOf(1L).equals(estado.idJugadorElegido())));
        verify(gestorTemporizadorMock, times(2)).programarTemporizador(eq("evento-global:" + codigoPartida), eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueVenceElTemporizadorCuandoFaltanTiradasYDaResolucionAutomatica() throws Exception {
        AtomicReference<Runnable> accionVencimiento = new AtomicReference<>();
        doAnswer(invocacion -> {
            accionVencimiento.set(invocacion.getArgument(2));
            return null;
        }).when(gestorTemporizadorMock).programarTemporizador(eq("evento-global:" + codigoPartida), eq(15), any(Runnable.class));
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(5, 4), new ResultadoTirada(1, 1));
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);

        assertNotNull(accionVencimiento.get());
        accionVencimiento.get().run();

        assertFalse(servicioEvento.verificarSiHayEventoGlobalActivo(codigoPartida));
        verify(servicioDadoMock, times(2)).tirarDados();
        verify(mensajeriaWebSocketMock).convertAndSend(eq(canalEvento), (Object) argThat((EstadoEventoGlobal estado) -> estado != null && "FINALIZADO".equals(estado.fase())));
        verify(gestorTemporizadorMock).programarTemporizadorSiNoExiste(eq(codigoPartida), eq(30), any(Runnable.class));
    }

    @Test
    public void dadoQueNoHayUnaSesionAutenticadaCuandoSeIntentaTirarYDaUnauthorized() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);

        mockMvc.perform(post("/partida/{codigoPartida}/evento-global/tirar", codigoPartida))
                .andExpect(status().isUnauthorized());

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueElUsuarioNoPerteneceALaPartidaCuandoIntentaTirarYDaForbidden() throws Exception {
        servicioEvento.iniciarEventoGlobal(partida, eventoGlobal);

        mockMvc.perform(post("/partida/{codigoPartida}/evento-global/tirar", codigoPartida).sessionAttr("NOMBRE", "PersonaAjena"))
                .andExpect(status().isForbidden());

        verify(servicioDadoMock, never()).tirarDados();
    }

    private org.springframework.test.web.servlet.ResultActions realizarTiradaRest(String nombre) throws Exception {
        return mockMvc.perform(post("/partida/{codigoPartida}/evento-global/tirar", codigoPartida).sessionAttr("NOMBRE", nombre));
    }
}
