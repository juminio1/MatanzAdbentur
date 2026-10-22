package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import java.util.List;
import java.util.Map;

import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

class ControladorPartidaPropiedadesTest {

    private static final String CODIGO = "PARTIDA123";
    private static final Long ID_PARTIDA = 10L;
    private static final Long ID_PROPIEDAD = 30L;

    private ServicioPartida servicioPartida;
    private SimpMessagingTemplate notificador;
    private ServicioCasillero servicioCasillero;
    private ServicioPropiedad servicioPropiedad;
    private ControladorPartida controlador;
    private NotificadorPartida notificadorPartidaMock;
    private ServicioGestionQuiebra servicioGestionQuiebraMock;

    @BeforeEach
    void inicializar() throws EstadoPartidaInvalidoException {
        servicioPartida = mock(ServicioPartida.class);
        notificador = mock(SimpMessagingTemplate.class);
        servicioCasillero = spy(new ServicioCasilleroImpl(mock(RepositorioCasillero.class)));
        servicioPropiedad = mock(ServicioPropiedad.class);
        notificadorPartidaMock = mock(NotificadorPartida.class);
        servicioGestionQuiebraMock = mock(ServicioGestionQuiebra.class);
        controlador = new ControladorPartida(
                servicioPartida,
                notificador,
                servicioCasillero,
                servicioPropiedad,
                null,
                notificadorPartidaMock,
                servicioGestionQuiebraMock
        );
        ServicioPropiedad preparador = new ServicioPropiedadImpl(
                mock(RepositorioPropiedad.class), servicioPartida, servicioCasillero);
        doAnswer(invocacion -> preparador.prepararCompra(
                invocacion.getArgument(0), invocacion.getArgument(1)))
                .when(servicioPropiedad).prepararCompra(any(), any());
        doAnswer(invocacion -> preparador.prepararVenta(
                invocacion.getArgument(0), invocacion.getArgument(1), invocacion.getArgument(2)))
                .when(servicioPropiedad).prepararVenta(any(), any(), any());
    }

    @Test
    void lasOperacionesDePropiedadConservanNotFoundParaPosicionesQueNoSonPropiedades() {
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(crearPartida());

        assertEquals(HttpStatus.NOT_FOUND, controlador.obtenerInfoPropiedad(CODIGO, 0).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controlador.comprarPropiedad(CODIGO, 0).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controlador.venderPropiedad(
                CODIGO, 0, new MockHttpServletRequest()).getStatusCode());
        verify(servicioCasillero, never()).obtenerTodosLosCasilleros();
    }

