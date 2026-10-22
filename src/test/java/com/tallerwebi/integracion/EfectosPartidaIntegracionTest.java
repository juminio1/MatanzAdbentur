package com.tallerwebi.integracion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.ServicioCasilleroImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.ServicioEventoCasilleroImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioInicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.infraestructura.RepositorioCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioEventoCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPropiedadImpl;
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
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EfectosPartidaIntegracionTest.Config.class)
class EfectosPartidaIntegracionTest {
    @Configuration
    @Import({ServicioPartidaImpl.class, ServicioTurnoImpl.class, ServicioEventoCasilleroImpl.class,
            ServicioCasilleroImpl.class, ServicioPropiedadImpl.class, RepositorioPartidaImpl.class,
            RepositorioCasilleroImpl.class, RepositorioPropiedadImpl.class, RepositorioEventoCasilleroImpl.class})
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
    }

    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioPartida servicioPartida;
    @Autowired private ServicioCasillero servicioCasillero;
    @Autowired private ServicioPropiedad servicioPropiedad;
    @Autowired private ServicioEventoCasillero servicioEvento;
    @Autowired private ServicioDado servicioDado;
    @Autowired private GestorTemporizadorPartida temporizador;
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
        controlador = new ControladorPartida(servicioPartida, notificador,
                servicioCasillero, servicioPropiedad, servicioEvento);
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
        verify(temporizador).programarTemporizador(eq(codigo), eq(38), any(Runnable.class));
    }

    @RepeatedTest(3)
    void queElEventoLleveAlDestinoIndicado(RepetitionInfo repeticion) throws Exception {
        Integer cantidad = repeticion.getCurrentRepetition() + 1;
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.AVANZA_A_POSICION, null, null, 22);
        controlador.tirarDados(codigo, idJugador);
        assertEquals(22, servicioPartida.obtenerJugadorPorId(idJugador).getPosicionActual());
        verify(temporizador).programarTemporizador(eq(codigo), eq(43), any(Runnable.class));
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

    @RepeatedTest(9)
    void queLaQuiebraSeApliqueDesdeMenosDosMil(RepetitionInfo repeticion) throws Exception {
        Integer caso = repeticion.getCurrentRepetition() - 1;
        Integer cantidad = 2 + caso / 3;
        Integer saldoFinal = List.of(-1999, -2000, -2001).get(caso % 3);
        Boolean quiebra = List.of(false, true, true).get(caso % 3);
        prepararCantidadJugadores(cantidad);
        cargarEvento(TipoEfectoEvento.RESTA, 200, null, null);
        transaccion.executeWithoutResult(estado ->
                sesiones.getCurrentSession().find(Jugador.class, idJugador).setDinero(saldoFinal + 200));
        RespuestaMovimientoDTO resultado = controlador.tirarDados(codigo, idJugador);
        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        assertEquals(saldoFinal, jugador.getDinero());
        assertEquals(quiebra, jugador.getEnBancarrota());
        assertEquals(quiebra, resultado.getSaldos().stream().filter(saldo -> saldo.idJugador().equals(idJugador))
                .findFirst().orElseThrow().enBancarrota());
        assertEquals(idPropietario, resultado.getTurnoSiguiente().getIdJugadorActual());
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
