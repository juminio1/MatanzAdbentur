package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.eventos.EventoGlobalActualizado;
import com.tallerwebi.dominio.eventos.EventoGlobalFinalizado;
import com.tallerwebi.dominio.resultados.EstadoEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServicioEventoTest {

    private ServicioEvento servicioEvento;
    private RepositorioEvento repositorioEventoMock;
    private ServicioDado servicioDadoMock;
    private ApplicationEventPublisher publicadorEventosMock;
    private GestorTemporizadorPartida gestorTemporizadorMock;
    private RepositorioPartida repositorioPartidaMock;

    @BeforeEach
    public void init() {
        repositorioEventoMock = mock(RepositorioEvento.class);
        servicioDadoMock = mock(ServicioDado.class);
        publicadorEventosMock = mock(ApplicationEventPublisher.class);
        gestorTemporizadorMock = mock(GestorTemporizadorPartida.class);
        repositorioPartidaMock = mock(RepositorioPartida.class);
        servicioEvento = new ServicioEventoImpl(repositorioEventoMock, servicioDadoMock, publicadorEventosMock, gestorTemporizadorMock, repositorioPartidaMock);
    }

    private Jugador crearJugador(Long id, Integer dinero, Integer posicion) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setDinero(dinero);
        jugador.setPosicionActual(posicion);
        return jugador;
    }

    private Partida crearPartida(String codigo, Integer ronda, Jugador... jugadores) {
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setRondaActual(ronda);
        partida.setJugadores(List.of(jugadores));
        return partida;
    }

    private Evento crearEvento(TipoEfecto tipoEfecto, int valor) {
        Evento evento = new Evento();
        evento.setTipoEfecto(tipoEfecto);
        evento.setValor(valor);
        evento.setDescripcion("Evento de prueba");
        return evento;
    }

    private EventoGlobal crearEventoGlobal(Long id, TipoEventoGlobal tipoEventoGlobal, TipoEfecto tipoEfecto, Integer valor) {
        EventoGlobal evento = new EventoGlobal();
        evento.setId(id);
        evento.setTipoEventoGlobal(tipoEventoGlobal);
        evento.setTipoEfecto(tipoEfecto);
        evento.setValor(valor);
        evento.setDescripcion("Evento global de prueba");
        return evento;
    }

    private EventoGlobal crearInundacion() {
        return crearEventoGlobal(1L, TipoEventoGlobal.INUNDACION, TipoEfecto.MOVER_CASILLEROS, -3);
    }

    private EventoGlobal crearPiquete() {
        return crearEventoGlobal(2L, TipoEventoGlobal.PIQUETE, TipoEfecto.VOLVER_INICIO, 0);
    }

    private EventoGlobal crearAfip() {
        return crearEventoGlobal(3L, TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0);
    }

    private EventoGlobal crearKevin() {
        return crearEventoGlobal(4L, TipoEventoGlobal.EL_KEVIN, TipoEfecto.PERDER_PLATA, 100);
    }

    @Test
    public void dadoQueElRepositorioTieneUnEventoCuandoSeObtieneUnoAleatorioYDaElEvento() {
        Evento evento = crearEvento(TipoEfecto.GANAR_PLATA, 300);
        when(repositorioEventoMock.obtenerEventoAleatorio()).thenReturn(evento);

        Evento resultado = servicioEvento.obtenerEventoAleatorio();

        assertEquals(evento, resultado);
        verify(repositorioEventoMock).obtenerEventoAleatorio();
    }

    @Test
    public void dadoQueElRepositorioNoTieneEventosCuandoSeBuscaUnoAleatorioYDaNull() {
        when(repositorioEventoMock.obtenerEventoAleatorio()).thenReturn(null);

        Evento resultado = servicioEvento.obtenerEventoAleatorio();

        assertNull(resultado);
    }

    @Test
    public void dadoQueElRepositorioTieneUnEventoGlobalCuandoSeObtieneUnoAleatorioYDaElEventoGlobal() {
        EventoGlobal evento = crearInundacion();
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(evento);

        Evento resultado = servicioEvento.obtenerEventosGlobalesAleatorio();

        assertSame(evento, resultado);
        verify(repositorioEventoMock).obtenerEventoGlobalAleatorio();
    }

    //Tests de aplicar los efectos de los eventos
    @Test
    public void dadoQueUnJugadorTieneMilQuinientosCuandoGanaMilDaComoResultadoDosMilQuinientos() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 0);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.GANAR_PLATA, 1000));

        assertEquals(2500, jugador.getDinero());
    }

    @Test
    public void dadoQueUnJugadorTieneMilQuinientosCuandoPierdeQuinientosDaComoResultadoMil() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 0);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.PERDER_PLATA, 500));

        assertEquals(1000, jugador.getDinero());
    }

    @Test
    public void dadoQueUnJugadorNoPierdeTurnoCuandoRecibeEseEventoDaComoResultadoTurnoPerdido() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 0);
        assertFalse(jugador.getPierdeTurno());

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.PERDER_TURNO, 1));

        assertTrue(jugador.getPierdeTurno());
    }

    @Test
    public void dadoQueUnJugadorEstaEnCincoCuandoAvanzaDosDaComoResultadoSiete() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 5);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.MOVER_CASILLEROS, 2));

        assertEquals(7, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorEstaEnUnoCuandoRetrocedeDosDaComoResultadoVeintinueve() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 1);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.MOVER_CASILLEROS, -2));

        assertEquals(29, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorEstaEnVeintiochoCuandoAvanzaCincoDaComoResultadoTres() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 28);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.MOVER_CASILLEROS, 5));

        assertEquals(3, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorEstaEnCincoCuandoDebeIrAlCasilleroQuinceDaComoResultadoQuince() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 5);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.IR_A_CASILLERO, 15));

        assertEquals(15, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnJugadorEstaEnDiezCuandoDebeVolverAlInicioDaComoResultadoCero() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 10);

        servicioEvento.aplicarEvento(jugador, crearEvento(TipoEfecto.VOLVER_INICIO, 0));

        assertEquals(0, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueElEventoEsNullCuandoSeIntentaAplicarYDaEventoInexistenteException() {
        Jugador jugador = crearJugador(1L, 1500, 10);

        assertThrows(EventoInexistenteException.class, () -> servicioEvento.aplicarEvento(jugador, null));

        assertEquals(1500, jugador.getDinero());
        assertEquals(10, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueElEventoNoTieneTipoEfectoCuandoSeAplicaYDaEventoInexistenteException() {
        Jugador jugador = crearJugador(1L, 1500, 10);
        Evento evento = new Evento();
        evento.setValor(200);

        assertThrows(EventoInexistenteException.class, () -> servicioEvento.aplicarEvento(jugador, evento));

        assertEquals(1500, jugador.getDinero());
        assertEquals(10, jugador.getPosicionActual());
    }

    //Inicializar los eventos globales y validar sus datos
    @Test
    public void dadoQueElEventoGlobalEsNullCuandoSeIniciaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, null));
    }

    @Test
    public void dadoQueElEventoGlobalNoTieneTipoEfectoCuandoSeIniciaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        EventoGlobal evento = crearInundacion();
        evento.setTipoEfecto(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, evento));
    }

    @Test
    public void dadoQueElEventoGlobalNoTieneTipoGlobalCuandoSeIniciaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        EventoGlobal evento = crearInundacion();
        evento.setTipoEventoGlobal(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, evento));
    }

    @Test
    public void dadoQueLaPartidaEsNullCuandoSeIniciaUnEventoGlobalYDaExcepcion() {
        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.iniciarEventoGlobal(null, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeIniciaUnEventoGlobalYDaExcepcion() {
        Partida partida = crearPartida(null, 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneListaDeJugadoresCuandoSeIniciaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5);
        partida.setJugadores(null);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaEstaSinJugadoresCuandoSeIniciaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueYaHayInundacionActivaCuandoSeIniciaOtroEventoYDaExcepcion() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(EventoGlobalActivoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearPiquete()));
    }

    @Test
    public void dadoQueSeIniciaKevinConUnJugadorCuandoSeAplicaYDaMenosDinero() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);

        servicioEvento.iniciarEventoGlobal(partida, crearKevin());

        assertEquals(900, jugador.getDinero());
    }

    @Test
    public void dadoQueSeIniciaFiatSeiscientosCuandoSeAplicaYDaMasDinero() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);
        EventoGlobal fiat = crearEventoGlobal(
            5L, TipoEventoGlobal.FIAT_600, TipoEfecto.GANAR_PLATA, 400);

        servicioEvento.iniciarEventoGlobal(partida, fiat);

        assertEquals(1400, jugador.getDinero());
    }

    @Test
    public void dadoQueSeIniciaAccidenteSarmientoCuandoSeAplicaYDaPerderTurno() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);
        EventoGlobal accidente = crearEventoGlobal(
            6L, TipoEventoGlobal.ACCIDENTE_SARMIENTO, TipoEfecto.PERDER_TURNO, 1);

        servicioEvento.iniciarEventoGlobal(partida, accidente);

        assertTrue(jugador.getPierdeTurno());
    }

    @Test
    public void dadoQueKevinTieneDosJugadoresCuandoSeActivaYDaUnSoloAfectado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno, dos);

        servicioEvento.iniciarEventoGlobal(partida, crearKevin());

        assertEquals(1900, uno.getDinero() + dos.getDinero());
    }

    //Tests de resolución, tiradas y desempates
    @Test
    public void dadoQueFaltaUnJugadorPorTirarCuandoTiraElPrimeroYDaEventoPendiente() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 12);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(6, 4));

        ResultadoTirada resultado = servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        assertEquals(10, resultado.sumarDados());
        assertEquals(10, uno.getPosicionActual());
        assertEquals(12, dos.getPosicionActual());
    }

    @Test
    public void dadoQueTodosTiraronInundacionCuandoHayUnMayorYDaRetrocederTres() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 12);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(6, 4), new ResultadoTirada(3, 4));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(7, uno.getPosicionActual());
        assertEquals(12, dos.getPosicionActual());
        verify(servicioDadoMock, times(2)).tirarDados();
    }

    @Test
    public void dadoQueTodosTiraronPiqueteCuandoHayUnMenorYDaVolverAlInicio() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 12);
        Jugador dos = crearJugador(2L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearPiquete();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(2, 3), new ResultadoTirada(4, 5));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(0, uno.getPosicionActual());
        assertEquals(20, dos.getPosicionActual());
    }

    @Test
    public void dadoQueTodosTiraronAfipCuandoHayUnMayorYDaAlcanzarAlMasAdelantado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearAfip();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 6), new ResultadoTirada(2, 3));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(20, uno.getPosicionActual());
        assertEquals(20, dos.getPosicionActual());
    }

    @Test
    public void dadoQueDosJugadoresEmpatanCuandoVuelvenATirarYDaUnGanador() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4),
            new ResultadoTirada(3, 3), new ResultadoTirada(6, 3));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        assertEquals(10, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(10, uno.getPosicionActual());
        assertEquals(12, dos.getPosicionActual());
    }

    @Test
    public void dadoQueSeRepiteElEmpateCuandoTiranOtraVezYDaResolucionFinal() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 4), new ResultadoTirada(4, 5),
            new ResultadoTirada(3, 3), new ResultadoTirada(2, 4),
            new ResultadoTirada(6, 2), new ResultadoTirada(3, 4));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        assertEquals(10, uno.getPosicionActual());

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(7, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        verify(servicioDadoMock, times(6)).tirarDados();
    }

    @Test
    public void dadoQueDosDeTresEmpatanCuandoElTerceroIntentaDesempatarYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(6, 5), new ResultadoTirada(5, 6),
            new ResultadoTirada(2, 2));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento));
        verify(servicioDadoMock, times(3)).tirarDados();
    }

    @Test
    public void dadoQueUnJugadorYaTiroCuandoIntentaRepetirAntesDelOtroYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(3, 3));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
        verify(servicioDadoMock, times(1)).tirarDados();
    }

    @Test
    public void dadoQueLaSumaEsMenorADosCuandoSeRegistraYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(0, 1));

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
        assertEquals(10, uno.getPosicionActual());
    }

    @Test
    public void dadoQueLaSumaEsMayorADoceCuandoSeRegistraYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(7, 7));

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueLaSumaDeDadosEsNullCuandoSeRegistraYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        ResultadoTirada resultadoMock = mock(ResultadoTirada.class);
        when(resultadoMock.sumarDados()).thenReturn(null);
        when(servicioDadoMock.tirarDados()).thenReturn(resultadoMock);

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueLaPartidaEsNullCuandoUnJugadorTiraYDaDatosInvalidos() {
        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(null, crearJugador(1L, 1000, 10), crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorEsNullCuandoIntentaTirarYDaDatosInvalidos() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, null, crearInundacion()));
    }

    @Test
    public void dadoQueElEventoEsNullCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, null));
    }

    @Test
    public void dadoQueElEventoNoTieneTipoEfectoCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setTipoEfecto(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueElEventoNoTieneTipoGlobalCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setTipoEventoGlobal(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida(null, 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneJugadoresCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        partida.setJugadores(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorNoTieneIdCuandoSeIntentaTirarYDaDatosInvalidos() {
        Jugador uno = crearJugador(null, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoIntentaTirarYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador ajeno = crearJugador(2L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, ajeno, evento));
    }

    @Test
    public void dadoQueNoExisteEventoGlobalActivoCuandoUnJugadorTiraYDaExcepcion() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElEventoActivoTieneOtroIdCuandoUnJugadorTiraYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearPiquete()));
    }

    @Test
    public void dadoQueElEventoActivoNoTieneIdCuandoSeIntentaTirarYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setId(null);
        servicioEvento.iniciarEventoGlobal(partida, evento);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueSeResuelveLaInundacionCuandoSeIntentaTirarOtraVezYDaEventoInactivo() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(3, 4));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        assertEquals(7, uno.getPosicionActual());
        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueFinalizaUnEventoCuandoSeIniciaOtroYDaNuevoEventoPermitido() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal primerEvento = crearInundacion();
        EventoGlobal segundoEvento = crearPiquete();
        servicioEvento.iniciarEventoGlobal(partida, primerEvento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(3, 4), new ResultadoTirada(2, 4));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, primerEvento);
        servicioEvento.iniciarEventoGlobal(partida, segundoEvento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, segundoEvento);

        assertEquals(0, uno.getPosicionActual());
    }

    @Test
    public void dadoQueExistenDosPartidasCuandoSeResuelveUnaYDaLaOtraPendiente() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 20);
        Partida partidaUno = crearPartida("ABC", 5, uno);
        Partida partidaDos = crearPartida("XYZ", 5, dos);
        EventoGlobal eventoUno = crearInundacion();
        EventoGlobal eventoDos = crearPiquete();
        servicioEvento.iniciarEventoGlobal(partidaUno, eventoUno);
        servicioEvento.iniciarEventoGlobal(partidaDos, eventoDos);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(4, 3), new ResultadoTirada(4, 5));

        servicioEvento.realizarTiradaEventoGlobal(partidaUno, uno, eventoUno);
        assertEquals(7, uno.getPosicionActual());
        assertEquals(20, dos.getPosicionActual());
        servicioEvento.realizarTiradaEventoGlobal(partidaDos, dos, eventoDos);

        assertEquals(0, dos.getPosicionActual());
    }

    //Test de activación por rondas
    @Test
    public void dadoQueLaPartidaEsNullCuandoSeVerificaActivacionYDaExcepcion() {
        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(null));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeVerificaActivacionYDaExcepcion() {
        Partida partida = crearPartida(null, 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueLaPartidaTieneCodigoVacioCuandoSeVerificaActivacionYDaExcepcion() {
        Partida partida = crearPartida("  ", 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueLaRondaEsNullCuandoSeVerificaActivacionYDaNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", null, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCeroCuandoSeVerificaActivacionYDaNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", 0, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCuatroCuandoSeVerificaActivacionYDaNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", 4, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCincoCuandoSeVerificaActivacionYDaAplicarEvento() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(partida);

        assertEquals(900, uno.getDinero());
        verify(repositorioEventoMock, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueYaSeActivoEnLaRondaCincoCuandoSeVerificaOtraVezYDaUnaSolaActivacion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(partida);
        servicioEvento.verificarActivacionEventoGlobal(partida);

        assertEquals(900, uno.getDinero());
        verify(repositorioEventoMock, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueSeActivoEnLaRondaCincoCuandoSeLlegaADiezYDaNuevoEvento() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(partida);
        partida.setRondaActual(10);
        servicioEvento.verificarActivacionEventoGlobal(partida);

        assertEquals(800, uno.getDinero());
        verify(repositorioEventoMock, times(2)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueNoHayEventoGlobalEnRepositorioCuandoSeActivaLaRondaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueElRepositorioDevuelveEventoComunCuandoSeActivaLaRondaYDaExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio())
            .thenReturn(crearEvento(TipoEfecto.GANAR_PLATA, 300));

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueQuedaUnEventoPendienteCuandoLlegaOtraRondaDeActivacionYDaExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearPiquete());
        servicioEvento.verificarActivacionEventoGlobal(partida);
        partida.setRondaActual(10);

        assertThrows(EventoGlobalActivoException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
        verify(repositorioEventoMock, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueHayDosPartidasEnRondaCincoCuandoSeVerificaActivacionYDaDosEventos() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 5);
        Partida primera = crearPartida("ABC", 5, uno);
        Partida segunda = crearPartida("XYZ", 5, dos);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(primera);
        servicioEvento.verificarActivacionEventoGlobal(segunda);

        assertEquals(900, uno.getDinero());
        assertEquals(900, dos.getDinero());
        verify(repositorioEventoMock, times(2)).obtenerEventoGlobalAleatorio();
    }


    // Nuevas pruebas: estado del evento, temporizadores, tiradas automáticas y desempates.
    // El mock del gestor no espera 15 segundos reales: almacena las acciones programadas.
    private List<Runnable> prepararVencimientosSimulados() {
        List<Runnable> vencimientos = new ArrayList<>();
        doAnswer(invocacion -> {
            vencimientos.add(invocacion.getArgument(2));
            return null;
        }).when(gestorTemporizadorMock).programarTemporizador(
            anyString(), anyInt(), any(Runnable.class));
        return vencimientos;
    }

    @Test
    public void dadoQueElCodigoEsNullCuandoSeConsultaElEventoYDaResultadoInactivo() {
        Boolean activo = servicioEvento.verificarSiHayEventoGlobalActivo(null);

        assertEquals(false, activo);
    }

    @Test
    public void dadoQueNoHayEventoCuandoSeConsultaLaPartidaYDaResultadoInactivo() {
        Boolean activo = servicioEvento.verificarSiHayEventoGlobalActivo("ABC");

        assertEquals(false, activo);
    }

    @Test
    public void dadoQueSeIniciaInundacionCuandoSeConsultaLaPartidaYDaResultadoActivo() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(gestorTemporizadorMock).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"),
            org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueTerminaUnEventoCuandoSeConsultaSuEstadoYDaInactivoYPublicaLaFinalizacion() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, jugador);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 4));

        servicioEvento.realizarTiradaEventoGlobal(partida, jugador, evento);

        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        assertEquals(7, jugador.getPosicionActual());
        verify(gestorTemporizadorMock).cancelarTemporizador("evento-global:ABC");
        verify(publicadorEventosMock).publishEvent(
            new com.tallerwebi.dominio.eventos.EventoGlobalFinalizado("ABC"));
    }

    @Test
    public void dadoQueFaltaOtroJugadorCuandoTiraElPrimeroYDaEventoTodaviaActivo() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(5, 3));

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(publicadorEventosMock, never()).publishEvent(any(EventoGlobalFinalizado.class));
        verify(gestorTemporizadorMock, never()).cancelarTemporizador("evento-global:ABC");
    }

    @Test
    public void dadoQueLaPartidaEsNullCuandoSePidenTiradasAutomaticasYDaExcepcion() {
        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.realizarTiradasAutomaticasPendientes(null));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSePidenTiradasAutomaticasYDaExcepcion() {
        Partida partida = crearPartida(null, 5, crearJugador(1L, 1000, 10));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.realizarTiradasAutomaticasPendientes(partida));
    }

    @Test
    public void dadoQueNoExisteEventoCuandoSePidenTiradasAutomaticasYDaCeroTiradas() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        verify(servicioDadoMock, never()).tirarDados();
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
    }

    @Test
    public void dadoQueNadieTiroCuandoSeEjecutanLasAutomaticasYDaTodasLasTiradas() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(6, 5), new ResultadoTirada(3, 4), new ResultadoTirada(1, 2));

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        assertEquals(7, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        assertEquals(20, tres.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(3)).tirarDados();
    }

    @Test
    public void dadoQueUnoYaTiroCuandoSeEjecutanAutomaticasYDaSoloLaTiradaPendiente() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(6, 5), new ResultadoTirada(2, 3));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        assertEquals(7, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        verify(servicioDadoMock, times(2)).tirarDados();
    }

    @Test
    public void dadoQueTodosYaTiraronCuandoSeSolicitanAutomaticasYDaNingunaTiradaExtra() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 3));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        assertEquals(7, uno.getPosicionActual());
        verify(servicioDadoMock, times(1)).tirarDados();
    }

    @Test
    public void dadoQueHayCuatroJugadoresCuandoFaltaUnoPorTirarYDaUnaSolaTiradaAutomatica() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Jugador cuatro = crearJugador(4L, 1000, 25);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres, cuatro);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(2, 2), new ResultadoTirada(3, 3),
            new ResultadoTirada(4, 4), new ResultadoTirada(6, 5));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        assertEquals(22, cuatro.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(4)).tirarDados();
    }

    @Test
    public void dadoQueHayTresEmpatadosCuandoSeDesempataYDaUnUnicoGanador() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4), new ResultadoTirada(4, 6),
            new ResultadoTirada(1, 3), new ResultadoTirada(2, 4), new ResultadoTirada(5, 6));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);

        assertEquals(10, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        assertEquals(17, tres.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(6)).tirarDados();
    }

    @Test
    public void dadoQueEmpatanLosCuatroCuandoSeRepiteElEmpateYDaOtraRondaDeTiradas() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Jugador cuatro = crearJugador(4L, 1000, 25);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres, cuatro);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4),
            new ResultadoTirada(4, 6), new ResultadoTirada(5, 5),
            new ResultadoTirada(3, 3), new ResultadoTirada(2, 4),
            new ResultadoTirada(5, 1), new ResultadoTirada(4, 2),
            new ResultadoTirada(2, 3), new ResultadoTirada(5, 6),
            new ResultadoTirada(1, 3), new ResultadoTirada(3, 5));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento);

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento);

        assertEquals(12, dos.getPosicionActual());
        assertEquals(10, uno.getPosicionActual());
        assertEquals(20, tres.getPosicionActual());
        assertEquals(25, cuatro.getPosicionActual());
        verify(gestorTemporizadorMock, times(3)).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"),
            org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueEmpatanCuatroCuandoSoloDosPersistenYDaDesempateEntreDos() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Jugador cuatro = crearJugador(4L, 1000, 25);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres, cuatro);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4),
            new ResultadoTirada(4, 6), new ResultadoTirada(5, 5),
            new ResultadoTirada(5, 6), new ResultadoTirada(6, 5),
            new ResultadoTirada(2, 3), new ResultadoTirada(1, 2),
            new ResultadoTirada(4, 3), new ResultadoTirada(6, 5));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento);

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento));
        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, cuatro, evento));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        assertEquals(12, dos.getPosicionActual());
        assertEquals(10, uno.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(10)).tirarDados();
    }

    @Test
    public void dadoQueHayEmpateCuandoSePidenAutomaticasYDaSoloTiradasParaEmpatados() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(4, 6),
            new ResultadoTirada(2, 3), new ResultadoTirada(4, 3), new ResultadoTirada(6, 5));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        servicioEvento.realizarTiradasAutomaticasPendientes(partida);

        assertEquals(12, dos.getPosicionActual());
        assertEquals(20, tres.getPosicionActual());
        verify(servicioDadoMock, times(5)).tirarDados();
    }

    @Test
    public void dadoQueVenceElTiempoCuandoNadieTiroYDaTiradasAutomaticas() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(6, 5), new ResultadoTirada(2, 3));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        vencimientos.get(0).run();

        assertEquals(7, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        verify(repositorioPartidaMock).buscarPartidaActivaPorCodigoUnico("ABC");
        verify(servicioDadoMock, times(2)).tirarDados();
    }

    @Test
    public void dadoQueVenceElTiempoCuandoSoloUnoTiroYDaUnaTiradaAutomatica() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(4, 6), new ResultadoTirada(2, 3));
        servicioEvento.iniciarEventoGlobal(partida, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        vencimientos.get(0).run();

        assertEquals(7, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        verify(servicioDadoMock, times(2)).tirarDados();
    }

    @Test
    public void dadoQueFinalizoElEventoCuandoVenceElTemporizadorAntiguoYDaNingunaTirada() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(3, 4));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        vencimientos.get(0).run();

        assertEquals(7, uno.getPosicionActual());
        verify(servicioDadoMock, times(1)).tirarDados();
        verify(repositorioPartidaMock, never()).buscarPartidaActivaPorCodigoUnico("ABC");
    }

    @Test
    public void dadoQueHuboDesempateCuandoVenceElTemporizadorAnteriorYDaNingunaTiradaNueva() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4));
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        vencimientos.get(0).run();

        assertEquals(10, uno.getPosicionActual());
        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(2)).tirarDados();
        verify(repositorioPartidaMock, never()).buscarPartidaActivaPorCodigoUnico("ABC");
        verify(gestorTemporizadorMock, times(2)).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"),
            org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueVenceElDesempateCuandoFaltanDadosYDaUnUnicoGanador() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(5, 5), new ResultadoTirada(6, 4),
            new ResultadoTirada(2, 3), new ResultadoTirada(5, 6));
        servicioEvento.iniciarEventoGlobal(partida, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        vencimientos.get(1).run();

        assertEquals(12, dos.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(servicioDadoMock, times(4)).tirarDados();
    }

    @Test
    public void dadoQueVenceElTiempoCuandoNoExisteLaPartidaYDaExcepcionDeEstado() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> vencimientos.get(0).run());

        assertEquals("No se encontró la partida del evento global", exception.getMessage());
        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueVenceElTiempoCuandoLosDadosSonInvalidosYDaExcepcionConCausa() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(0, 1));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> vencimientos.get(0).run());

        assertEquals(TiradaDadoInvalidaException.class, exception.getCause().getClass());
        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
    }

    @Test
    public void dadoQueHayDosPartidasCuandoVenceUnaYDaOtraPartidaSinCambios() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 20);
        Partida primera = crearPartida("ABC", 5, uno);
        Partida segunda = crearPartida("XYZ", 5, dos);
        servicioEvento.iniciarEventoGlobal(primera, crearInundacion());
        servicioEvento.iniciarEventoGlobal(segunda, crearPiquete());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(primera);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 5));

        vencimientos.get(0).run();

        assertEquals(7, uno.getPosicionActual());
        assertEquals(20, dos.getPosicionActual());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("XYZ"));
        verify(gestorTemporizadorMock).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:XYZ"),
            org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueHayCuatroEmpatadosCuandoVenceLaPrimeraEtapaYDaNuevoPlazo() throws Exception {
        List<Runnable> vencimientos = prepararVencimientosSimulados();
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Jugador cuatro = crearJugador(4L, 1000, 25);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres, cuatro);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(
            new ResultadoTirada(4, 5), new ResultadoTirada(5, 4),
            new ResultadoTirada(6, 3), new ResultadoTirada(3, 6));

        vencimientos.get(0).run();

        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        assertEquals(10, uno.getPosicionActual());
        assertEquals(15, dos.getPosicionActual());
        assertEquals(20, tres.getPosicionActual());
        assertEquals(25, cuatro.getPosicionActual());
        verify(gestorTemporizadorMock, times(2)).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"),
            org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }



    // Tests de la integración REST y las notificaciones de eventos globales.

    @Test
    public void dadoQueNoHayEventoActivoCuandoSeConsultaSuEstadoYDaNull() {
        EstadoEventoGlobal resultado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertNull(resultado);
        verify(repositorioPartidaMock, never()).buscarPartidaActivaPorCodigoUnico("ABC");
    }

    @Test
    public void dadoQueElEventoEstaActivoPeroLaPartidaNoExisteCuandoSeConsultaYDaNull() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(null);

        EstadoEventoGlobal resultado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertNull(resultado);
        verify(repositorioPartidaMock).buscarPartidaActivaPorCodigoUnico("ABC");
    }

    @Test
    public void dadoQueComienzaUnEventoCuandoSeConsultaSuEstadoYDaLosDatosIniciales() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        servicioEvento.iniciarEventoGlobal(partida, evento);

        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertEquals("ABC", estado.codigoPartida());
        assertEquals("INICIADO", estado.fase());
        assertEquals(TipoEventoGlobal.INUNDACION, estado.tipo());
        assertEquals("Evento global de prueba", estado.descripcion());
        assertEquals(0, estado.etapa());
        assertEquals(15, estado.segundosRestantes());
        assertEquals(List.of(1L, 2L), estado.participantes());
        assertEquals(0, estado.tiradas().size());
        assertNull(estado.idJugadorElegido());
    }

    @Test
    public void dadoQueQuedanOchoSegundosCuandoSeConsultaElEventoYDaElTiempoRestante() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(gestorTemporizadorMock.obtenerSegundosRestantes("evento-global:ABC")).thenReturn(8);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertEquals(8, estado.segundosRestantes());
        verify(gestorTemporizadorMock, times(2)).obtenerSegundosRestantes("evento-global:ABC");
    }

    @Test
    public void dadoQueUnJugadorYaTiroCuandoSeConsultaElEventoYDaLaTiradaRegistrada() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 3));
        servicioEvento.iniciarEventoGlobal(partida, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertEquals("INICIADO", estado.fase());
        assertEquals(7, estado.tiradas().get(1L));
        assertEquals(List.of(1L, 2L), estado.participantes());
        verify(servicioDadoMock).tirarDados();
    }

    @Test
    public void dadoQueHayDesempateCuandoSeConsultaElEstadoYDaSoloLosParticipantesEmpatados() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        EventoGlobal evento = crearInundacion();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(5, 5), new ResultadoTirada(6, 4), new ResultadoTirada(2, 3));
        servicioEvento.iniciarEventoGlobal(partida, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, tres, evento);

        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal("ABC");

        assertEquals("DESEMPATE", estado.fase());
        assertEquals(1, estado.etapa());
        assertEquals(List.of(1L, 2L), estado.participantes());
        assertEquals(0, estado.tiradas().size());
        assertEquals(true, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeTiraDesdeRestYDaPartidaNoEncontradaException() {
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(null);

        assertThrows(PartidaNoEncontradaException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 1L));

        verify(repositorioPartidaMock).buscarPartidaActivaPorCodigoUnico("ABC");
        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeTiraDesdeRestYDaPartidaNoEncontradaException() {
        Partida partida = new Partida();
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);

        assertThrows(PartidaNoEncontradaException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 1L));

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueNoHayEventoActivoCuandoSeTiraDesdeRestYDaDatosInvalidos() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);

        assertThrows(DatosEnventoInvalidoExceptions.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 1L));

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueElIdDelJugadorEsNullCuandoSeTiraDesdeRestYDaJugadorNoEncontradoException() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(JugadorNoEncontradoException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", null));

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueLaListaDeJugadoresEsNullCuandoSeTiraDesdeRestYDaJugadorNoEncontradoException() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        partida.setJugadores(null);

        assertThrows(JugadorNoEncontradoException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 1L));

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoTiraDesdeRestYDaJugadorNoEncontradoException() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(JugadorNoEncontradoException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 99L));

        verify(servicioDadoMock, never()).tirarDados();
    }

    @Test
    public void dadoQueElJugadorEstaHabilitadoCuandoTiraDesdeRestYDaElResultadoDeDados() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(6, 3));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        ResultadoTirada resultado = servicioEvento.tirarDadosEventoGlobal("ABC", 1L);

        assertEquals(9, resultado.sumarDados());
        assertEquals(10, uno.getPosicionActual());
        assertEquals(9, servicioEvento.consultarEstadoEventoGlobal("ABC").tiradas().get(1L));
        verify(repositorioPartidaMock, times(2)).buscarPartidaActivaPorCodigoUnico("ABC");
        verify(servicioDadoMock).tirarDados();
    }

    @Test
    public void dadoQueElJugadorYaTiroCuandoRepiteDesdeRestYDaTiradaDadoInvalidaException() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 4));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        servicioEvento.tirarDadosEventoGlobal("ABC", 1L);

        assertThrows(TiradaDadoInvalidaException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 1L));

        verify(servicioDadoMock, times(1)).tirarDados();
    }

    @Test
    public void dadoQueUnJugadorPerdioElDesempateCuandoIntentaTirarDesdeRestYDaTiradaDadoInvalidaException() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Jugador tres = crearJugador(3L, 1000, 20);
        Partida partida = crearPartida("ABC", 5, uno, dos, tres);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(5, 5), new ResultadoTirada(6, 4), new ResultadoTirada(2, 3));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());
        servicioEvento.tirarDadosEventoGlobal("ABC", 1L);
        servicioEvento.tirarDadosEventoGlobal("ABC", 2L);
        servicioEvento.tirarDadosEventoGlobal("ABC", 3L);

        assertThrows(TiradaDadoInvalidaException.class, () -> servicioEvento.tirarDadosEventoGlobal("ABC", 3L));

        verify(servicioDadoMock, times(3)).tirarDados();
    }

    @Test
    public void dadoQueElEventoEstaActivoCuandoSeIniciaYDaNotificacionIniciado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        verify(publicadorEventosMock).publishEvent(argThat((Object publicado) ->
            publicado instanceof EventoGlobalActualizado actualizacion
            && "INICIADO".equals(actualizacion.estado().fase())
            && "ABC".equals(actualizacion.estado().codigoPartida())
            && actualizacion.estado().segundosRestantes().equals(15)
            && actualizacion.estado().participantes().equals(List.of(1L))));
    }

    @Test
    public void dadoQueUnJugadorTiraCuandoFaltanParticipantesYDaNotificacionTiradaConResultado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 5));
        when(gestorTemporizadorMock.obtenerSegundosRestantes("evento-global:ABC")).thenReturn(7);
        servicioEvento.iniciarEventoGlobal(partida, evento);

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        verify(publicadorEventosMock).publishEvent(argThat((Object publicado) ->
            publicado instanceof EventoGlobalActualizado actualizacion
            && "TIRADA".equals(actualizacion.estado().fase())
            && actualizacion.estado().tiradas().get(1L).equals(9)
            && actualizacion.estado().segundosRestantes().equals(7)));
        verify(publicadorEventosMock, never()).publishEvent(any(EventoGlobalFinalizado.class));
    }

    @Test
    public void dadoQueDosJugadoresEmpatanCuandoSePreparanNuevasTiradasYDaNotificacionDesempate() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador dos = crearJugador(2L, 1000, 15);
        Partida partida = crearPartida("ABC", 5, uno, dos);
        EventoGlobal evento = crearInundacion();
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(4, 5), new ResultadoTirada(5, 4));
        servicioEvento.iniciarEventoGlobal(partida, evento);
        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        servicioEvento.realizarTiradaEventoGlobal(partida, dos, evento);

        verify(publicadorEventosMock).publishEvent(argThat((Object publicado) ->
            publicado instanceof EventoGlobalActualizado actualizacion
            && "DESEMPATE".equals(actualizacion.estado().fase())
            && actualizacion.estado().etapa().equals(1)
            && actualizacion.estado().segundosRestantes().equals(15)
            && actualizacion.estado().participantes().equals(List.of(1L, 2L))
            && actualizacion.estado().tiradas().isEmpty()));
        verify(gestorTemporizadorMock, times(2)).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"), org.mockito.ArgumentMatchers.eq(15), any(Runnable.class));
    }

    @Test
    public void dadoQueSeResuelveElEventoCuandoHayUnUnicoGanadorYDaNotificacionFinalizada() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(5, 4));
        servicioEvento.iniciarEventoGlobal(partida, evento);

        servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento);

        verify(publicadorEventosMock).publishEvent(argThat((Object publicado) ->
            publicado instanceof EventoGlobalActualizado actualizacion
            && "FINALIZADO".equals(actualizacion.estado().fase())
            && actualizacion.estado().segundosRestantes().equals(0)
            && actualizacion.estado().idJugadorElegido().equals(1L)
            && actualizacion.estado().tiradas().get(1L).equals(9)));
        verify(publicadorEventosMock).publishEvent(new EventoGlobalFinalizado("ABC"));
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
    }

    @Test
    public void dadoQueElKevinSeActivaCuandoSeAplicaInmediatamenteYDaNotificacionFinalizada() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearKevin();

        servicioEvento.iniciarEventoGlobal(partida, evento);

        assertEquals(900, uno.getDinero());
        assertEquals(false, servicioEvento.verificarSiHayEventoGlobalActivo("ABC"));
        verify(publicadorEventosMock).publishEvent(argThat((Object publicado) ->
            publicado instanceof EventoGlobalActualizado actualizacion
            && "FINALIZADO".equals(actualizacion.estado().fase())
            && actualizacion.estado().etapa().equals(0)
            && actualizacion.estado().segundosRestantes().equals(0)
            && actualizacion.estado().idJugadorElegido().equals(1L)
            && actualizacion.estado().tiradas().isEmpty()));
        verify(gestorTemporizadorMock, never()).programarTemporizador(
            org.mockito.ArgumentMatchers.eq("evento-global:ABC"), anyInt(), any(Runnable.class));
    }

    @Test
    public void dadoQueHayUnEventoEnOtraPartidaCuandoSeConsultaUnCodigoDistintoYDaNull() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        EstadoEventoGlobal estado = servicioEvento.consultarEstadoEventoGlobal("XYZ");

        assertNull(estado);
        verify(repositorioPartidaMock, never()).buscarPartidaActivaPorCodigoUnico("XYZ");
    }

    @Test
    public void dadoQueLaTiradaManualCompletaElEventoCuandoSeEjecutaDesdeRestYDaFinalizacionYReanudacion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico("ABC")).thenReturn(partida);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(6, 3));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        ResultadoTirada resultado = servicioEvento.tirarDadosEventoGlobal("ABC", 1L);

        assertEquals(9, resultado.sumarDados());
        assertEquals(7, uno.getPosicionActual());
        assertNull(servicioEvento.consultarEstadoEventoGlobal("ABC"));
        verify(gestorTemporizadorMock).cancelarTemporizador("evento-global:ABC");
        verify(publicadorEventosMock).publishEvent(new EventoGlobalFinalizado("ABC"));
    }

}
