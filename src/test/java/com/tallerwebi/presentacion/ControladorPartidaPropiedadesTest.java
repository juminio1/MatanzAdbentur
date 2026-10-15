package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.servicios.ServicioCasillero;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPropiedad;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControladorPartidaPropiedadesTest {

    private static final String CODIGO = "PARTIDA123";
    private static final Long ID_PARTIDA = 10L;
    private static final Long ID_PROPIEDAD = 30L;

    private ServicioPartida servicioPartida;
    private SimpMessagingTemplate notificador;
    private ServicioCasillero servicioCasillero;
    private ServicioPropiedad servicioPropiedad;
    private ControladorPartida controlador;

    @BeforeEach
    void inicializar() {
        servicioPartida = mock(ServicioPartida.class);
        notificador = mock(SimpMessagingTemplate.class);
        servicioCasillero = mock(ServicioCasillero.class);
        servicioPropiedad = mock(ServicioPropiedad.class);
        controlador = new ControladorPartida(servicioPartida, notificador, servicioCasillero, servicioPropiedad);
    }

    @Test
    void consultaUnaPropiedadSinRegistroYDevuelveLosDatosDelCatalogo() {
        Partida partida = crearPartida();
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> respuesta = controlador.obtenerInfoPropiedad(CODIGO, 1);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Plaza Barrio Marina", respuesta.getBody().get("nombre"));
        assertEquals(300, respuesta.getBody().get("precioCompra"));
        assertEquals(50, respuesta.getBody().get("alquiler"));
        assertEquals(true, respuesta.getBody().get("disponible"));
    }

    @Test
    void consultaUnaPropiedadExistenteYDevuelveSuPropietario() {
        Partida partida = crearPartida();
        Jugador propietario = crearJugador(2L, "Luna", 500, 4);
        Propiedad propiedad = crearPropiedad(propietario);
        CasilleroPropiedad casillero = crearCasillero(propiedad);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(75);

        ResponseEntity<Map<String, Object>> respuesta = controlador.obtenerInfoPropiedad(CODIGO, 4);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Plaza San Justo", respuesta.getBody().get("nombre"));
        assertEquals("Luna", respuesta.getBody().get("propietario"));
        assertEquals(2L, respuesta.getBody().get("propietarioId"));
        assertEquals(75, respuesta.getBody().get("alquiler"));
        assertEquals(false, respuesta.getBody().get("disponible"));
    }

    @Test
    void compraLaPropiedadDelJugadorActivoYNotificaElSiguienteTurno() {
        Partida partida = crearPartida();
        Jugador comprador = crearJugador(1L, "Ana", 1000, 1);
        Jugador siguiente = crearJugador(2L, "Luna", 1000, 0);
        partida.setJugadores(List.of(comprador, siguiente));
        Propiedad propiedad = crearPropiedad(null);
        CasilleroPropiedad casillero = crearCasillero(propiedad, 1);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(comprador, comprador, siguiente);
        when(servicioPartida.obtenerJugadorPorId(comprador.getId())).thenReturn(comprador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(50);
        doAnswer(invocacion -> {
            propiedad.setPropietario(comprador);
            return null;
        }).when(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, comprador.getId());

        ResponseEntity<Map<String, Object>> respuesta = controlador.comprarPropiedad(CODIGO, 1);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(comprador.getId(), respuesta.getBody().get("idJugadorPropietario"));
        assertEquals(siguiente.getId(), respuesta.getBody().get("idJugadorActual"));
        assertEquals(false, respuesta.getBody().get("disponible"));
        verify(servicioPartida).avanzarTurno(CODIGO);
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
    }

    @Test
    void rechazaLaCompraSiElJugadorActivoNoCayoEnEsaPropiedad() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 2);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(jugador);

        ResponseEntity<Map<String, Object>> respuesta = controlador.comprarPropiedad(CODIGO, 1);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().get("error").toString().contains("casillero 2"));
    }

    @Test
    void vendeLaPropiedadDelJugadorYActualizaSuSaldo() {
        Partida partida = crearPartida();
        Jugador vendedor = crearJugador(1L, "Ana", 700, 4);
        partida.setJugadores(List.of(vendedor));
        Propiedad propiedad = crearPropiedad(vendedor);
        CasilleroPropiedad casillero = crearCasillero(propiedad);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", vendedor.getId());
        request.getSession().setAttribute("NOMBRE", vendedor.getApodo());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPartida.obtenerJugadorPorId(vendedor.getId())).thenReturn(vendedor);
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(50);
        when(servicioPropiedad.venderPropiedadParaJugador(ID_PROPIEDAD, vendedor.getId())).thenAnswer(invocacion -> {
            propiedad.setPropietario(null);
            vendedor.setDinero(850);
            return 150;
        });

        ResponseEntity<Map<String, Object>> respuesta = controlador.venderPropiedad(CODIGO, 4, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(150, respuesta.getBody().get("precioVenta"));
        assertEquals(850, respuesta.getBody().get("dineroJugador"));
        assertEquals(true, respuesta.getBody().get("disponible"));
        assertNull(respuesta.getBody().get("propietarioId"));
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/dinero"), any(Map.class));
    }

    @Test
    void devuelveElTurnoVigenteParaLaSincronizacionDeLasPantallas() {
        Partida partida = crearPartida();
        Jugador actual = crearJugador(2L, "Luna", 900, 1);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(actual);

        ResponseEntity<TurnoDTO> respuesta = controlador.obtenerTurnoActual(CODIGO);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(actual.getId(), respuesta.getBody().getIdJugadorActual());
        assertEquals(1, respuesta.getBody().getRondaActual());
    }

    private Partida crearPartida() {
        Partida partida = new Partida();
        partida.setId(ID_PARTIDA);
        partida.setCodigoUnico(CODIGO);
        return partida;
    }

    private Jugador crearJugador(Long id, String apodo, Integer dinero, Integer posicion) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        jugador.setDinero(dinero);
        jugador.setPosicionActual(posicion);
        return jugador;
    }

    private Propiedad crearPropiedad(Jugador propietario) {
        return new Propiedad(ID_PROPIEDAD, "San Justo", 300, 50, propietario, GrupoPropiedades.SAN_JUSTO);
    }

    private CasilleroPropiedad crearCasillero(Propiedad propiedad) {
        return crearCasillero(propiedad, 4);
    }

    private CasilleroPropiedad crearCasillero(Propiedad propiedad, Integer posicion) {
        CasilleroPropiedad casillero = new CasilleroPropiedad(ID_PROPIEDAD, posicion, "Plaza San Justo",
                "San Justo", propiedad);
        casillero.setPartidaId(ID_PARTIDA);
        return casillero;
    }
}
