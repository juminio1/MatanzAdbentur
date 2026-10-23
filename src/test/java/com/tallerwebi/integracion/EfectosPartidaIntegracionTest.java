package com.tallerwebi.integracion;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.observadores.ObservadorTurnoQuiebra;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.ControladorPartida;
import com.tallerwebi.presentacion.ControladorOfertaPropiedad;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EfectosPartidaIntegracionTest.Config.class)
class EfectosPartidaIntegracionTest {
    @Configuration
    @Import({
            ServicioPartidaImpl.class,
            ServicioTurnoImpl.class,
            ServicioEventoCasilleroImpl.class,
            ServicioCasilleroImpl.class,
            ServicioPropiedadImpl.class,
            ServicioGestionQuiebraImpl.class,
            RepositorioPartidaImpl.class,
            RepositorioCasilleroImpl.class,
            RepositorioPropiedadImpl.class,
            RepositorioEventoCasilleroImpl.class,
            RepositorioDeudaPendienteImpl.class
    })
    static class Config extends HibernateInfraestructuraTestConfig {
        @Bean
        @Override
        public DataSource dataSource() {
            DriverManagerDataSource fuente = new DriverManagerDataSource();
            fuente.setDriverClassName("org.hsqldb.jdbcDriver");
            fuente.setUrl("jdbc:hsqldb:mem:efectos_partida");
            fuente.setUsername("sa");
            fuente.setPassword("");
            return fuente;
        }

        @Bean
        ServicioDado servicioDado() { return mock(ServicioDado.class); }
        @Bean
        ServicioInicioPartida servicioInicioPartida() { return mock(ServicioInicioPartida.class); }
        @Bean
        GestorTemporizadorPartida gestorTemporizadorPartida() { return mock(GestorTemporizadorPartida.class); }

        @Bean
        PublicadorEventos<EventoJugadorAbandonado> publicadorAbandono() { return new PublicadorEventos<>(List.of()); }

        @Bean
        ObservadorTurnoQuiebra observadorTurnoQuiebra(
                ServicioPartida servicioPartida) {
            return new ObservadorTurnoQuiebra(servicioPartida);
        }
        @Bean
        PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra(
                ObservadorTurnoQuiebra observadorTurnoQuiebra) {

            return new PublicadorEventos<>(
                    List.of(observadorTurnoQuiebra)
            );
        }
    }

    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioPartida servicioPartida;
    @Autowired private ServicioCasillero servicioCasillero;
    @Autowired private ServicioPropiedad servicioPropiedad;
    @Autowired private ServicioEventoCasillero servicioEvento;
    @Autowired private ServicioDado servicioDado;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private ServicioGestionQuiebra servicioGestionQuiebra;
    private TransactionTemplate transaccion;
    private ControladorPartida controlador;
    private String codigo;
    private Long idJugador;
    private Long idPropietario;
    private SimpMessagingTemplate notificador;

    @BeforeEach
    void preparar() {
        transaccion = new TransactionTemplate(transacciones);
        notificador = mock(SimpMessagingTemplate.class);
        controlador = new ControladorPartida(
                servicioPartida,
                notificador,
                servicioCasillero,
                servicioPropiedad,
                servicioEvento,
                new NotificadorPartida(notificador),
                servicioGestionQuiebra);
        codigo = UUID.randomUUID().toString();
        transaccion.executeWithoutResult(estado -> {
            sesiones.getCurrentSession().createMutationQuery("DELETE FROM EventoCasillero").executeUpdate();
            Usuario creador = new Usuario();
            creador.setUsername(codigo);
            creador.setEmail(codigo + "@test.com");
            creador.setPassword("prueba");
            sesiones.getCurrentSession().persist(creador);
            Jugador jugador = crearJugador("Ana", 0);
            Jugador propietario = crearJugador("Beto", 1);
            Partida partida = new Partida();
            partida.setCreador(creador);
            partida.setCodigoUnico(codigo);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setJugadores(List.of(jugador, propietario));
            sesiones.getCurrentSession().persist(partida);
            idJugador = jugador.getId();
            idPropietario = propietario.getId();
            sesiones.getCurrentSession().persist(new CasilleroEvento(null, 3, "Eventos", "Eventos", null, null));
        });
        when(servicioDado.tirarDados()).thenReturn(new ResultadoTiradaDTO(1, 1));
    }

