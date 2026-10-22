package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControladorOfertaPropiedadTest {

    private static final String CODIGO = "PARTIDA123";
    private static final Long ID_PARTIDA = 10L;
    private static final Long ID_VENDEDOR = 1L;
    private static final Long ID_COMPRADOR = 2L;
    private static final Integer POSICION = 1;
    private ServicioPartida servicioPartida;
    private ServicioCasillero servicioCasillero;
    private ServicioPropiedad servicioPropiedad;
    private SimpMessagingTemplate notificador;
    private ControladorOfertaPropiedad controlador;
    private Partida partida;
    private Jugador vendedor;
    private Jugador comprador;
    private Propiedad propiedad;
    private CasilleroPropiedad casillero;

    @BeforeEach
    void inicializar() {
        servicioPartida = mock(ServicioPartida.class);
        servicioCasillero = mock(ServicioCasillero.class);
        servicioPropiedad = mock(ServicioPropiedad.class);
        notificador = mock(SimpMessagingTemplate.class);
        controlador = new ControladorOfertaPropiedad(servicioPartida, servicioCasillero,
                servicioPropiedad, notificador);
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
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
    }

    @Test
    void creaOfertaCuandoElPropietarioOfreceAOtroJugadorDeLaPartida() {
        ResponseEntity<Map<String, Object>> respuesta = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("pendiente", respuesta.getBody().get("estado"));
        assertNotNull(respuesta.getBody().get("idOferta"));
    }

    @Test
    void rechazaOfertaInvalidaYSolicitudesDePartidaInexistente() {
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR))
                .getStatusCode());

        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        ResponseEntity<Map<String, Object>> respuesta = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_VENDEDOR, "precio", 450), sesion(ID_VENDEDOR));
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test
    void rechazaOfertaDeUnaPropiedadQueNoEsDelVendedor() {
        propiedad.setPropietario(comprador);

        ResponseEntity<Map<String, Object>> respuesta = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test
    void listaOfertaPendienteSoloParaElJugadorComprador() {
        controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR));

        ResponseEntity<?> respuesta = controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        List<?> ofertas = (List<?>) respuesta.getBody();
        assertEquals(1, ofertas.size());
        Map<?, ?> oferta = (Map<?, ?>) ofertas.get(0);
        assertEquals("Plaza Barrio Marina", oferta.get("nombrePropiedad"));
        assertEquals("Ana", oferta.get("apodoVendedor"));
        assertEquals(450, oferta.get("precio"));
    }

    @Test
    void rechazaUnaOfertaPendienteYLaQuitaDeLaLista() {
        ResponseEntity<Map<String, Object>> creada = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR));
        String idOferta = creada.getBody().get("idOferta").toString();

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", false), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("rechazada", respuesta.getBody().get("estado"));
        assertEquals(0, ((List<?>) controlador.ofertasPendientes(CODIGO, sesion(ID_COMPRADOR)).getBody()).size());
    }

    @Test
    void aceptaOfertaTransfiereLaPropiedadYNotificaLosSaldos() {
        ResponseEntity<Map<String, Object>> creada = controlador.crearOferta(CODIGO, POSICION,
                Map.of("idJugadorComprador", ID_COMPRADOR, "precio", 450), sesion(ID_VENDEDOR));
        String idOferta = creada.getBody().get("idOferta").toString();
        doAnswer(invocacion -> {
            propiedad.setPropietario(comprador);
            return null;
        }).when(servicioPropiedad).transferirPropiedadParaJugadores(50L, ID_VENDEDOR, ID_COMPRADOR, 450);
        when(servicioPropiedad.obtenerPropiedadExistente(50L)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(75);

        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, idOferta,
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(ID_COMPRADOR, respuesta.getBody().get("propietarioId"));
        assertEquals(450, respuesta.getBody().get("precioVenta"));
        assertEquals(75, respuesta.getBody().get("alquiler"));
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
        verify(notificador, org.mockito.Mockito.times(2)).convertAndSend(
                eq("/topic/partida/" + CODIGO + "/dinero"), any(Map.class));
    }

    @Test
    void informaConflictoSiLaOfertaYaNoExiste() {
        ResponseEntity<Map<String, Object>> respuesta = controlador.responderOferta(CODIGO, "inexistente",
                Map.of("aceptar", true), sesion(ID_COMPRADOR));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
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