    @Test
    void ignoraCasillerosDeOtraPartidaYSinPropiedadAlConsultarElCatalogo() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 0);
        partida.setJugadores(List.of(jugador));
        CasilleroPropiedad ajeno = crearCasillero(crearPropiedad(jugador));
        ajeno.setPartidaId(99L);
        CasilleroPropiedad sinPropiedad = crearCasillero(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", jugador.getId());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(ajeno, sinPropiedad));

        Map<String, Object> individual = controlador.obtenerInfoPropiedad(CODIGO, 4).getBody();
        List<?> catalogo = (List<?>) controlador.obtenerPropiedadesTablero(CODIGO, request).getBody();

        assertEquals(true, individual.get("disponible"));
        assertNull(individual.get("propietarioId"));
        assertEquals(individual, catalogo.get(2));
    }

    @Test
    void mejoraLaPropiedadDelJugadorActivoYPublicaSuSaldoActualizado() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 4);
        partida.setJugadores(List.of(jugador));
        Propiedad propiedad = crearPropiedad(jugador);
        CasilleroPropiedad casillero = crearCasillero(propiedad);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", jugador.getId());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(jugador.getId())).thenReturn(jugador);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(jugador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(100);

        ResponseEntity<Map<String, Object>> respuesta = controlador.subirAlquilerPropiedad(CODIGO, 4, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(100, respuesta.getBody().get("alquiler"));
        assertEquals(1000, respuesta.getBody().get("dineroJugador"));
        verify(servicioPropiedad).mejorarPropiedad(ID_PROPIEDAD, jugador);
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
    }

    @Test
    void rechazaLaMejoraCuandoElJugadorNoTieneElTurno() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 4);
        partida.setJugadores(List.of(jugador));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", jugador.getId());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(jugador.getId())).thenReturn(jugador);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(crearJugador(2L, "Luna", 1000, 0));

        ResponseEntity<Map<String, Object>> respuesta = controlador.subirAlquilerPropiedad(CODIGO, 4, request);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Solo podés subir alquileres durante tu turno.", respuesta.getBody().get("error"));
        verify(servicioPropiedad, never()).mejorarPropiedad(any(), any());
    }

    @Test
    void rechazaLaCompraSinJugadorActivoSinConsultarNuevamenteElTurno() {
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(crearPartida());

        ResponseEntity<Map<String, Object>> respuesta = controlador.comprarPropiedad(CODIGO, 1);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("No hay un jugador activo en esta partida.", respuesta.getBody().get("error"));
        verify(servicioPartida).obtenerJugadorActual(CODIGO);
        verify(servicioPropiedad, never()).comprarPropiedadParaJugador(any(), any());
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
    void devuelveElCatalogoCompletoDePropiedadesParaUnJugadorDeLaPartida() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 0);
        partida.setJugadores(List.of(jugador));
        Propiedad propiedad = crearPropiedad(jugador);
        CasilleroPropiedad casillero = crearCasillero(propiedad);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", jugador.getId());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(75);

        ResponseEntity<?> respuesta = controlador.obtenerPropiedadesTablero(CODIGO, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        List<?> catalogo = (List<?>) respuesta.getBody();
        assertEquals(23, catalogo.size());
        Map<?, ?> propiedadDisponible = (Map<?, ?>) catalogo.get(0);
        assertEquals(1, propiedadDisponible.get("posicion"));
        assertEquals("Plaza Barrio Marina", propiedadDisponible.get("nombre"));
        assertEquals(true, propiedadDisponible.get("disponible"));
        Map<?, ?> propiedadAdquirida = (Map<?, ?>) catalogo.get(2);
        assertEquals(4, propiedadAdquirida.get("posicion"));
        assertEquals("Plaza San Justo", propiedadAdquirida.get("nombre"));
        assertEquals(jugador.getId(), propiedadAdquirida.get("propietarioId"));
        assertEquals(75, propiedadAdquirida.get("alquiler"));
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
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(comprador, siguiente);
        when(servicioPartida.obtenerJugadorPorId(comprador.getId())).thenReturn(comprador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero));
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
        when(servicioPropiedad.calcularAlquiler(casillero, propiedad)).thenReturn(50);
        doAnswer(invocacion -> {
            propiedad.setPropietario(comprador);
            return null;
        }).when(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, comprador.getId());
        doAnswer(invocacion -> {
            Map<?, ?> publicado = invocacion.getArgument(1);
            assertEquals(comprador.getId(), publicado.get("idJugadorPropietario"));
            assertEquals(1000, publicado.get("dineroJugador"));
            assertEquals(false, publicado.get("enBancarrota"));
            assertTrue(!publicado.containsKey("idJugadorActual"));
            return null;
        }).when(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));

        ResponseEntity<Map<String, Object>> respuesta = controlador.comprarPropiedad(CODIGO, 1);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(comprador.getId(), respuesta.getBody().get("idJugadorPropietario"));
        assertEquals(siguiente.getId(), respuesta.getBody().get("idJugadorActual"));
        assertEquals(false, respuesta.getBody().get("disponible"));
        verify(servicioPartida).avanzarTurno(CODIGO);
        verify(servicioPartida, times(2)).obtenerJugadorActual(CODIGO);
        verify(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, comprador.getId());
        verify(servicioCasillero, never()).guardarCasillero(any());
        verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
        var orden = inOrder(notificador, servicioPartida);
        orden.verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));
        orden.verify(notificador, times(2)).convertAndSend(eq("/topic/partida/" + CODIGO + "/dinero"), any(Map.class));
        orden.verify(servicioPartida).avanzarTurno(CODIGO);
    }

    @Test
    void compraSinFondosConservaElCodigoDeErrorYNoNotificaNiAvanza() {
        prepararCompraExistente();
        doThrow(new DineroInsuficienteException("Fondos insuficientes"))
                .when(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, 1L);

        var respuesta = controlador.comprarPropiedad(CODIGO, 4);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(Map.of("codigoError", "DINERO_INSUFICIENTE", "error", "Fondos insuficientes"),
                respuesta.getBody());
        verifyNoInteractions(notificador);
        verify(servicioPartida, never()).avanzarTurno(any());
    }

    @Test
    void errorDeMaterializacionConservaElErrorInternoSinComprar() {
        prepararCompraExistente();
        when(servicioCasillero.buscarOCrearPropiedadEnPosicion(any(), eq(4)))
                .thenThrow(new IllegalStateException("Fallo de persistencia"));

        var respuesta = controlador.comprarPropiedad(CODIGO, 4);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals(Map.of("error", "Error interno al comprar la propiedad: Fallo de persistencia"),
                respuesta.getBody());
        verify(servicioPropiedad, never()).comprarPropiedadParaJugador(any(), any());
        verifyNoInteractions(notificador);
        verify(servicioPartida, never()).avanzarTurno(any());
    }

    @Test
    void errorDeCompraConMensajeVacioConservaElConflictoPredeterminado() {
        prepararCompraExistente();
        doThrow(new IllegalStateException(" "))
                .when(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, 1L);

        var respuesta = controlador.comprarPropiedad(CODIGO, 4);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(Map.of("error", "Error al guardar la compra de la propiedad."), respuesta.getBody());
        verifyNoInteractions(notificador);
        verify(servicioPartida, never()).avanzarTurno(any());
    }

    @Test
    void falloAlNotificarLaCompraNoPublicaSaldosNiAvanzaElTurno() {
        prepararCompraExistente();
        doThrow(new IllegalStateException("Fallo de envio")).when(notificador)
                .convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), any(Map.class));

        var respuesta = controlador.comprarPropiedad(CODIGO, 4);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(Map.of("error", "Fallo de envio"), respuesta.getBody());
        verify(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, 1L);
        verify(notificador, never()).convertAndSend(eq("/topic/partida/" + CODIGO + "/dinero"), any(Map.class));
        verify(servicioPartida, never()).avanzarTurno(any());
    }

    private void prepararCompraExistente() {
        Partida partida = crearPartida();
        Jugador comprador = crearJugador(1L, "Ana", 1000, 4);
        partida.setJugadores(List.of(comprador));
        Propiedad propiedad = crearPropiedad(null);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(comprador);
        when(servicioPartida.obtenerJugadorPorId(1L)).thenReturn(comprador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(crearCasillero(propiedad)));
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
    }

    @Test
    void lasConsultasDelTableroNoMaterializanPropiedades() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 0);
        partida.setJugadores(List.of(jugador));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", jugador.getId());
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(jugador.getId())).thenReturn(jugador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of());

        assertEquals(HttpStatus.OK, controlador.obtenerInfoPropiedad(CODIGO, 1).getStatusCode());
        ResponseEntity<?> catalogo = controlador.obtenerPropiedadesTablero(CODIGO, request);
        assertEquals(HttpStatus.OK, catalogo.getStatusCode());
        assertEquals(23, ((List<?>) catalogo.getBody()).size());
        var vista = controlador.verTablero(CODIGO, request);
        assertEquals("partida", vista.getViewName());
        assertEquals(catalogo.getBody(), vista.getModel().get("propiedadesTablero"));

        verify(servicioCasillero, never()).guardarCasillero(any());
        verify(servicioPropiedad, never()).comprarPropiedadParaJugador(any(), any());
        verify(servicioPartida, never()).actualizarJugador(any());
    }

    @Test
    void laCompraMaterializaUnaPropiedadComunConLosDatosDelCatalogo() {
        comprobarCreacionEnCompra(1, "Plaza Barrio Marina", "Villa Luzuriaga", 50,
                GrupoPropiedades.VILLA_LUZURIAGA);
    }

    @Test
    void laCompraMaterializaUnaParadaSinGrupoYConAlquilerBaseVeinte() {
        comprobarCreacionEnCompra(6, "Colectivo 242", "Parada de bondi", 20, null);
    }

    @Test
    void laCompraUsaLaPropiedadDeSuPartidaAunqueOtraOcupeLaMismaPosicion() {
        Partida partida = crearPartida();
        Jugador comprador = crearJugador(1L, "Ana", 1000, 4);
        partida.setJugadores(List.of(comprador));
        Propiedad propia = crearPropiedad(null);
        Propiedad ajena = crearPropiedad(null);
        ajena.setId(99L);
        CasilleroPropiedad casilleroAjeno = crearCasillero(ajena);
        casilleroAjeno.setPartidaId(99L);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(comprador);
        when(servicioPartida.obtenerJugadorPorId(comprador.getId())).thenReturn(comprador);
        when(servicioCasillero.obtenerTodosLosCasilleros())
                .thenReturn(List.of(casilleroAjeno, crearCasillero(propia)));
        when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propia);

        assertEquals(HttpStatus.OK, controlador.comprarPropiedad(CODIGO, 4).getStatusCode());

        verify(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, comprador.getId());
        verify(servicioPropiedad, never()).comprarPropiedadParaJugador(eq(99L), any());
        verify(servicioCasillero, never()).guardarCasillero(any());
    }

    @Test
    void laTiradaMaterializaLaPropiedadDelDestinoYEsperaLaCompra() throws Exception {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 1000, 0);
        partida.setJugadores(List.of(jugador));
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(jugador);
        when(servicioPartida.tirarDados()).thenReturn(new ResultadoTiradaDTO(2, 2));
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of());
        doAnswer(invocacion -> {
            jugador.setPosicionActual(4);
            return null;
        }).when(servicioPartida).moverJugador(jugador, 4, 30);
        when(servicioPropiedad.estaDisponible(any(Propiedad.class))).thenReturn(true);

        RespuestaMovimientoDTO respuesta = controlador.tirarDados(CODIGO, jugador.getId());

        ArgumentCaptor<Casillero> captor = ArgumentCaptor.forClass(Casillero.class);
        verify(servicioCasillero).guardarCasillero(captor.capture());
        CasilleroPropiedad creado = (CasilleroPropiedad) captor.getValue();
        assertEquals(4, creado.getPosicion());
        assertEquals(ID_PARTIDA, creado.getPartidaId());
        assertNull(creado.getPropiedad().getPropietario());
        verify(servicioPropiedad).estaDisponible(creado.getPropiedad());
        assertEquals(0, respuesta.getPosicionAnterior());
        assertEquals(4, respuesta.getNuevaPosicion());
        verify(notificador).convertAndSend("/topic/partida/" + CODIGO, respuesta);
        verify(servicioPartida, never()).avanzarTurno(any());
        verify(servicioPropiedad, never()).pagarAlquiler(any(), any(), any());
    }

    private void comprobarCreacionEnCompra(Integer posicion, String nombre, String descripcion,
                                           Integer alquilerBase, GrupoPropiedades grupo) {
        Partida partida = crearPartida();
        Jugador comprador = crearJugador(1L, "Ana", 1000, posicion);
        partida.setJugadores(List.of(comprador));
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorActual(CODIGO)).thenReturn(comprador);
        when(servicioPartida.obtenerJugadorPorId(comprador.getId())).thenReturn(comprador);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of());
        doAnswer(invocacion -> {
            CasilleroPropiedad creado = invocacion.getArgument(0);
            assertNull(creado.getId());
            Propiedad propiedad = creado.getPropiedad();
            assertNull(propiedad.getId());
            assertNull(propiedad.getPropietario());
            propiedad.setId(ID_PROPIEDAD);
            when(servicioPropiedad.obtenerPropiedadExistente(ID_PROPIEDAD)).thenReturn(propiedad);
            return null;
        }).when(servicioCasillero).guardarCasillero(any());

        ResponseEntity<Map<String, Object>> respuesta = controlador.comprarPropiedad(CODIGO, posicion);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        ArgumentCaptor<Casillero> captor = ArgumentCaptor.forClass(Casillero.class);
        verify(servicioCasillero).guardarCasillero(captor.capture());
        CasilleroPropiedad creado = (CasilleroPropiedad) captor.getValue();
        assertEquals(ID_PARTIDA, creado.getPartidaId());
        assertEquals(posicion, creado.getPosicion());
        assertEquals(nombre, creado.getNombre());
        assertEquals(descripcion, creado.getDescripcion());
        Propiedad propiedad = creado.getPropiedad();
        assertEquals(nombre, propiedad.getNombre());
        assertEquals(300, propiedad.getPrecioCompra());
        assertEquals(alquilerBase, propiedad.getPrecioAlquiler());
        assertEquals(grupo, propiedad.getGrupo());
        assertEquals(0, propiedad.getNivelMejora());
        verify(servicioPropiedad).comprarPropiedadParaJugador(ID_PROPIEDAD, comprador.getId());
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
        Jugador vendedor = crearJugador(1L, "Ana", 700, 9);
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
        var orden = inOrder(notificador);
        orden.verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/propiedades"), eq(respuesta.getBody()));
        orden.verify(notificador).convertAndSend(eq("/topic/partida/" + CODIGO + "/dinero"),
                eq(Map.of("idJugador", vendedor.getId(), "dinero", 850, "enBancarrota", false)));
        verify(servicioPartida, never()).obtenerJugadorActual(any());
        verify(servicioPartida, never()).avanzarTurno(any());
        verify(servicioCasillero, never()).guardarCasillero(any());
    }

    @Test
    void ventaRechazaJugadorAjenoOInhabilitadoSinBuscarNiCrearLaPropiedad() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 700, 9);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 1L);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(1L)).thenReturn(jugador);

        var ajeno = controlador.venderPropiedad(CODIGO, 4, request);
        assertEquals(HttpStatus.FORBIDDEN, ajeno.getStatusCode());
        assertEquals(Map.of("error", "No perteneces a esta partida."), ajeno.getBody());
        partida.setJugadores(List.of(jugador));
        jugador.setEnBancarrota(true);
        var inhabilitado = controlador.venderPropiedad(CODIGO, 4, request);
        assertEquals(HttpStatus.FORBIDDEN, inhabilitado.getStatusCode());
        assertEquals(ajeno.getBody(), inhabilitado.getBody());
        verify(servicioCasillero, never()).obtenerTodosLosCasilleros();
        verify(servicioCasillero, never()).guardarCasillero(any());
        verify(servicioPropiedad, never()).venderPropiedadParaJugador(any(), any());
        verifyNoInteractions(notificador);
    }

    @Test
    void ventaSinCasilleroPropioDevuelveConflictoSinMaterializar() {
        Partida partida = crearPartida();
        Jugador jugador = crearJugador(1L, "Ana", 700, 9);
        partida.setJugadores(List.of(jugador));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 1L);
        when(servicioPartida.obtenerPartida(CODIGO)).thenReturn(partida);
        when(servicioPartida.obtenerJugadorPorId(1L)).thenReturn(jugador);
        CasilleroPropiedad ajeno = crearCasillero(crearPropiedad(jugador));
        ajeno.setPartidaId(99L);
        when(servicioCasillero.obtenerTodosLosCasilleros()).thenReturn(List.of(ajeno));

        var respuesta = controlador.venderPropiedad(CODIGO, 4, request);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(Map.of("error", "La propiedad no está disponible para vender."), respuesta.getBody());
        verify(servicioCasillero, never()).guardarCasillero(any());
        verify(servicioPropiedad, never()).venderPropiedadParaJugador(any(), any());
        verifyNoInteractions(notificador);
    }

    @Test
    void errorDeVentaConservaElConflictoSinNotificarNiAvanzar() {
        prepararCompraExistente();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", 1L);
        when(servicioPropiedad.venderPropiedadParaJugador(ID_PROPIEDAD, 1L))
                .thenThrow(new IllegalStateException(" "));

        var respuesta = controlador.venderPropiedad(CODIGO, 4, request);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(Map.of("error", "No se pudo vender la propiedad."), respuesta.getBody());
        verifyNoInteractions(notificador);
        verify(servicioPartida, never()).avanzarTurno(any());
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