    private Jugador crearJugador(String apodo, Integer orden) {
        Jugador jugador = new Jugador();
        jugador.setApodo(apodo);
        jugador.setOrden(orden);
        jugador.setDinero(2000);
        return jugador;
    }

    private void prepararCantidadJugadores(Integer cantidad) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigo);
            for (Integer orden = 2; orden < cantidad; orden++) {
                Jugador jugador = crearJugador("Jugador " + orden, orden);
                sesiones.getCurrentSession().persist(jugador);
                partida.getJugadores().add(jugador);
            }
        });
    }

    private Long idDelOrden(Integer orden) {
        return servicioPartida.obtenerPartida(codigo).getJugadores().stream()
                .filter(jugador -> orden.equals(jugador.getOrden())).findFirst().orElseThrow().getId();
    }

    private void cargarEvento(TipoEfectoEvento tipo, Integer monto, Integer pasos, Integer destino) {
        transaccion.executeWithoutResult(estado -> {
            sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(1);
            sesiones.getCurrentSession().persist(new EventoCasillero(null, codigo, "Descripción", tipo, monto, pasos, destino));
        });
    }

    @Test
    void queUnEventoRestaRegistreDeudaSiElJugadorTienePropiedades() throws Exception{        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Propiedad propiedad = new Propiedad(
                    null, "Propiedad de Ana", 300, 50, jugador, null);

            CasilleroPropiedad casillero = new CasilleroPropiedad(
                    null, 4, "Propiedad de Ana", "Barrio", propiedad);

            casillero.setPartidaId(
                    servicioPartida.obtenerPartida(codigo).getId());

            sesiones.getCurrentSession().persist(casillero);
        });

        controlador.tirarDados(codigo, idJugador);

        Jugador jugadorActualizado =
                servicioPartida.obtenerJugadorPorId(idJugador);

        DeudaPendiente deuda = servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador);

        assertEquals(100, jugadorActualizado.getDinero());
        assertNotNull(deuda);
        assertEquals(200, deuda.getMontoPendiente());
        assertEquals("EVENTO_RESTA", deuda.getMotivo());
        assertFalse(jugadorActualizado.getEnBancarrota());
    }
    @RepeatedTest(3)
    void queElEventoQueDaPlataSeGuardeAlTerminarLaTirada(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.SUMA, 500, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertEquals(2500, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2500, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queElEventoQueQuitaPlataSeGuardeAlTerminarLaTirada(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertEquals(1800, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(1800, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queElEventoAvanceLaFichaYGuardeElDestino(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_CASILLEROS, null, 2, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertEquals(3, resultado.getPosicionCaidaEvento());
        assertEquals(5, resultado.getNuevaPosicion());
        assertEquals(5, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verify(temporizador).programarTemporizador(eq(codigo), eq(31), any(Runnable.class));
    }

    @RepeatedTest(3)
    void queElEventoLleveAlDestinoIndicado(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 22);
        controlador.tirarDados(codigo, idJugador);
        assertEquals(22, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verify(temporizador).programarTemporizador(eq(codigo), eq(36), any(Runnable.class));
    }

    @RepeatedTest(3)
    void queElEventoSalteeSolamenteElProximoTurno(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.PIERDE_TURNO, null, null, null);
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertTrue(servicioPartida.obtenerJugadorPorId(idJugador).getPierdeTurno());
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertEquals(idPropietario, resultado.getTurnoSiguiente().getIdJugadorActual());
        verify(temporizador).cancelarTemporizador(codigo);
        verify(temporizador).programarTemporizador(eq(codigo), eq(30), any(Runnable.class));
        for (Integer orden = 1; orden < cantidad; orden++) {
            assertEquals(idDelOrden(orden), servicioPartida.obtenerJugadorActual(codigo).getId());
            servicioPartida.avanzarTurno(codigo);
        }
        assertEquals(idPropietario, servicioPartida.obtenerJugadorActual(codigo).getId());
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getPierdeTurno());
        for (Integer avance = 1; avance < cantidad; avance++) {
            servicioPartida.avanzarTurno(codigo);
        }
        assertEquals(idJugador, servicioPartida.obtenerJugadorActual(codigo).getId());
    }

    @RepeatedTest(3)
    void queElAlquilerSeDescuenteYSeAcrediteAlPropietario(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarPropiedadConDueno();
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertEquals(1950, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2050, servicioPartida.obtenerJugadorPorId(idPropietario).getDinero());
        assertEquals(50, resultado.getAlquiler().monto());
        assertEquals(idJugador, resultado.getAlquiler().idJugadorPagador());
        assertEquals(1950, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
        assertEquals(2050, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idPropietario))
                .findFirst().orElseThrow().dinero());
    }

    @RepeatedTest(3)
    void queUnEventoQueCruzaInicioYTerminaEnPropiedadAjenaInformeElSaldoFinal(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 2);
        cargarPropiedadConDueno();
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        assertEquals(2, resultado.getNuevaPosicion());
        assertEquals(200, resultado.getDineroPorInicio());
        assertEquals(50, resultado.getAlquiler().monto());
        assertEquals(2150, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertEquals(2150, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().dinero());
    }

    private void cargarPropiedadConDueno() {
        transaccion.executeWithoutResult(estado -> {
            Jugador propietario = sesiones.getCurrentSession().find(Jugador.class, idPropietario);
            Propiedad propiedad = new Propiedad(null, "Plaza", 300, 50, propietario, null);
            CasilleroPropiedad casillero = new CasilleroPropiedad(null, 2, "Plaza", "Plaza", propiedad);
            casillero.setPartidaId(servicioPartida.obtenerPartida(codigo).getId());
            sesiones.getCurrentSession().persist(casillero);
        });
    }

    @Test
    void queElJugadorQuiebreSiNoPuedePagarUnEventoYNoTienePropiedades() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession()
                        .find(Jugador.class, idJugador)
                        .setDinero(100));

        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);

        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);

        assertEquals(100, jugador.getDinero());
        assertTrue(jugador.getEnBancarrota());

        assertTrue(resultado.getSaldos().stream()
                .filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst()
                .orElseThrow()
                .enBancarrota());
    }

    @RepeatedTest(3)
    void queSePuedaComprarYOfrecerPropiedadesACadaUnoDeLosOtrosJugadores(RepetitionInfo repeticion) {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        ControladorOfertaPropiedad ofertas = new ControladorOfertaPropiedad(servicioPartida, servicioCasillero,
                servicioPropiedad, notificador);
        List<Integer> posiciones = List.of(2, 4, 5);
        for (Integer orden = 1; orden < cantidad; orden++) {
            Integer posicion = posiciones.get(orden - 1);
            cargarPropiedadDisponible(posicion);
            assertEquals(true, controlador.obtenerInfoPropiedad(codigo, posicion).getBody().get("disponible"));
            assertEquals(HttpStatus.OK, controlador.comprarPropiedad(codigo, posicion).getStatusCode());
            Long comprador = idDelOrden(orden);
            var oferta = ofertas.crearOferta(codigo, posicion,
                    Map.of("idJugadorComprador", comprador, "precio", 450), sesion(idJugador));
            assertEquals(HttpStatus.OK, oferta.getStatusCode());
            for (Integer destinatario = 0; destinatario < cantidad; destinatario++) {
                List<?> pendientes = (List<?>) ofertas.ofertasPendientes(codigo, sesion(idDelOrden(destinatario))).getBody();
                assertEquals(destinatario.equals(orden) ? 1 : 0, pendientes.size());
            }
            String idOferta = (String) oferta.getBody().get("idOferta");
            var venta = ofertas.responderOferta(codigo, idOferta, Map.of("aceptar", true), sesion(comprador));
            assertEquals(HttpStatus.OK, venta.getStatusCode());
            assertEquals(comprador, venta.getBody().get("propietarioId"));
            assertEquals(1550, servicioPartida.obtenerJugadorPorId(comprador).getDinero());
            assertEquals(2000 + 150 * orden, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        }
    }

    @RepeatedTest(3)
    void queSePuedaVenderUnaPropiedadAlBanco(RepetitionInfo repeticion) {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarPropiedadConDueno();
        var venta = controlador.venderPropiedad(codigo, 2, sesion(idPropietario));
        assertEquals(HttpStatus.OK, venta.getStatusCode());
        assertEquals(2150, servicioPartida.obtenerJugadorPorId(idPropietario).getDinero());
        assertEquals(true, controlador.obtenerInfoPropiedad(codigo, 2).getBody().get("disponible"));
    }


    @Test
    void queAlVenderUnaPropiedadSePagueLaDeudaPendiente() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Propiedad propiedad = new Propiedad(
                    null, "Propiedad de Ana", 300, 50, jugador, null);

            CasilleroPropiedad casillero = new CasilleroPropiedad(
                    null, 4, "Propiedad de Ana", "Barrio", propiedad);

            casillero.setPartidaId(
                    servicioPartida.obtenerPartida(codigo).getId());

            sesiones.getCurrentSession().persist(casillero);
        });

        controlador.tirarDados(codigo, idJugador);

        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));

        var respuesta = controlador.venderPropiedad(
                codigo, 4, sesion(idJugador));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());

        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);

        assertEquals(50, jugador.getDinero());
        assertFalse(jugador.getEnBancarrota());

        assertNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
    }


    @Test
    void queSePuedanVenderVariasPropiedadesParaPagarUnaDeuda() throws Exception {
        cargarEvento(TipoEfectoEvento.RESTA, 400, null, null);

        transaccion.executeWithoutResult(estado -> {
            Jugador jugador = sesiones.getCurrentSession()
                    .find(Jugador.class, idJugador);

            jugador.setDinero(100);

            Partida partida = servicioPartida.obtenerPartida(codigo);

            for (Integer posicion : List.of(4, 5)) {
                Propiedad propiedad = new Propiedad(
                        null, "Propiedad " + posicion, 300, 50, jugador, null);

                CasilleroPropiedad casillero = new CasilleroPropiedad(
                        null, posicion, "Propiedad " + posicion, "Barrio", propiedad);

                casillero.setPartidaId(partida.getId());
                sesiones.getCurrentSession().persist(casillero);
            }
        });

        controlador.tirarDados(codigo, idJugador);

        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));

        var primeraVenta = controlador.venderPropiedad(
                codigo, 4, sesion(idJugador));

        assertEquals(HttpStatus.OK, primeraVenta.getStatusCode());
        assertEquals(250, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertNotNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());

        var segundaVenta = controlador.venderPropiedad(
                codigo, 5, sesion(idJugador));

        assertEquals(HttpStatus.OK, segundaVenta.getStatusCode());
        assertEquals(0, servicioPartida.obtenerJugadorPorId(idJugador).getDinero());
        assertNull(servicioGestionQuiebra
                .obtenerDeudaPendiente(codigo, idJugador));
        assertFalse(servicioPartida.obtenerJugadorPorId(idJugador).getEnBancarrota());
    }

    private void cargarPropiedadDisponible(Integer posicion) {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = servicioPartida.obtenerPartida(codigo);
            partida.setIndiceTurnoActual(0);
            sesiones.getCurrentSession().find(Jugador.class, idJugador).setPosicionActual(posicion);
            Propiedad propiedad = new Propiedad(null, "Propiedad " + posicion, 300, 50, null, null);
            CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, propiedad.getNombre(), "Barrio", propiedad);
            casillero.setPartidaId(partida.getId());
            sesiones.getCurrentSession().persist(casillero);
        });
    }

    private MockHttpServletRequest sesion(Long id) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("id", id);
        return request;
    }
}
