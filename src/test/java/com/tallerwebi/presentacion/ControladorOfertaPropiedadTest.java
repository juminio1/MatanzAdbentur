package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.servicios.ServicioOfertaPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ControladorOfertaPropiedadTest {

    private static final String CODIGO = "PARTIDA123";
    private static final Long ID_PARTIDA = 10L;
    private static final Long ID_VENDEDOR = 1L;
    private static final Long ID_COMPRADOR = 2L;
    private static final Integer POSICION = 1;
    private static final String TOPIC_OFERTAS = "/topic/partida/" + CODIGO + "/ofertas";
    private ServicioPartida servicioPartida;
    private ServicioCasillero servicioCasillero;
    private ServicioPropiedad servicioPropiedad;
    private ServicioGestionQuiebra servicioGestionQuiebra;
    private SimpMessagingTemplate notificador;
    private ControladorOfertaPropiedad controlador;
    private Partida partida;
    private Jugador vendedor;
    private Jugador comprador;
    private Propiedad propiedad;
    private CasilleroPropiedad casillero;

    @BeforeEach
    public void inicializar() {
        servicioPartida = mock(ServicioPartida.class);
        servicioCasillero = mock(ServicioCasillero.class);
        servicioPropiedad = mock(ServicioPropiedad.class);
        servicioGestionQuiebra = mock(ServicioGestionQuiebra.class);
        notificador = mock(SimpMessagingTemplate.class);
        ServicioParticipacionImpl participacion = new ServicioParticipacionImpl();
        controlador = new ControladorOfertaPropiedad(servicioPartida, notificador, servicioCasillero,
                servicioPropiedad, servicioGestionQuiebra, participacion,
                new ServicioOfertaPropiedadImpl(servicioPartida, servicioCasillero, servicioPropiedad, participacion));
        partida = new Partida();
        partida.setId(ID_PARTIDA);
        partida.setCodigoUnico(CODIGO);
        vendedor = crearJugador(ID_VENDEDOR, "Ana");
        comprador = crearJugador(ID_COMPRADOR, "Luna");
        partida.setJugadores(List.of(vendedor, comprador));
        propiedad = new Propiedad(50L, "Plaza Barrio Marina", 300, 50, vendedor,
                GrupoPropiedades.VILLA_LUZURIAGA);
        casillero = new CasilleroPropiedad(50L, POSICION, "Plaza Barrio Marina",
                "Villa Luzuriaga", propiedad);
        casillero.setPartidaId(ID_PARTIDA);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(ID_VENDEDOR)).thenReturn(vendedor);
        when(servicioPartida.obtenerJugadorPorId(ID_COMPRADOR)).thenReturn(comprador);
        when(servicioCasillero.existePosicionDePropiedad(partida, POSICION)).thenReturn(true);
        when(servicioCasillero.buscarCasilleroPropiedad(partida, POSICION)).thenReturn(casillero);
        when(servicioCasillero.obtenerDatosCatalogoPropiedad(POSICION))
                .thenReturn(List.of("Plaza Barrio Marina", "Villa Luzuriaga", "VILLA_LUZURIAGA"));
    }

    @Test
    public void lasRutasDeOfertaTienenUnSoloControladorRegistrado() {
        ControladorPropiedadesPartida propiedades = new ControladorPropiedadesPartida(servicioPartida,
                notificador, servicioCasillero, servicioPropiedad, servicioGestionQuiebra,
                new ServicioParticipacionImpl());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controlador, propiedades).build();
        RequestMappingHandlerMapping rutas = mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);

        assertEquals(3L, rutas.getHandlerMethods().values().stream()
                .filter(metodo -> metodo.getBeanType().equals(ControladorOfertaPropiedad.class))
                .count());
    }

    @Test
    public void creaOfertaCuandoElPropietarioOfreceAOtroJugadorDeLaPartida() {
        ResponseEntity<Map<String, Object>> respuesta = crearOfertaDeVendedor(450);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("pendiente", respuesta.getBody().get("estado"));
        assertNotNull(respuesta.getBody().get("idOferta"));
    }

    @Test
    public void avisaLaOfertaNuevaPorWebSocketConElCompradorComoDestinatario() {
        AtomicReference<Map<?, ?>> mensaje = new AtomicReference<>();
        doAnswer(invocacion -> {
            mensaje.set(invocacion.getArgument(1, Map.class));
            return null;
        }).when(notificador).convertAndSend(eq(TOPIC_OFERTAS), any(Map.class));

        ResponseEntity<Map<String, Object>> respuesta = crearOfertaDeVendedor(450);

        verify(notificador).convertAndSend(eq(TOPIC_OFERTAS), any(Map.class));
        Map<?, ?> aviso = mensaje.get();
        assertNotNull(aviso);
        assertEquals(respuesta.getBody().get("idOferta"), aviso.get("idOferta"));
        assertEquals(ID_COMPRADOR, aviso.get("idComprador"));
        assertEquals("Ana", aviso.get("apodoVendedor"));
        assertEquals("Plaza Barrio Marina", aviso.get("nombrePropiedad"));
        assertEquals(450, aviso.get("precio"));
    }

    @Test
    public void siFallaElAvisoPorWebSocketLaOfertaQuedaPendienteIgual() {
        doThrow(new IllegalStateException("Sin conexión")).when(notificador)
                .convertAndSend(eq(TOPIC_OFERTAS), any(Map.class));

        ResponseEntity<Map<String, Object>> respuesta = crearOfertaDeVendedor(450);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(1, ((List<?>) controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR)).getBody()).size());
    }

    @Test
    public void rechazaOfertaInvalidaYSolicitudesDePartidaInexistente() {
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, crearOfertaDeVendedor(450).getStatusCode());

        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        ResponseEntity<Map<String, Object>> respuesta = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_VENDEDOR, "precio", 450), sesion(ID_VENDEDOR));
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, crearOfertaDeVendedor(-1).getStatusCode());
        verify(notificador, never()).convertAndSend(eq(TOPIC_OFERTAS), any(Map.class));
    }

    @Test
    public void rechazaOfertaDeUnaPropiedadQueNoEsDelVendedor() {
        propiedad.setPropietario(comprador);

        ResponseEntity<Map<String, Object>> respuesta = crearOfertaDeVendedor(450);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test
    public void listaOfertaPendienteSoloParaElJugadorComprador() {
        crearOfertaDeVendedor(450);

        ResponseEntity<?> respuesta = controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        List<?> ofertas = (List<?>) respuesta.getBody();
        assertEquals(1, ofertas.size());
        Map<?, ?> oferta = (Map<?, ?>) ofertas.get(0);
        assertEquals("Plaza Barrio Marina", oferta.get("nombrePropiedad"));
        assertEquals("Ana", oferta.get("apodoVendedor"));
        assertEquals(450, oferta.get("precio"));
        assertEquals(0, ((List<?>) controlador.ofertasPendientes(CODIGO, sesion(ID_VENDEDOR)).getBody()).size());
    }

    @Test
    public void rechazaUnaOfertaPendienteYLaQuitaDeLaLista() {
        String idOferta = crearOfertaDeVendedor(450).getBody().get("idOferta").toString();

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", false), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("rechazada", respuesta.getBody().get("estado"));
        assertEquals(0, ((List<?>) controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR)).getBody()).size());
    }

    @Test
    public void aceptaOfertaTransfiereLaPropiedadYNotificaLosSaldos() {
        String idOferta = crearOfertaDeVendedor(450).getBody().get("idOferta").toString();
        doAnswer(invocacion -> {
            propiedad.setPropietario(comprador);
            return null;
        }).when(servicioPropiedad).transferirPropiedadParaJugadores(50L, ID_VENDEDOR, ID_COMPRADOR, 450);
        when(servicioPropiedad.obtenerPropiedadExistente(50L)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(75);

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode(), () -> String.valueOf(respuesta.getBody()));
        assertEquals(ID_COMPRADOR, respuesta.getBody().get("propietarioId"));
        assertEquals(450, respuesta.getBody().get("precioVenta"));
        assertEquals(75, respuesta.getBody().get("alquiler"));
        assertEquals(ID_VENDEDOR, respuesta.getBody().get("idVendedor"));
        verify(servicioGestionQuiebra).intentarResolverDeuda(CODIGO, ID_VENDEDOR);
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
        verify(notificador, times(2)).convertAndSend(eq("/topic/partida/" + CODIGO + "/dinero"), any(Map.class));
    }

    @Test
    public void siLaTransferenciaFallaInformaConflictoYLaOfertaSigueDisponible() {
        String idOferta = crearOfertaDeVendedor(450).getBody().get("idOferta").toString();
        doThrow(new IllegalStateException("El comprador no tiene dinero suficiente para aceptar la oferta."))
                .when(servicioPropiedad).transferirPropiedadParaJugadores(50L, ID_VENDEDOR, ID_COMPRADOR, 450);

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("El comprador no tiene dinero suficiente para aceptar la oferta.",
                respuesta.getBody().get("error"));
        assertEquals(1, ((List<?>) controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR)).getBody()).size());
    }

    @Test
    public void elCompradorEnBancarrotaNoPuedeAceptarLaOferta() {
        String idOferta = crearOfertaDeVendedor(450).getBody().get("idOferta").toString();
        comprador.setEnBancarrota(true);

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        verify(servicioPropiedad, never()).transferirPropiedadParaJugadores(any(), any(), any(), any());
    }

    @Test
    public void informaConflictoSiLaOfertaYaNoExiste() {
        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, "inexistente",
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    private ResponseEntity<Map<String, Object>> crearOfertaDeVendedor(Integer precio) {
        return controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", precio), sesion(ID_VENDEDOR));
    }

    private MockHttpServletRequest sesion(Long idJugador) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", idJugador);
        return request;
    }

    private Jugador crearJugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        jugador.setDinero(1000);
        return jugador;
    }
}
