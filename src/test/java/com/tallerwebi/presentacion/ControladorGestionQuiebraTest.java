package com.tallerwebi.presentacion;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import com.tallerwebi.dominio.servicios.ServicioJugadaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl.DeclaracionQuiebraInvalidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class ControladorGestionQuiebraTest {
    private final com.tallerwebi.dominio.servicios.interfaces.ServicioDado dados = org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioDado.class);
    private final ServicioEconomia economia = org.mockito.Mockito.spy(new ServicioEconomiaImpl());
    private final ServicioMovimiento movimiento = org.mockito.Mockito.spy(new ServicioMovimientoImpl(economia));

    private ServicioPartida servicioPartidaMock;
    private ServicioGestionQuiebra servicioGestionQuiebraMock;
    private ServicioPropiedad servicioPropiedadMock;
    private NotificadorPartida notificadorPartidaMock;
    private SimpMessagingTemplate notificadorWebSocketMock;
    private ControladorGestionQuiebra controladorGestionQuiebra;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    void preparar() {
        servicioPartidaMock = mock(ServicioPartida.class);
        servicioGestionQuiebraMock = mock(ServicioGestionQuiebra.class);
        servicioPropiedadMock = mock(ServicioPropiedad.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        notificadorWebSocketMock = mock(SimpMessagingTemplate.class);
        controladorGestionQuiebra = new ControladorGestionQuiebra(servicioPartidaMock,
                servicioGestionQuiebraMock, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
    }

    @Test
    void queSpringRegistreLasRutasDeDeudaYQuiebraUnaSolaVez() {
        ControladorPartida controladorPartida = new ControladorPartida(servicioPartidaMock,
                notificadorWebSocketMock,
                mock(ServicioCasillero.class),
                servicioPropiedadMock,
                new com.tallerwebi.presentacion.WebSocket.NotificadorPartida(notificadorWebSocketMock),
                new ServicioJugadaImpl(servicioPartidaMock, mock(ServicioCasillero.class), servicioPropiedadMock, null, org.mockito.Mockito.mock(com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra.class), movimiento, new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl(), dados), new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controladorPartida, controladorGestionQuiebra).build();
        RequestMappingHandlerMapping rutas = mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);
        for (String ruta : List.of("/partida/{codigoPartida}/deuda-pendiente",
                "/partida/{codigoPartida}/declarar-quiebra")) {
            var coincidencias = rutas.getHandlerMethods().entrySet().stream()
                    .filter(entrada -> entrada.getKey().getPatternValues().contains(ruta)).toList();
            assertEquals(1, coincidencias.size());
            assertEquals(ControladorGestionQuiebra.class, coincidencias.get(0).getValue().getBeanType());
        }
    }

    @Test
    void queElPostDeDeclararQuiebraRespondaJsonYUseLaSesion() throws Exception {
        prepararSesionParaQuiebra("id", 1L);
        org.springframework.test.web.servlet.MockMvc mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controladorGestionQuiebra).build();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/partida/PARTIDA123/declarar-quiebra")
                        .session(session).param("idJugador", "2"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith("application/json"))
                .andExpect(resultado -> assertEquals("Te declaraste en quiebra.",
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree(resultado.getResponse().getContentAsByteArray()).get("mensaje").asText()));
        verify(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
        verify(servicioGestionQuiebraMock, never()).declararQuiebra(anyString(), eq(2L));
    }

    @Test
    void queDeclararQuiebraUseElJugadorDeSesionAunqueLaPeticionIndiqueOtro() throws PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 1L);
        request.addParameter("idJugador", "2");

        ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.declararQuiebra("PARTIDA123", request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Te declaraste en quiebra.", respuesta.getBody().get("mensaje"));
        verify(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
        verify(servicioGestionQuiebraMock, never()).declararQuiebra(anyString(), eq(2L));
        verifyNoInteractions(notificadorPartidaMock);
        verifyNoInteractions(servicioPropiedadMock, notificadorWebSocketMock);
        verify(servicioPartidaMock, never()).abandonarPartida(anyString(), anyLong());
        verify(servicioPartidaMock, never()).avanzarTurno(anyString());
    }

    @Test
    void queDeclararQuiebraResuelvaLaSesionConIDUSUARIO() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("IDUSUARIO", 1L);
        assertEquals(HttpStatus.OK, controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());
        verify(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
    }

    @Test
    void queDeclararQuiebraResuelvaLaSesionConNombre() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("NOMBRE", "Ana");
        assertEquals(HttpStatus.OK, controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());
        verify(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
    }

    @Test
    void queDeclararQuiebraSinSesionDevuelvaUnauthorized() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        assertEquals(HttpStatus.UNAUTHORIZED,
                controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());
        verifyNoInteractions(servicioGestionQuiebraMock, servicioPartidaMock);
    }

    @Test
    void queDeclararQuiebraEnPartidaInexistenteDevuelvaNotFound() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        session.setAttribute("id", 1L);
        assertEquals(HttpStatus.NOT_FOUND,
                controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());
        verifyNoInteractions(servicioGestionQuiebraMock);
    }

    @Test
    void queDeclararQuiebraDeUnJugadorAjenoDevuelvaForbidden() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 99L);
        assertEquals(HttpStatus.FORBIDDEN,
                controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());
        verifyNoInteractions(servicioGestionQuiebraMock);
    }

    @Test
    void queUnaDeclaracionInvalidaDevuelvaConflictConElMotivo() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 1L);
        for (String motivo : List.of("La partida no está en curso.", "El jugador no tiene un pago pendiente.",
                "El jugador ya abandonó o está en quiebra.")) {
            doThrow(new DeclaracionQuiebraInvalidaException(motivo))
                    .when(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
            ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.declararQuiebra("PARTIDA123", request);
            assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
            assertEquals(motivo, respuesta.getBody().get("error"));
            verifyNoInteractions(notificadorPartidaMock);
        }
    }

    @Test
    void queUnErrorInternoAlDeclararQuiebraNoSeConviertaEnConflict() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 1L);
        IllegalStateException fallo = new IllegalStateException("Fallo interno");
        doThrow(fallo).when(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
        assertSame(fallo, assertThrows(IllegalStateException.class,
                () -> controladorGestionQuiebra.declararQuiebra("PARTIDA123", request)));
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    void queElControladorNoDupliqueLaNotificacionDelObservador() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 1L);
        Partida confirmada = new Partida();
        confirmada.setEstado(EstadoPartida.FINALIZADA);
        when(servicioGestionQuiebraMock.declararQuiebra("PARTIDA123", 1L)).thenReturn(confirmada);

        assertEquals(HttpStatus.OK, controladorGestionQuiebra.declararQuiebra("PARTIDA123", request).getStatusCode());

        verify(servicioGestionQuiebraMock).declararQuiebra("PARTIDA123", 1L);
        verifyNoInteractions(notificadorPartidaMock);
    }

    @Test
    void queLaConsultaDeDeudaPorAlquilerDevuelvaSoloElImporteMotivoYAcreedor() throws Exception {
        prepararSesionParaQuiebra("id", 1L);
        Jugador acreedor = new Jugador();
        acreedor.setId(2L);
        acreedor.setApodo("José");
        acreedor.setDinero(9000);
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(200);
        deuda.setMotivo("ALQUILER");
        deuda.setJugadorAcreedor(acreedor);
        when(servicioGestionQuiebraMock.obtenerDeudaPendiente("PARTIDA123", 1L)).thenReturn(deuda);

        verificarJsonDeDeuda("{\"tieneDeudaPendiente\":true,\"montoPendiente\":200,"
                + "\"motivo\":\"ALQUILER\",\"acreedor\":{\"idJugador\":2,\"apodo\":\"José\"}}");
    }

    @Test
    void queLaConsultaDeDeudaPorEventoRestaDevuelvaAcreedorNulo() throws Exception {
        prepararSesionParaQuiebra("id", 1L);
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(300);
        deuda.setMotivo("EVENTO_RESTA");
        when(servicioGestionQuiebraMock.obtenerDeudaPendiente("PARTIDA123", 1L)).thenReturn(deuda);

        verificarJsonDeDeuda("{\"tieneDeudaPendiente\":true,\"montoPendiente\":300,"
                + "\"motivo\":\"EVENTO_RESTA\",\"acreedor\":null}");
    }

    @Test
    void queLaConsultaSinDeudaDevuelvaFalseYLosDemasCamposNulos() throws Exception {
        prepararSesionParaQuiebra("id", 1L);
        verificarJsonDeDeuda("{\"tieneDeudaPendiente\":false,\"montoPendiente\":null,"
                + "\"motivo\":null,\"acreedor\":null}");
    }

    @Test
    void queLaConsultaDeDeudaSinSesionDevuelvaUnauthorized() {
        ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.obtenerDeudaPendiente("PARTIDA123", request);
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Debés iniciar sesión.", respuesta.getBody().get("error"));
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verifyNoInteractions(servicioPartidaMock, servicioGestionQuiebraMock);
    }

    @Test
    void queLaConsultaDeDeudaDeUnJugadorAjenoDevuelvaForbidden() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 99L);
        ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.obtenerDeudaPendiente("PARTIDA123", request);
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("No pertenecés a esta partida.", respuesta.getBody().get("error"));
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verifyNoInteractions(servicioGestionQuiebraMock);
    }

    @Test
    void queLaConsultaDeDeudaDeUnaPartidaNoDisponibleDevuelvaNotFound() {
        session.setAttribute("id", 1L);
        ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.obtenerDeudaPendiente("PARTIDA123", request);
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("La partida no existe o ya finalizó.", respuesta.getBody().get("error"));
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verifyNoInteractions(servicioGestionQuiebraMock);
    }

    @Test
    void queLaConsultaDeDeudaResuelvaLasVariantesDeIdentidadDeSesion() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        for (String atributo : List.of("id", "IDUSUARIO", "NOMBRE")) {
            prepararSesionParaQuiebra(atributo, atributo.equals("NOMBRE") ? "Ana" : 1L);
            assertEquals(HttpStatus.OK,
                    controladorGestionQuiebra.obtenerDeudaPendiente("PARTIDA123", request).getStatusCode());
            session.removeAttribute(atributo);
        }
        verify(servicioGestionQuiebraMock, times(3)).obtenerDeudaPendiente("PARTIDA123", 1L);
    }

    @Test
    void queElGetDeDeudaIgnoreElJugadorIndicadoEnLaPeticion() throws Exception {
        prepararSesionParaQuiebra("id", 1L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controladorGestionQuiebra).build();
        mvc.perform(get("/partida/PARTIDA123/deuda-pendiente").session(session).param("idJugador", "2"))
                .andExpect(status().isOk());
        verify(servicioGestionQuiebraMock).obtenerDeudaPendiente("PARTIDA123", 1L);
        verify(servicioGestionQuiebraMock, never()).obtenerDeudaPendiente(anyString(), eq(2L));
    }

    @Test
    void queLaDeudaPuedaConsultarseFueraDeTurnoSinCambiarLaPartida() throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        prepararSesionParaQuiebra("id", 1L);
        Partida partida = servicioPartidaMock.obtenerPartida("PARTIDA123");
        Jugador jugadorSesion = partida.getJugadores().get(0);
        jugadorSesion.setOrden(0);
        Jugador jugadorDelTurno = new Jugador();
        jugadorDelTurno.setId(2L);
        jugadorDelTurno.setOrden(1);
        partida.setJugadores(List.of(jugadorSesion, jugadorDelTurno));
        partida.setIndiceTurnoActual(1);
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(200);
        deuda.setMotivo("EVENTO_RESTA");
        when(servicioGestionQuiebraMock.obtenerDeudaPendiente("PARTIDA123", 1L)).thenReturn(deuda);

        ResponseEntity<Map<String, Object>> respuesta = controladorGestionQuiebra.obtenerDeudaPendiente("PARTIDA123", request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(true, respuesta.getBody().get("tieneDeudaPendiente"));
        assertEquals(1, partida.getIndiceTurnoActual());
        verify(servicioGestionQuiebraMock).obtenerDeudaPendiente("PARTIDA123", 1L);
        org.mockito.Mockito.verifyNoMoreInteractions(servicioGestionQuiebraMock);
        verifyNoInteractions(servicioPropiedadMock, notificadorPartidaMock, notificadorWebSocketMock);
    }

    private void verificarJsonDeDeuda(String esperado) throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controladorGestionQuiebra).build();
        mvc.perform(get("/partida/PARTIDA123/deuda-pendiente").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(resultado -> {
                    ObjectMapper mapper = new ObjectMapper();
                    assertEquals(mapper.readTree(esperado),
                            mapper.readTree(resultado.getResponse().getContentAsByteArray()));
                });
    }

    // Prepara la identidad de sesión y una partida para probar el endpoint.
    private void prepararSesionParaQuiebra(String atributo, Object valor) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        session.setAttribute(atributo, valor);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setCodigoUnico("PARTIDA123");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(jugador));
        when(servicioPartidaMock.obtenerPartida("PARTIDA123")).thenReturn(partida);
        when(servicioGestionQuiebraMock.declararQuiebra("PARTIDA123", 1L)).thenReturn(partida);
    }
}
