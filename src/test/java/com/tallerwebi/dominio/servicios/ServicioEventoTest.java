package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.excepcion.DatosEnventoInvalidoExceptions;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaDadoInvalidaException;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        publicadorEventosMock = (ApplicationEventPublisher) mock(PublicadorEventos.class);
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
    public void dadoQueElRepositorioTieneUnEventoCuandoSeObtieneUnoAleatorioDaComoResultadoElEvento() {
        Evento evento = crearEvento(TipoEfecto.GANAR_PLATA, 300);
        when(repositorioEventoMock.obtenerEventoAleatorio()).thenReturn(evento);

        Evento resultado = servicioEvento.obtenerEventoAleatorio();

        assertEquals(evento, resultado);
        verify(repositorioEventoMock).obtenerEventoAleatorio();
    }

    @Test
    public void dadoQueElRepositorioNoTieneEventosCuandoSeBuscaUnoAleatorioDaComoResultadoNull() {
        when(repositorioEventoMock.obtenerEventoAleatorio()).thenReturn(null);

        Evento resultado = servicioEvento.obtenerEventoAleatorio();

        assertNull(resultado);
    }

    @Test
    public void dadoQueElRepositorioTieneUnEventoGlobalCuandoSeObtieneUnoAleatorioDaComoResultadoElEventoGlobal() {
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
    public void dadoQueElEventoEsNullCuandoSeIntentaAplicarDaComoResultadoJugadorSinCambios() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 10);

        servicioEvento.aplicarEvento(jugador, null);

        assertEquals(1500, jugador.getDinero());
        assertEquals(10, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueElEventoNoTieneTipoEfectoCuandoSeAplicaDaComoResultadoJugadorSinCambios() throws EventoInexistenteException {
        Jugador jugador = crearJugador(1L, 1500, 10);
        Evento evento = new Evento();
        evento.setValor(200);

        servicioEvento.aplicarEvento(jugador, evento);

        assertEquals(1500, jugador.getDinero());
        assertEquals(10, jugador.getPosicionActual());
    }

    //Inicializar los eventos globales y validar sus datos
    @Test
    public void dadoQueElEventoGlobalEsNullCuandoSeIniciaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, null));
    }

    @Test
    public void dadoQueElEventoGlobalNoTieneTipoEfectoCuandoSeIniciaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        EventoGlobal evento = crearInundacion();
        evento.setTipoEfecto(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, evento));
    }

    @Test
    public void dadoQueElEventoGlobalNoTieneTipoGlobalCuandoSeIniciaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        EventoGlobal evento = crearInundacion();
        evento.setTipoEventoGlobal(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, evento));
    }

    @Test
    public void dadoQueLaPartidaEsNullCuandoSeIniciaUnEventoGlobalDaComoResultadoExcepcion() {
        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.iniciarEventoGlobal(null, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeIniciaUnEventoGlobalDaComoResultadoExcepcion() {
        Partida partida = crearPartida(null, 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneListaDeJugadoresCuandoSeIniciaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5);
        partida.setJugadores(null);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaEstaSinJugadoresCuandoSeIniciaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearInundacion()));
    }

    @Test
    public void dadoQueYaHayInundacionActivaCuandoSeIniciaOtroEventoDaComoResultadoExcepcion() throws Exception {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(EventoGlobalActivoException.class,
            () -> servicioEvento.iniciarEventoGlobal(partida, crearPiquete()));
    }

    @Test
    public void dadoQueSeIniciaKevinConUnJugadorCuandoSeAplicaDaComoResultadoMenosDinero() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);

        servicioEvento.iniciarEventoGlobal(partida, crearKevin());

        assertEquals(900, jugador.getDinero());
    }

    @Test
    public void dadoQueSeIniciaFiatSeiscientosCuandoSeAplicaDaComoResultadoMasDinero() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);
        EventoGlobal fiat = crearEventoGlobal(
            5L, TipoEventoGlobal.FIAT_600, TipoEfecto.GANAR_PLATA, 400);

        servicioEvento.iniciarEventoGlobal(partida, fiat);

        assertEquals(1400, jugador.getDinero());
    }

    @Test
    public void dadoQueSeIniciaAccidenteSarmientoCuandoSeAplicaDaComoResultadoPerderTurno() throws Exception {
        Jugador jugador = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, jugador);
        EventoGlobal accidente = crearEventoGlobal(
            6L, TipoEventoGlobal.ACCIDENTE_SARMIENTO, TipoEfecto.PERDER_TURNO, 1);

        servicioEvento.iniciarEventoGlobal(partida, accidente);

        assertTrue(jugador.getPierdeTurno());
    }

    @Test
    public void dadoQueKevinTieneDosJugadoresCuandoSeActivaDaComoResultadoUnSoloAfectado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno, dos);

        servicioEvento.iniciarEventoGlobal(partida, crearKevin());

        assertEquals(1900, uno.getDinero() + dos.getDinero());
    }

    //Tests de resolución, tiradas y desempates
    @Test
    public void dadoQueFaltaUnJugadorPorTirarCuandoTiraElPrimeroDaComoResultadoEventoPendiente() throws Exception {
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
    public void dadoQueTodosTiraronInundacionCuandoHayUnMayorDaComoResultadoRetrocederTres() throws Exception {
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
    public void dadoQueTodosTiraronPiqueteCuandoHayUnMenorDaComoResultadoVolverAlInicio() throws Exception {
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
    public void dadoQueTodosTiraronAfipCuandoHayUnMayorDaComoResultadoAlcanzarAlMasAdelantado() throws Exception {
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
    public void dadoQueDosJugadoresEmpatanCuandoVuelvenATirarDaComoResultadoUnGanador() throws Exception {
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
    public void dadoQueSeRepiteElEmpateCuandoTiranOtraVezDaComoResultadoResolucionFinal() throws Exception {
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
    public void dadoQueDosDeTresEmpatanCuandoElTerceroIntentaDesempatarDaComoResultadoExcepcion() throws Exception {
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
    public void dadoQueUnJugadorYaTiroCuandoIntentaRepetirAntesDelOtroDaComoResultadoExcepcion() throws Exception {
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
    public void dadoQueLaSumaEsMenorADosCuandoSeRegistraDaComoResultadoExcepcion() throws Exception {
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
    public void dadoQueLaSumaEsMayorADoceCuandoSeRegistraDaComoResultadoExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);
        when(servicioDadoMock.tirarDados()).thenReturn(new ResultadoTirada(7, 7));

        assertThrows(TiradaDadoInvalidaException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueLaSumaDeDadosEsNullCuandoSeRegistraDaComoResultadoExcepcion() throws Exception {
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
    public void dadoQueLaPartidaEsNullCuandoUnJugadorTiraDaComoResultadoDatosInvalidos() {
        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(null, crearJugador(1L, 1000, 10), crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorEsNullCuandoIntentaTirarDaComoResultadoDatosInvalidos() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 10));

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, null, crearInundacion()));
    }

    @Test
    public void dadoQueElEventoEsNullCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, null));
    }

    @Test
    public void dadoQueElEventoNoTieneTipoEfectoCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setTipoEfecto(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueElEventoNoTieneTipoGlobalCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setTipoEventoGlobal(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida(null, 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueLaPartidaNoTieneJugadoresCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        partida.setJugadores(null);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorNoTieneIdCuandoSeIntentaTirarDaComoResultadoDatosInvalidos() {
        Jugador uno = crearJugador(null, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoIntentaTirarDaComoResultadoExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Jugador ajeno = crearJugador(2L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        servicioEvento.iniciarEventoGlobal(partida, evento);

        assertThrows(JugadorNoEncontradoException.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, ajeno, evento));
    }

    @Test
    public void dadoQueNoExisteEventoGlobalActivoCuandoUnJugadorTiraDaComoResultadoExcepcion() {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearInundacion()));
    }

    @Test
    public void dadoQueElEventoActivoTieneOtroIdCuandoUnJugadorTiraDaComoResultadoExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        servicioEvento.iniciarEventoGlobal(partida, crearInundacion());

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, crearPiquete()));
    }

    @Test
    public void dadoQueElEventoActivoNoTieneIdCuandoSeIntentaTirarDaComoResultadoExcepcion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 10);
        Partida partida = crearPartida("ABC", 5, uno);
        EventoGlobal evento = crearInundacion();
        evento.setId(null);
        servicioEvento.iniciarEventoGlobal(partida, evento);

        assertThrows(DatosEnventoInvalidoExceptions.class,
            () -> servicioEvento.realizarTiradaEventoGlobal(partida, uno, evento));
    }

    @Test
    public void dadoQueSeResuelveLaInundacionCuandoSeIntentaTirarOtraVezDaComoResultadoEventoInactivo() throws Exception {
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
    public void dadoQueFinalizaUnEventoCuandoSeIniciaOtroDaComoResultadoNuevoEventoPermitido() throws Exception {
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
    public void dadoQueExistenDosPartidasCuandoSeResuelveUnaDaComoResultadoLaOtraPendiente() throws Exception {
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
    public void dadoQueLaPartidaEsNullCuandoSeVerificaActivacionDaComoResultadoExcepcion() {
        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(null));
    }

    @Test
    public void dadoQueLaPartidaNoTieneCodigoCuandoSeVerificaActivacionDaComoResultadoExcepcion() {
        Partida partida = crearPartida(null, 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueLaPartidaTieneCodigoVacioCuandoSeVerificaActivacionDaComoResultadoExcepcion() {
        Partida partida = crearPartida("  ", 5, crearJugador(1L, 1000, 5));

        assertThrows(PartidaNoEncontradaException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueLaRondaEsNullCuandoSeVerificaActivacionDaComoResultadoNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", null, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCeroCuandoSeVerificaActivacionDaComoResultadoNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", 0, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCuatroCuandoSeVerificaActivacionDaComoResultadoNoActivarEvento() throws Exception {
        Partida partida = crearPartida("ABC", 4, crearJugador(1L, 1000, 5));

        servicioEvento.verificarActivacionEventoGlobal(partida);

        verify(repositorioEventoMock, never()).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueLaRondaEsCincoCuandoSeVerificaActivacionDaComoResultadoAplicarEvento() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(partida);

        assertEquals(900, uno.getDinero());
        verify(repositorioEventoMock, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueYaSeActivoEnLaRondaCincoCuandoSeVerificaOtraVezDaComoResultadoUnaSolaActivacion() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Partida partida = crearPartida("ABC", 5, uno);
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(crearKevin());

        servicioEvento.verificarActivacionEventoGlobal(partida);
        servicioEvento.verificarActivacionEventoGlobal(partida);

        assertEquals(900, uno.getDinero());
        verify(repositorioEventoMock, times(1)).obtenerEventoGlobalAleatorio();
    }

    @Test
    public void dadoQueSeActivoEnLaRondaCincoCuandoSeLlegaADiezDaComoResultadoNuevoEvento() throws Exception {
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
    public void dadoQueNoHayEventoGlobalEnRepositorioCuandoSeActivaLaRondaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio()).thenReturn(null);

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueElRepositorioDevuelveEventoComunCuandoSeActivaLaRondaDaComoResultadoExcepcion() {
        Partida partida = crearPartida("ABC", 5, crearJugador(1L, 1000, 5));
        when(repositorioEventoMock.obtenerEventoGlobalAleatorio())
            .thenReturn(crearEvento(TipoEfecto.GANAR_PLATA, 300));

        assertThrows(EventoGlobalInvalidoException.class,
            () -> servicioEvento.verificarActivacionEventoGlobal(partida));
    }

    @Test
    public void dadoQueQuedaUnEventoPendienteCuandoLlegaOtraRondaDeActivacionDaComoResultadoExcepcion() throws Exception {
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
    public void dadoQueHayDosPartidasEnRondaCincoCuandoSeVerificaActivacionDaComoResultadoDosEventos() throws Exception {
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
}
