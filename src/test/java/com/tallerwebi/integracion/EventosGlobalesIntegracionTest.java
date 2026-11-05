package com.tallerwebi.integracion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoJugada.EventoAplicado;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.eventos.*;
import com.tallerwebi.dominio.observadores.*;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.ControladorEventoGlobal;
import com.tallerwebi.presentacion.ControladorPropiedadesPartida;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.*;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EventosGlobalesIntegracionTest.Config.class)
class EventosGlobalesIntegracionTest {
    @Configuration
    @Import({ServicioPartidaImpl.class, ServicioTurnoImpl.class, ServicioParticipacionImpl.class,
            ServicioEventoImpl.class, AplicadorConsecuenciaEventoGlobal.class,
            ServicioEconomiaImpl.class, ServicioMovimientoImpl.class, ServicioGestionQuiebraImpl.class,
            ServicioPropiedadImpl.class, ServicioCasilleroImpl.class, ServicioFinalizacionPartidaImpl.class,
            ServicioOfertaPropiedadImpl.class,
            ServicioJugadaImpl.class, ServicioEventoCasilleroImpl.class,
            RepositorioPartidaImpl.class, RepositorioEventoGlobalImpl.class, RepositorioDeudaPendienteImpl.class,
            RepositorioPropiedadImpl.class, RepositorioCasilleroImpl.class, RepositorioFinalizarPartidaImpl.class,
            RepositorioEventoCasilleroImpl.class,
            ObservadorTurnoQuiebra.class, ObservadorTurnoAbandono.class, ObservadorVictoria.class})
    static class Config extends HibernateInfraestructuraTestConfig {
        @Bean @Override public DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:hsqldb:mem:eventos_integracion", "sa", "");
        }
        @Bean ServicioDado dados() { return mock(ServicioDado.class); }
        @Bean @Primary RepositorioEventoCasillero tarjetas() { return mock(RepositorioEventoCasillero.class); }
        @Bean ServicioInicioPartida inicio() { return mock(ServicioInicioPartida.class); }
        @Bean GestorTemporizadorPartida temporizador() { return mock(GestorTemporizadorPartida.class); }
        @Bean SimpMessagingTemplate mensajeria() { return mock(SimpMessagingTemplate.class); }
        @Bean NotificadorPartida notificador(SimpMessagingTemplate mensajeria) { return new NotificadorPartida(mensajeria); }
        @Bean PublicadorEventos<EventoTurnoCambiado> turnos() { return new PublicadorEventos<>(List.of()); }
        @Bean PublicadorEventos<EventoJugadorQuebrado> quiebras(ObservadorTurnoQuiebra turno, ObservadorVictoria victoria) {
            return new PublicadorEventos<>(List.of(turno, evento -> victoria.verificarVictoria(evento.partida())));
        }
        @Bean PublicadorEventos<EventoJugadorAbandonado> abandonos(ObservadorTurnoAbandono turno, ObservadorVictoria victoria) {
            return new PublicadorEventos<>(List.of(turno, victoria));
        }
    }

    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioEvento eventos;
    @Autowired private ServicioPartida partidas;
    @Autowired private ServicioGestionQuiebra quiebras;
    @Autowired private ServicioJugada jugadas;
    @Autowired private ServicioPropiedad propiedades;
    @Autowired private ServicioCasillero casilleros;
    @Autowired private ServicioOfertaPropiedad ofertas;
    @Autowired private RepositorioEventoCasillero tarjetas;
    @Autowired private ServicioDado dados;
    @Autowired private GestorTemporizadorPartida temporizador;
    @Autowired private SimpMessagingTemplate mensajeria;
    private TransactionTemplate transaccion;
    private String codigo;
    private List<Long> jugadores;
    private RandomGenerator aleatorio;

    @BeforeEach
    void preparar() {
        transaccion = new TransactionTemplate(transacciones);
        codigo = UUID.randomUUID().toString();
        reset(dados, temporizador, mensajeria, tarjetas);
        when(dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        aleatorio = mock(RandomGenerator.class);
        Object servicioReal = AopTestUtils.getTargetObject(eventos);
        ReflectionTestUtils.setField(servicioReal, "aleatorio", aleatorio);
        transaccion.executeWithoutResult(estado -> {
            var sesion = sesiones.getCurrentSession();
            if (sesion.createQuery("FROM EventoGlobal", EventoGlobal.class).list().isEmpty()) {
                for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
                    EventoGlobal evento = new EventoGlobal();
                    evento.setNombre(tipo.name());
                    evento.setDescripcion(tipo.name());
                    evento.setTipoEventoGlobal(tipo);
                    evento.setTipoEfecto(TipoEfectoEventoGlobal.MOVER_CASILLEROS);
                    evento.setValor(new int[]{0, -3, 0, 500, 400, 1}[tipo.ordinal()]);
                    sesion.persist(evento);
                }
            }
            Usuario creador = new Usuario();
            creador.setEmail(codigo + "@test.com");
            creador.setUsername(codigo);
            sesion.persist(creador);
            Partida partida = new Partida();
            partida.setCreador(creador);
            partida.setCodigoUnico(codigo);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(4);
            partida.setIndiceTurnoActual(2);
            for (int indice = 0; indice < 3; indice++) {
                Jugador jugador = new Jugador();
                jugador.setOrden(indice);
                jugador.setApodo("Participante " + indice);
                jugador.setDinero(2000);
                jugador.setPosicionActual(1 + indice * 6);
                jugador.setUltimaRondaConOportunidad(indice == 2 ? 3 : 4);
                partida.getJugadores().add(jugador);
            }
            sesion.persist(partida);
            jugadores = partida.getJugadores().stream().map(Jugador::getId).toList();
        });
    }

    @Test
    void dadoQueUnaInundacionCruzaInicioCuandoSeRecargaElEstadoDaComoResultadoPosicionYTiradasPersistidas() {
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        assertEquals(28, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        Map<String, Object> recargado = eventos.consultar(codigo, jugadores.get(1));
        assertEquals(3, ((Map<?, ?>) recargado.get("tiradas")).size());
        assertEquals(true, recargado.get("resuelto"));
        assertThrows(IllegalStateException.class, () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        confirmarTodos();
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertEquals(28, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoEsComisariaCuandoResuelveElMovimientoDaComoResultadoEncarcelamientoPersistido() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 8);
            iniciar(tipo);
            tirarTodos();
            Jugador preso = partidas.obtenerJugadorPorId(jugadores.get(0));
            assertEquals(8, preso.getPosicionActual());
            assertTrue(preso.getEstadoCarcel().getEncarcelado());
            transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession()
                    .find(Jugador.class, preso.getId()).getEstadoCarcel().registrarIntentoFallido());
            eventos.consultar(codigo, preso.getId());
            confirmarTodos();
            assertEquals(1, partidas.obtenerJugadorPorId(preso.getId()).getEstadoCarcel().getIntentosSalirCarcel());
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoEsEventosCuandoSeConsultaYConfirmaDaComoResultadoUnaTarjetaAplicadaYPersistida() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 3);
            prepararTarjeta(TipoEfectoEvento.SUMA, 0);
            iniciar(tipo);
            tirarTodos();
            EventoAplicado tarjeta = (EventoAplicado) eventos.consultar(codigo, jugadores.get(0)).get("tarjeta");
            assertEquals("Tarjeta " + codigo, tarjeta.nombre());
            assertEquals("Consecuencia de prueba", tarjeta.descripcion());
            assertEquals(2500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(3, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
            assertThrows(IllegalStateException.class, () -> eventos.tirar(codigo, jugadores.get(0), 5, 1));
            confirmarTodos();
            eventos.confirmar(codigo, jugadores.get(0), 5);
            assertEquals(tarjeta, eventos.consultar(codigo, jugadores.get(0)).get("tarjeta"));
            assertEquals(2500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            verify(tarjetas, times(1)).obtenerTodos();
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoEsPropiedadAjenaMejoradaCuandoSeRepiteLaConsultaDaComoResultadoUnSoloAlquiler() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            crearPropiedad(5, 1, 2);
            iniciar(tipo);
            tirarTodos();
            assertEquals(1700, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(2300, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
            assertTrue(eventos.consultar(codigo, jugadores.get(0)).get("consecuencia").toString().contains("Alquiler: $300"));
            assertThrows(IllegalStateException.class, () -> eventos.tirar(codigo, jugadores.get(0), 5, 1));
            confirmarTodos();
            eventos.confirmar(codigo, jugadores.get(0), 5);
            assertEquals(1700, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(2300, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoEsPropiedadPropiaCuandoSeResuelveDaComoResultadoNingunCobro() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            crearPropiedad(5, 0, 2);
            iniciar(tipo);
            tirarTodos();
            assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoNoTienePropietarioCuandoSeResuelveDaComoResultadoNingunaCompra() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            Long propiedad = crearPropiedad(5, null, 0);
            iniciar(tipo);
            tirarTodos();
            assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertNull(propiedades.obtenerPropiedadExistente(propiedad).getPropietario());
            assertThrows(com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException.class,
                    () -> propiedades.prepararCompra(codigo, 5));
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElAlquilerSuperaElSaldoCuandoPuedeVenderDaComoResultadoDeudaSinDuplicados() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            crearPropiedad(5, 1, 2);
            Long vendible = crearPropiedad(1, 0, 0);
            cambiarDinero(jugadores.get(0), 200);
            iniciar(tipo);
            tirarTodos();
            DeudaPendiente deuda = quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0));
            assertEquals(300, deuda.getMontoPendiente());
            assertEquals(jugadores.get(1), deuda.getJugadorAcreedor().getId());
            confirmarTodos();
            assertEquals(true, eventos.consultar(codigo, jugadores.get(0)).get("activo"));
            assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
            assertTrue(quiebras.venderPropiedadYResolverDeuda(codigo, jugadores.get(0), vendible));
            assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
            assertFalse(quiebras.intentarResolverDeuda(codigo, jugadores.get(0)));
            assertEquals(2300, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
            assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElAlquilerSuperaElPatrimonioCuandoSeResuelveDaComoResultadoLaQuiebraExistente() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            crearPropiedad(5, 1, 2);
            cambiarDinero(jugadores.get(0), 10);
            iniciar(tipo);
            tirarTodos();
            assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEnBancarrota(),
                    () -> eventos.consultar(codigo, jugadores.get(0)).toString());
            assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueLaTarjetaMueveAPropiedadAjenaCuandoSeResuelveDaComoResultadoUnAlquilerFinal() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 3);
            prepararTarjeta(TipoEfectoEvento.AVANZA_A_POSICION, 5);
            crearPropiedad(5, 1, 2);
            iniciar(tipo);
            tirarTodos();
            assertEquals(5, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
            assertEquals(1700, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(2300, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
            eventos.consultar(codigo, jugadores.get(0));
            verify(tarjetas, times(1)).obtenerTodos();
        });
    }

    @Test
    void dadoQueLaTarjetaMueveAOtroEventoCuandoSeResuelveDaComoResultadoNingunaCadenaDeTarjetas() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 3);
        prepararTarjeta(TipoEfectoEvento.AVANZA_A_POSICION, 10);
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        assertEquals(10, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        verify(tarjetas, times(1)).obtenerTodos();
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElDestinoEsUnaParadaCuandoTieneDosElPropietarioDaComoResultadoElAlquilerExistente() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 6);
            crearPropiedad(6, 1, 0);
            crearPropiedad(12, 1, 0);
            iniciar(tipo);
            tirarTodos();
            assertEquals(1950, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(2050, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
        });
    }

    @Test
    void dadoQueLaTarjetaRestaSinSaldoCuandoTienePropiedadesDaComoResultadoLaDeudaExistente() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 3);
        prepararTarjeta(TipoEfectoEvento.RESTA, 0);
        crearPropiedad(1, 0, 0);
        cambiarDinero(jugadores.get(0), 10);
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        DeudaPendiente deuda = quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0));
        assertEquals(500, deuda.getMontoPendiente());
        assertEquals("EVENTO_RESTA", deuda.getMotivo());
        assertNotNull(eventos.consultar(codigo, jugadores.get(0)).get("tarjeta"));
        verify(tarjetas, times(1)).obtenerTodos();
    }

    @Test
    void dadoQueLaTarjetaRestaSinPatrimonioCuandoSeResuelveDaComoResultadoQuiebraSinRepetirTarjeta() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 3);
        prepararTarjeta(TipoEfectoEvento.RESTA, 0);
        cambiarDinero(jugadores.get(0), 10);
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEnBancarrota());
        assertNotNull(eventos.consultar(codigo, jugadores.get(0)).get("tarjeta"));
        verify(tarjetas, times(1)).obtenerTodos();
    }

    @Test
    void dadoQueFallaLaTarjetaCuandoSeRevierteLaCaidaDaComoResultadoMovimientoYResolucionSinAplicar() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 3);
        Integer idTarjeta = prepararTarjeta(TipoEfectoEvento.SUMA, 0);
        doReturn(List.of()).when(tarjetas).obtenerTodos();
        iniciar(TipoEventoGlobal.INUNDACION);
        eventos.tirar(codigo, jugadores.get(0), 5, 1);
        eventos.tirar(codigo, jugadores.get(1), 5, 1);
        assertThrows(com.tallerwebi.dominio.excepcion.EventoCasilleroNoEncontradoException.class,
                () -> eventos.tirar(codigo, jugadores.get(2), 5, 1));
        assertEquals(6, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaResuelto());
        assertNull(partidas.obtenerPartida(codigo).getEventoGlobal().getTarjeta());
        doAnswer(invocacion -> List.of(sesiones.getCurrentSession().find(EventoCasillero.class, idTarjeta)))
                .when(tarjetas).obtenerTodos();
        eventos.tirar(codigo, jugadores.get(2), 5, 1);
        assertEquals(2500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaResuelto());
    }

    @Test
    void dadoQueLaTarjetaMueveAComisariaCuandoSeResuelveDaComoResultadoEncarcelamiento() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 3);
        prepararTarjeta(TipoEfectoEvento.AVANZA_A_POSICION, 8);
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEstadoCarcel().getEncarcelado());
        verify(tarjetas, times(1)).obtenerTodos();
    }

    @Test
    void dadoQueAfipNoDesplazaAlSeleccionadoCuandoEstaEnEventosDaComoResultadoNingunaTarjeta() {
        prepararDestino(TipoEventoGlobal.TRAMITES_AFIP, 3);
        cambiarPosicion(jugadores.get(0), 3);
        prepararTarjeta(TipoEfectoEvento.SUMA, 0);
        iniciar(TipoEventoGlobal.TRAMITES_AFIP);
        tirarTodos();
        assertNull(eventos.consultar(codigo, jugadores.get(0)).get("tarjeta"));
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        verifyNoInteractions(tarjetas);
    }

    @Test
    void dadoQueAfipNoDesplazaAlSeleccionadoCuandoEstaEnPropiedadAjenaDaComoResultadoNingunCobro() {
        prepararDestino(TipoEventoGlobal.TRAMITES_AFIP, 5);
        cambiarPosicion(jugadores.get(0), 5);
        crearPropiedad(5, 1, 2);
        iniciar(TipoEventoGlobal.TRAMITES_AFIP);
        tirarTodos();
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElSeleccionadoEstaPresoCuandoResuelveElEventoDaComoResultadoNingunDesplazamiento() {
        return destinosDeMovimiento(tipo -> {
            cambiarPosicion(jugadores.get(0), 8);
            transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession()
                    .find(Jugador.class, jugadores.get(0)).getEstadoCarcel().encarcelar());
            iniciar(tipo);
            tirarTodos();
            assertEquals(8, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
            assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEstadoCarcel().getEncarcelado());
        });
    }

    private Stream<DynamicTest> destinosDeMovimiento(Consumer<TipoEventoGlobal> prueba) {
        return Stream.of(TipoEventoGlobal.INUNDACION, TipoEventoGlobal.TRAMITES_AFIP)
                .map(tipo -> DynamicTest.dynamicTest(tipo.name(), () -> {
                    preparar();
                    prueba.accept(tipo);
                }));
    }

    private void prepararDestino(TipoEventoGlobal tipo, Integer destino) {
        cambiarPosicion(jugadores.get(0), tipo == TipoEventoGlobal.INUNDACION ? destino + 3 : 1);
        cambiarPosicion(jugadores.get(1), destino);
        cambiarPosicion(jugadores.get(2), 0);
    }

    private void cambiarPosicion(Long jugador, Integer posicion) {
        transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession().find(Jugador.class, jugador).setPosicionActual(posicion));
    }

    private Long crearPropiedad(Integer posicion, Integer propietario, Integer nivel) {
        return transaccion.execute(estado -> {
            Partida partida = sesiones.getCurrentSession().createQuery("FROM Partida WHERE codigoUnico = :codigo", Partida.class)
                    .setParameter("codigo", codigo).getSingleResult();
            CasilleroPropiedad casillero = casilleros.buscarOCrearPropiedadEnPosicion(partida, posicion);
            Propiedad propiedad = casillero.getPropiedad();
            propiedad.setNivelMejora(nivel);
            if (propietario != null) propiedad.setPropietario(sesiones.getCurrentSession().find(Jugador.class, jugadores.get(propietario)));
            return propiedad.getId();
        });
    }

    private Integer prepararTarjeta(TipoEfectoEvento efecto, Integer destino) {
        Integer idTarjeta = transaccion.execute(estado -> {
            var sesion = sesiones.getCurrentSession();
            for (Integer posicion : List.of(3, 10)) {
                if (sesion.createQuery("FROM CasilleroEvento WHERE posicion = :posicion", CasilleroEvento.class)
                        .setParameter("posicion", posicion).list().isEmpty()) {
                    sesion.persist(new CasilleroEvento(null, posicion, "EVENTOS", "Tarjeta", null, null));
                }
            }
            EventoCasillero tarjeta = new EventoCasillero(null, "Tarjeta " + codigo,
                    "Consecuencia de prueba", efecto, 500, 2, destino);
            sesion.persist(tarjeta);
            return tarjeta.getId();
        });
        doAnswer(invocacion -> List.of(sesiones.getCurrentSession().find(EventoCasillero.class, idTarjeta)))
                .when(tarjetas).obtenerTodos();
        return idTarjeta;
    }

    @Test
    void dadoQueHayUnEventoActivoCuandoSeIntentaComprarDaComoResultadoUnaAccionRechazadaSinCambiosEconomicos() {
        iniciar(TipoEventoGlobal.FIAT_600);
        assertThrows(com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException.class,
                () -> propiedades.prepararCompra(codigo, 1));
        assertEquals(2400, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
    }

    @Test
    void dadoQueHayRoboSinDineroCuandoSeVendeUnaPropiedadDaComoResultadoUnaUnicaCancelacionDeLaDeuda() {
        cambiarDinero(jugadores.get(0), 100);
        Long propiedad = transaccion.execute(estado -> {
            Propiedad terreno = new Propiedad();
            terreno.setNombre("Terreno para pagar el robo");
            terreno.setPrecioCompra(2000);
            terreno.setPrecioAlquiler(100);
            terreno.setPropietario(sesiones.getCurrentSession().find(Jugador.class, jugadores.get(0)));
            sesiones.getCurrentSession().persist(terreno);
            return terreno.getId();
        });
        iniciar(TipoEventoGlobal.ROBO);
        assertEquals("ROBO_EVENTO_GLOBAL", quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)).getMotivo());
        confirmarTodos();
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertTrue(quiebras.venderPropiedadYResolverDeuda(codigo, jugadores.get(0), propiedad));
        assertEquals(600, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        assertFalse(quiebras.intentarResolverDeuda(codigo, jugadores.get(0)));
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueElRobadoQuiebraCuandoLosOtrosConfirmanDaComoResultadoSuExclusionYLaReanudacionCorrecta() {
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        confirmarTodos();
        quiebras.declararQuiebra(codigo, jugadores.get(0));
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getEnBancarrota());
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueSarmientoAfectaAlProximoCuandoSeReanudaDaComoResultadoUnTurnoOmitidoYLuegoUnaTiradaPermitida() throws Exception {
        iniciar(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        confirmarTodos();
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        partidas.avanzarTurno(codigo);
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(0), partidas.obtenerJugadorActual(codigo).getId());
        assertFalse(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        assertDoesNotThrow(() -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        assertEquals(6, partidas.obtenerPartida(codigo).getRondaActual());
    }

    @Test
    void dadoQueSarmientoAfectaAlJugadorPendienteCuandoConfirmanDaComoResultadoUnaOportunidadConsumida() {
        iniciar(TipoEventoGlobal.ACCIDENTE_SARMIENTO);
        assertTrue(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        confirmarTodos();
        assertEquals(jugadores.get(1), partidas.obtenerJugadorActual(codigo).getId());
        assertEquals(5, partidas.obtenerJugadorPorId(jugadores.get(0)).getUltimaRondaConOportunidad());
        assertFalse(partidas.obtenerJugadorPorId(jugadores.get(0)).getPierdeTurno());
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(2), partidas.obtenerJugadorActual(codigo).getId());
        partidas.avanzarTurno(codigo);
        assertEquals(jugadores.get(0), partidas.obtenerJugadorActual(codigo).getId());
    }

    @Test
    void dadoQueQuedaUnaPenalizacionPendienteCuandoSeIntentaTirarDaComoResultadoAccionRechazada() {
        transaccion.executeWithoutResult(estado -> {
            Partida partida = sesiones.getCurrentSession().createQuery(
                    "FROM Partida WHERE codigoUnico = :codigo", Partida.class)
                    .setParameter("codigo", codigo).getSingleResult();
            partida.setIndiceTurnoActual(0);
            partida.getJugadores().get(0).setPierdeTurno(true);
        });
        assertThrows(com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException.class,
                () -> jugadas.tirarYMover(codigo, jugadores.get(0)));
        verify(dados, never()).tirarDados();
    }

    @Test
    void dadoQueLaQuiebraDelRobadoDejaUnSoloActivoCuandoFinalizaLaPartidaDaComoResultadoLaLimpiezaDelEvento() throws Exception {
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        partidas.abandonarPartida(codigo, jugadores.get(2));
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        quiebras.declararQuiebra(codigo, jugadores.get(0));
        Partida finalizada = partidas.obtenerPartidaParaVisualizacion(codigo);
        assertEquals(EstadoPartida.FINALIZADA, finalizada.getEstado());
        assertFalse(finalizada.getEventoGlobal().estaActivo());
        verify(temporizador, never()).programarTemporizador(eq(codigo), anyInt(), any());
        verify(temporizador, atLeastOnce()).cancelarTemporizadorEvento(eq("evento:" + codigo), anyLong());
    }

    @Test
    void dadoQueUnJugadorAbandonaSinTirarCuandoLosRestantesTiranDaComoResultadoQueNoSeLoEspera() throws Exception {
        iniciar(TipoEventoGlobal.INUNDACION);
        partidas.abandonarPartida(codigo, jugadores.get(2));
        eventos.tirar(codigo, jugadores.get(0), 5, 1);
        eventos.tirar(codigo, jugadores.get(1), 5, 1);
        assertEquals(true, eventos.consultar(codigo, jugadores.get(0)).get("resuelto"));
        assertThrows(SecurityException.class, () -> eventos.tirar(codigo, jugadores.get(2), 5, 1));
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueDosPeticionesRepitenUnaTiradaCuandoSeEjecutanSimultaneamenteDaComoResultadoUnSoloRegistro() throws Exception {
        iniciar(TipoEventoGlobal.INUNDACION);
        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Boolean> tirar = () -> {
                inicio.await();
                try { eventos.tirar(codigo, jugadores.get(0), 5, 1); return true; }
                catch (IllegalStateException esperado) { return false; }
            };
            Future<Boolean> primera = ejecutor.submit(tirar);
            Future<Boolean> segunda = ejecutor.submit(tirar);
            inicio.countDown();
            assertNotEquals(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, ((Map<?, ?>) eventos.consultar(codigo, jugadores.get(0)).get("tiradas")).size());
        verify(dados, times(1)).tirarDados();
    }

    @Test
    void dadoQueLaActivacionSeRevierteCuandoSeDeshaceLaTransaccionDaComoResultadoNingunEfectoNiNotificacion() {
        when(aleatorio.nextInt(6)).thenReturn(TipoEventoGlobal.FIAT_600.ordinal());
        transaccion.executeWithoutResult(estado -> {
            partidas.avanzarTurno(codigo);
            verifyNoInteractions(mensajeria, temporizador);
            estado.setRollbackOnly();
        });
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        verifyNoInteractions(mensajeria, temporizador);
    }

    @Test
    void dadoQueSeDuplicaLaUltimaConfirmacionCuandoSeProcesaEnParaleloDaComoResultadoUnaSolaReanudacion() throws Exception {
        iniciar(TipoEventoGlobal.FIAT_600);
        eventos.confirmar(codigo, jugadores.get(0), 5);
        eventos.confirmar(codigo, jugadores.get(1), 5);
        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Map<String, Object>> confirmar = () -> {
                inicio.await();
                return eventos.confirmar(codigo, jugadores.get(2), 5);
            };
            var primera = ejecutor.submit(confirmar);
            var segunda = ejecutor.submit(confirmar);
            inicio.countDown();
            assertEquals(false, primera.get(10, TimeUnit.SECONDS).get("activo"));
            assertEquals(false, segunda.get(10, TimeUnit.SECONDS).get("activo"));
        }
        assertEquals(2400, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        verify(temporizador, times(1)).programarTemporizador(eq(codigo), anyInt(), any());
    }

    @Test
    void dadoQueAlguienIntentaUsarOtroIdCuandoTiraDaComoResultadoQueSoloCuentaLaSesionAutenticada() {
        iniciar(TipoEventoGlobal.INUNDACION);
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("id", 99999L);
        solicitud.getSession().setAttribute("NOMBRE", "Participante 0");
        solicitud.addParameter("idJugador", jugadores.get(1).toString());
        controlador.tirar(codigo, 5, 1, solicitud);
        var tiradas = partidas.obtenerPartida(codigo).getEventoGlobal().getTiradas();
        assertTrue(tiradas.containsKey("1:" + jugadores.get(0)));
        assertFalse(tiradas.containsKey("1:" + jugadores.get(1)));
        solicitud.getSession().setAttribute("NOMBRE", "Intruso");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controlador.consultar(codigo, solicitud));
    }

    @Test
    void dadoQueRoboTieneSaldoSuficienteCuandoSeResuelveDaComoResultadoUnDescuentoDeQuinientos() {
        iniciar(TipoEventoGlobal.ROBO);
        assertEquals(1500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertTrue(eventos.consultar(codigo, jugadores.get(0)).get("consecuencia").toString().contains("$500"));
        confirmarTodos();
        assertEquals(1500, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
    }

    @Test
    void dadoQueRoboNoTieneSaldoCuandoSeConsultaDaComoResultadoUnaDeudaDeQuinientosSinSaldoNegativo() {
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        assertEquals(500, quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)).getMontoPendiente());
        assertEquals(100, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        eventos.consultar(codigo, jugadores.get(0));
        assertEquals(500, quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)).getMontoPendiente());
    }

    @Test
    void dadoQueHayDeudaGlobalCuandoSeAceptaUnaOfertaDaComoResultadoTransferenciaYPagoUnaSolaVez() {
        Long propiedad = crearPropiedad(1, 0, 0);
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        var oferta = ofertas.crearOferta(partidas.obtenerPartida(codigo), 1, jugadores.get(0), jugadores.get(1), 600);
        confirmarTodos();
        ofertas.aceptar(partidas.obtenerPartida(codigo), oferta.id(), jugadores.get(1));
        assertEquals(jugadores.get(1), propiedades.obtenerPropiedadExistente(propiedad).getPropietario().getId());
        assertEquals(200, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertEquals(1400, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
        assertNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        assertFalse(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertThrows(com.tallerwebi.dominio.excepcion.OfertaNoDisponibleException.class,
                () -> ofertas.aceptar(partidas.obtenerPartida(codigo), oferta.id(), jugadores.get(1)));
        assertEquals(200, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
    }

    @Test
    void dadoQueHayDeudaGlobalCuandoSeRechazaUnaOfertaDaComoResultadoPropiedadYSaldosSinCambios() {
        Long propiedad = crearPropiedad(1, 0, 0);
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        var oferta = ofertas.crearOferta(partidas.obtenerPartida(codigo), 1, jugadores.get(0), jugadores.get(1), 600);
        ofertas.rechazar(codigo, oferta.id(), jugadores.get(1));
        assertEquals(jugadores.get(0), propiedades.obtenerPropiedadExistente(propiedad).getPropietario().getId());
        assertEquals(100, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
        assertNotNull(quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)));
        assertTrue(ofertas.ofertasPendientes(codigo, jugadores.get(1)).isEmpty());
    }

    @Test
    void dadoQueLaOfertaNoAlcanzaParaLaDeudaCuandoSeAceptaDaComoResultadoDeudaPendienteYEventoPausado() {
        crearPropiedad(1, 0, 0);
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        var oferta = ofertas.crearOferta(partidas.obtenerPartida(codigo), 1, jugadores.get(0), jugadores.get(1), 200);
        ofertas.aceptar(partidas.obtenerPartida(codigo), oferta.id(), jugadores.get(1));
        assertEquals(300, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertEquals(500, quiebras.obtenerDeudaPendiente(codigo, jugadores.get(0)).getMontoPendiente());
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueNoTieneDeudaCuandoIntentaOfertarDuranteUnEventoDaComoResultadoAccionBloqueada() {
        crearPropiedad(1, 0, 0);
        iniciar(TipoEventoGlobal.FIAT_600);
        assertThrows(IllegalStateException.class, () -> ofertas.crearOferta(
                partidas.obtenerPartida(codigo), 1, jugadores.get(0), jugadores.get(1), 200));
        assertThrows(com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException.class,
                () -> propiedades.prepararCompra(codigo, 1));
    }

    @Test
    void dadoQueElCompradorNoTieneSaldoCuandoAceptaUnaOfertaParaDeudaDaComoResultadoNingunaTransferencia() {
        Long propiedad = crearPropiedad(1, 0, 0);
        cambiarDinero(jugadores.get(0), 100);
        iniciar(TipoEventoGlobal.ROBO);
        var oferta = ofertas.crearOferta(partidas.obtenerPartida(codigo), 1, jugadores.get(0), jugadores.get(1), 3000);
        assertThrows(com.tallerwebi.dominio.excepcion.DineroInsuficienteException.class,
                () -> ofertas.aceptar(partidas.obtenerPartida(codigo), oferta.id(), jugadores.get(1)));
        assertEquals(jugadores.get(0), propiedades.obtenerPropiedadExistente(propiedad).getPropietario().getId());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(1)).getDinero());
        assertEquals(1, ofertas.ofertasPendientes(codigo, jugadores.get(1)).size());
    }

    @TestFactory
    Stream<DynamicTest> dadoQueCaeEnPropiedadLibrePorEventoCuandoCompraDaComoResultadoCompraOpcionalSinAvanzarTurnos() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            iniciar(tipo);
            tirarTodos();
            assertEquals(5, eventos.consultar(codigo, jugadores.get(0)).get("compraPendiente"));
            assertThrows(IllegalStateException.class, () -> propiedades.responderCompraDeEvento(codigo, jugadores.get(0), 5, true));
            confirmarTodos();
            partidas.avanzarTurno(codigo);
            Long jugadorActual = partidas.obtenerJugadorActual(codigo).getId();
            Integer ronda = partidas.obtenerPartida(codigo).getRondaActual();
            Integer finalizados = partidas.obtenerPartida(codigo).getTurnosFinalizadosEnRonda();
            var respuesta = controladorPropiedades().responderCompraDeEvento(codigo, Map.of("ronda", 5, "aceptar", true), sesionPropietario());
            assertEquals(200, respuesta.getStatusCode().value(), () -> respuesta.getBody().toString());
            assertEquals(1700, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertEquals(jugadores.get(0), transaccion.execute(estado -> casilleros.buscarCasilleroPropiedad(partidas.obtenerPartida(codigo), 5).getPropiedad().getPropietario().getId()));
            assertEquals(jugadorActual, partidas.obtenerJugadorActual(codigo).getId());
            assertEquals(ronda, partidas.obtenerPartida(codigo).getRondaActual());
            assertEquals(finalizados, partidas.obtenerPartida(codigo).getTurnosFinalizadosEnRonda());
            assertNull(eventos.consultar(codigo, jugadores.get(0)).get("compraPendiente"));
            assertEquals(409, controladorPropiedades().responderCompraDeEvento(codigo, Map.of("ronda", 5, "aceptar", true), sesionPropietario()).getStatusCode().value());
            assertEquals(1700, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        });
    }

    @TestFactory
    Stream<DynamicTest> dadoQueCaeEnPropiedadLibrePorEventoCuandoRechazaLaCompraDaComoResultadoNingunDescuento() {
        return destinosDeMovimiento(tipo -> {
            prepararDestino(tipo, 5);
            iniciar(tipo);
            tirarTodos();
            confirmarTodos();
            Long actual = partidas.obtenerJugadorActual(codigo).getId();
            assertEquals(200, controladorPropiedades().responderCompraDeEvento(codigo, Map.of("ronda", 5, "aceptar", false), sesionPropietario()).getStatusCode().value());
            assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
            assertNull(eventos.consultar(codigo, jugadores.get(0)).get("compraPendiente"));
            assertEquals(actual, partidas.obtenerJugadorActual(codigo).getId());
        });
    }

    @Test
    void dadoQueLaPropiedadYaNoEstaLibreCuandoAceptaLaCompraDeEventoDaComoResultadoNingunDescuento() {
        prepararDestino(TipoEventoGlobal.INUNDACION, 5);
        iniciar(TipoEventoGlobal.INUNDACION);
        tirarTodos();
        confirmarTodos();
        crearPropiedad(5, 1, 0);
        assertThrows(com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException.class,
                () -> propiedades.responderCompraDeEvento(codigo, jugadores.get(0), 5, true));
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
    }

    @Test
    void dadoQueDosPartidasCompartenLosServiciosCuandoUnaConfirmaDaComoResultadoLaOtraSinCambios() {
        // Preparación
        iniciar(TipoEventoGlobal.FIAT_600);
        String primerCodigo = codigo;
        List<Long> primerosJugadores = List.copyOf(jugadores);
        preparar();
        iniciar(TipoEventoGlobal.INUNDACION);
        Map<String, Object> segundoEstado = eventos.consultar(codigo, jugadores.get(0));

        // Ejecución
        primerosJugadores.forEach(jugador -> eventos.confirmar(primerCodigo, jugador, 5));

        // Verificación
        assertFalse(partidas.obtenerPartida(primerCodigo).getEventoGlobal().estaActivo());
        assertEquals(2400, partidas.obtenerJugadorPorId(primerosJugadores.get(0)).getDinero());
        assertEquals(segundoEstado, eventos.consultar(codigo, jugadores.get(0)));
        assertEquals(1, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        tirarTodos();
        assertEquals(28, partidas.obtenerJugadorPorId(jugadores.get(0)).getPosicionActual());
        assertEquals(2000, partidas.obtenerJugadorPorId(jugadores.get(0)).getDinero());
        assertEquals(2400, partidas.obtenerJugadorPorId(primerosJugadores.get(0)).getDinero());
        assertEquals(1, partidas.obtenerJugadorPorId(primerosJugadores.get(0)).getPosicionActual());
    }

    private ControladorPropiedadesPartida controladorPropiedades() {
        return new ControladorPropiedadesPartida(partidas, mensajeria, casilleros, propiedades,
                quiebras, new ServicioParticipacionImpl(), ofertas);
    }

    private MockHttpServletRequest sesionPropietario() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "Participante 0");
        return solicitud;
    }

    private void iniciar(TipoEventoGlobal tipo) {
        when(aleatorio.nextInt(6)).thenReturn(tipo.ordinal());
        partidas.avanzarTurno(codigo);
        assertTrue(partidas.obtenerPartida(codigo).getEventoGlobal().estaActivo());
        assertEquals(tipo, partidas.obtenerPartida(codigo).getEventoGlobal().getEvento().getTipoEventoGlobal());
        if (!tipo.requiereDados()) {
            assertEquals(jugadores.get(0), partidas.obtenerPartida(codigo).getEventoGlobal().getSeleccionado());
        }
    }
    private void tirarTodos() { jugadores.forEach(jugador -> eventos.tirar(codigo, jugador, 5, 1)); }
    private void confirmarTodos() { jugadores.forEach(jugador -> eventos.confirmar(codigo, jugador, 5)); }
    private void cambiarDinero(Long jugador, int dinero) {
        transaccion.executeWithoutResult(estado -> sesiones.getCurrentSession().find(Jugador.class, jugador).setDinero(dinero));
    }
}
