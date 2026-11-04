package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ServicioEventoImplTest {
    @TestFactory
    Stream<DynamicTest> dadoQueHayDosTresOCuatroJugadoresCuandoOcurreCadaEventoDaComoResultadoSuConsecuencia() {
        return IntStream.rangeClosed(2, 4).boxed().flatMap(cantidad -> Arrays.stream(TipoEventoGlobal.values())
                .map(tipo -> DynamicTest.dynamicTest(tipo + " con " + cantidad + " jugadores", () -> {
                    Escenario caso = new Escenario(cantidad, tipo, 0);
                    caso.iniciar();
                    caso.tirarTodos();
                    Jugador elegido = caso.jugador(0);
                    assertEquals(elegido.getId(), caso.partida.getEventoGlobal().getSeleccionado());
                    switch (tipo) {
                        case TRAMITES_AFIP -> assertEquals(caso.jugador(cantidad - 1).getPosicionActual(), elegido.getPosicionActual());
                        case INUNDACION -> assertEquals(5, elegido.getPosicionActual());
                        case PIQUETE -> assertEquals(0, elegido.getPosicionActual());
                        case ROBO -> assertEquals(1500, elegido.getDinero());
                        case FIAT_600 -> assertEquals(2400, elegido.getDinero());
                        case ACCIDENTE_SARMIENTO -> assertTrue(elegido.getPierdeTurno());
                    }
                    int saldo = elegido.getDinero();
                    int posicion = elegido.getPosicionActual();
                    caso.confirmarTodos();
                    caso.eventos.confirmar("GLOBAL", caso.jugador(0).getId(), 5);
                    assertFalse(caso.partida.getEventoGlobal().estaActivo());
                    assertEquals(saldo, elegido.getDinero());
                    assertEquals(posicion, elegido.getPosicionActual());
                    verify(caso.temporizador, times(1)).programarTemporizador(eq("GLOBAL"), anyInt(), any());
                })));
    }

    @TestFactory
    Stream<DynamicTest> dadoQueSarmientoPenalizaDistintosOrdenesCuandoSeReanudaDaComoResultadoUnaSolaOportunidadOmitida() {
        return IntStream.rangeClosed(2, 4).boxed().flatMap(cantidad -> IntStream.range(0, cantidad).mapToObj(afectado ->
                DynamicTest.dynamicTest(cantidad + " jugadores, penalizado " + afectado, () -> {
                    Escenario caso = new Escenario(cantidad, TipoEventoGlobal.ACCIDENTE_SARMIENTO, afectado);
                    caso.iniciar();
                    caso.confirmarTodos();
                    List<Integer> esperados = IntStream.range(0, cantidad * 2 + 1)
                            .filter(indice -> indice != afectado).map(indice -> indice % cantidad).boxed().toList();
                    for (int indice = 0; indice < cantidad * 2; indice++) {
                        assertEquals(caso.jugador(esperados.get(indice)).getId(), caso.partidas.obtenerJugadorActual("GLOBAL").getId());
                        assertFalse(caso.partidas.obtenerJugadorActual("GLOBAL").getPierdeTurno());
                        caso.partidas.avanzarTurno("GLOBAL");
                    }
                    assertFalse(caso.jugador(afectado).getPierdeTurno());
                    assertTrue(caso.partida.getRondaActual() >= 7);
                })));
    }

    @Test
    void dadoQueHayEmpatesConsecutivosCuandoSeRegistranTodasLasTiradasDaComoResultadoSoloLosEmpatadosHabilitados() {
        Escenario caso = new Escenario(4, TipoEventoGlobal.INUNDACION, 0);
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(6, 6),
                new ResultadoTirada(1, 1), new ResultadoTirada(2, 2), new ResultadoTirada(5, 5),
                new ResultadoTirada(5, 5), new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(Set.of(1L, 2L), caso.partida.getEventoGlobal().getHabilitados());
        assertEquals(2, caso.partida.getEventoGlobal().getEtapa());
        assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 3L, 5, 2));
        caso.eventos.tirar("GLOBAL", 1L, 5, 2);
        assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 1L, 5, 2));
        caso.eventos.tirar("GLOBAL", 2L, 5, 2);
        assertEquals(3, caso.partida.getEventoGlobal().getEtapa());
        caso.eventos.tirar("GLOBAL", 1L, 5, 3);
        assertFalse(caso.partida.getEventoGlobal().estaResuelto());
        caso.eventos.tirar("GLOBAL", 2L, 5, 3);
        assertEquals(5, caso.jugador(0).getPosicionActual());
        assertEquals(8, caso.partida.getEventoGlobal().getTiradas().size());
    }

    @TestFactory
    Stream<DynamicTest> dadoQueTodosEmpatanCuandoVuelvenATirarDaComoResultadoUnUnicoSeleccionado() {
        return IntStream.rangeClosed(2, 4).mapToObj(cantidad -> DynamicTest.dynamicTest("Empatan " + cantidad, () -> {
            Escenario caso = new Escenario(cantidad, TipoEventoGlobal.TRAMITES_AFIP, 0);
            when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(3, 3));
            caso.iniciar();
            caso.tirarTodos();
            assertEquals(cantidad, caso.partida.getEventoGlobal().getHabilitados().size());
            when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
            caso.tirarTodos();
            assertTrue(caso.partida.getEventoGlobal().estaResuelto());
        }));
    }

    @Test
    void dadoQueUnJugadorNoTieneDineroCuandoSufreUnRoboDaComoResultadoLaDeudaExistenteSinSaldoNegativo() {
        Escenario caso = new Escenario(3, TipoEventoGlobal.ROBO, 0);
        caso.jugador(0).setDinero(100);
        DeudaPendiente deuda = new DeudaPendiente();
        doAnswer(invocacion -> {
            when(caso.quiebras.obtenerDeudaPendiente("GLOBAL", 1L)).thenReturn(deuda);
            return null;
        }).when(caso.quiebras).registrarDeuda(eq("GLOBAL"), eq(caso.jugador(0)), isNull(), eq(500), eq("ROBO_EVENTO_GLOBAL"));
        caso.iniciar();
        caso.confirmarTodos();
        assertTrue(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(100, caso.jugador(0).getDinero());
        verify(caso.temporizador, never()).programarTemporizador(eq("GLOBAL"), anyInt(), any());
        when(caso.quiebras.obtenerDeudaPendiente("GLOBAL", 1L)).thenReturn(null);
        caso.eventos.consultar("GLOBAL", 1L);
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        verify(caso.quiebras, times(1)).registrarDeuda(anyString(), any(), isNull(), eq(500), anyString());
    }

    @Test
    void dadoQueUnJugadorAbandonaDuranteElDesempateCuandoSeActualizanParticipantesDaComoResultadoSuExclusion() {
        Escenario caso = new Escenario(4, TipoEventoGlobal.INUNDACION, 0);
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6));
        caso.iniciar();
        caso.tirarTodos();
        caso.jugador(3).setAbandono(true);
        caso.jugador(2).setEnBancarrota(true);
        caso.eventos.participantesCambiaron(caso.partida);
        assertEquals(Set.of(1L, 2L), caso.partida.getEventoGlobal().getHabilitados());
        assertThrows(SecurityException.class, () -> caso.eventos.tirar("GLOBAL", 4L, 5, 2));
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        caso.eventos.tirar("GLOBAL", 1L, 5, 2);
        caso.eventos.tirar("GLOBAL", 2L, 5, 2);
        caso.confirmarTodos();
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
    }

    @Test
    void dadoQueSeActivaUnEventoCuandoLleganConsultasYUnTemporizadorAnteriorDaComoResultadoUnaUnicaActivacion() {
        Escenario caso = new Escenario(3, TipoEventoGlobal.FIAT_600, 0);
        caso.partidas.iniciarTemporizadorTurnoSiNoExiste("GLOBAL");
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorSiNoExiste(eq("GLOBAL"), anyInt(), vencimiento.capture());
        caso.iniciar();
        vencimiento.getValue().run();
        caso.partidas.avanzarTurno("GLOBAL");
        caso.eventos.activarSiCorresponde(caso.partida);
        caso.eventos.consultar("GLOBAL", 1L);
        assertEquals(2400, caso.jugador(0).getDinero());
        assertEquals(5, caso.partida.getRondaActual());
        assertEquals(0, caso.partida.getIndiceTurnoActual());
        verify(caso.catalogo, times(1)).obtenerTodos();
        assertThrows(IllegalStateException.class, () -> caso.participacion.validarAccionOrdinaria(caso.partida));
        caso.confirmarTodos();
        assertFalse(caso.eventos.activarSiCorresponde(caso.partida));
    }

    @Test
    void dadoQueUnaInundacionCruzaInicioCuandoSeResuelveDaComoResultadoRetrocesoCircularSinPremio() {
        Escenario caso = new Escenario(2, TipoEventoGlobal.INUNDACION, 0);
        caso.jugador(0).setPosicionActual(1);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(28, caso.jugador(0).getPosicionActual());
        assertEquals(2000, caso.jugador(0).getDinero());
    }

    @Test
    void dadoQueVenceElTiempoCuandoFaltanTiradasDaComoResultadoDadosAutomaticosSoloParaActivos() {
        Escenario caso = new Escenario(4, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();
        caso.eventos.tirar("GLOBAL", 1L, 5, 1);
        caso.jugador(3).setAbandono(true);
        ReflectionTestUtils.setField(caso.partida.getEventoGlobal(), "vencimientoEvento", java.time.Instant.now().minusSeconds(1));
        caso.eventos.consultar("GLOBAL", 1L);
        ArgumentCaptor<Runnable> vencimientos = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador, atLeastOnce()).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimientos.capture());
        List<Runnable> acciones = vencimientos.getAllValues();
        acciones.get(acciones.size() - 1).run();
        assertTrue(caso.partida.getEventoGlobal().estaResuelto());
        assertEquals(3, caso.partida.getEventoGlobal().getTiradas().size());
        verify(caso.dados, times(3)).tirarDados();
        ReflectionTestUtils.setField(caso.partida.getEventoGlobal(), "vencimientoEvento", java.time.Instant.now().minusSeconds(1));
        caso.eventos.consultar("GLOBAL", 1L);
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(5, caso.jugador(0).getPosicionActual());
    }

    @Test
    void dadoQuePiqueteAfectaAAlguienEnInicioCuandoSeResuelveDaComoResultadoUnaExplicacionSinDesplazamiento() {
        Escenario caso = new Escenario(2, TipoEventoGlobal.PIQUETE, 0);
        caso.jugador(0).setPosicionActual(0);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(0, caso.jugador(0).getPosicionActual());
        assertTrue(caso.partida.getEventoGlobal().getConsecuencia().contains("ya está en Inicio"));
    }

    @Test
    void dadoQueAfipSeleccionaAlPrimeroCuandoSeResuelveDaComoResultadoUnaExplicacionSinMovimiento() {
        Escenario caso = new Escenario(2, TipoEventoGlobal.TRAMITES_AFIP, 0);
        caso.jugador(0).setPosicionActual(25);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(25, caso.jugador(0).getPosicionActual());
        assertTrue(caso.partida.getEventoGlobal().getConsecuencia().contains("no necesita avanzar"));
    }

    @Test
    void dadoQueHayDosPartidasCuandoUnaFinalizaDaComoResultadoEstadosAislados() {
        Escenario primera = new Escenario(3, TipoEventoGlobal.FIAT_600, 0);
        Escenario segunda = new Escenario(3, TipoEventoGlobal.PIQUETE, 0);
        primera.iniciar();
        segunda.iniciar();
        primera.partida.setEstado(EstadoPartida.FINALIZADA);
        primera.eventos.participantesCambiaron(primera.partida);
        assertFalse(primera.partida.getEventoGlobal().estaActivo());
        assertTrue(segunda.partida.getEventoGlobal().estaActivo());
        assertFalse(primera.eventos.activarSiCorresponde(primera.partida));
    }

    private static class Escenario {
        final Partida partida = new Partida();
        final ServicioParticipacion participacion = new ServicioParticipacionImpl();
        final RepositorioPartida repositorio = mock(RepositorioPartida.class);
        final RepositorioEventoGlobal catalogo = mock(RepositorioEventoGlobal.class);
        final ServicioDado dados = mock(ServicioDado.class);
        final ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        final GestorTemporizadorPartida temporizador = mock(GestorTemporizadorPartida.class);
        final ServicioPartidaImpl partidas;
        final ServicioEventoImpl eventos;

        Escenario(int cantidad, TipoEventoGlobal tipo, int seleccionado) {
            partida.setCodigoUnico("GLOBAL");
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(4);
            partida.setIndiceTurnoActual(cantidad - 1);
            for (int indice = 0; indice < cantidad; indice++) {
                Jugador jugador = new Jugador();
                jugador.setId((long) indice + 1);
                jugador.setOrden(indice);
                jugador.setApodo("Jugador " + indice);
                jugador.setDinero(2000);
                jugador.setPosicionActual(8 + indice * 5);
                jugador.setUltimaRondaConOportunidad(indice == cantidad - 1 ? 3 : 4);
                partida.getJugadores().add(jugador);
            }
            when(repositorio.buscarPartidaActivaPorCodigoUnico("GLOBAL")).thenReturn(partida);
            when(repositorio.buscarPartidaPorCodigoUnico("GLOBAL")).thenReturn(partida);
            when(repositorio.buscarPartidaPorJugador(anyLong())).thenReturn(partida);
            var transacciones = mock(PlatformTransactionManager.class);
            when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            PublicadorEventos<EventoTurnoCambiado> turnos = new PublicadorEventos<>(List.of());
            PublicadorEventos<EventoJugadorAbandonado> abandonos = new PublicadorEventos<>(List.of());
            partidas = new ServicioPartidaImpl(repositorio, mock(ServicioInicioPartida.class), temporizador,
                    new ServicioTurnoImpl(participacion), turnos, abandonos, quiebras, null,
                    mock(ServicioFinalizacionPartida.class), transacciones);
            ReflectionTestUtils.setField(partidas, "participacion", participacion);
            var economia = new ServicioEconomiaImpl();
            var aplicador = new AplicadorConsecuenciaEventoGlobal(economia,
                    new ServicioMovimientoImpl(economia, repositorio, participacion), quiebras, participacion);
            eventos = new ServicioEventoImpl(catalogo, repositorio, participacion, dados, aplicador,
                    partidas, temporizador, mock(NotificadorPartida.class));
            ReflectionTestUtils.setField(partidas, "eventosGlobales", eventos);
            RandomGenerator generador = mock(RandomGenerator.class);
            when(generador.nextInt(6)).thenReturn(tipo.ordinal());
            when(generador.nextInt(cantidad)).thenReturn(seleccionado);
            eventos.usarAleatorio(generador);
            when(catalogo.obtenerTodos()).thenReturn(Arrays.stream(TipoEventoGlobal.values()).map(valor -> {
                EventoGlobal evento = new EventoGlobal();
                evento.setId(valor.ordinal() + 1);
                evento.setNombre(valor.name());
                evento.setDescripcion(valor.name());
                evento.setTipoEventoGlobal(valor);
                evento.setValor(new int[]{0, -3, 0, 500, 400, 1}[valor.ordinal()]);
                return evento;
            }).toList());
            when(dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
            if (tipo == TipoEventoGlobal.PIQUETE) {
                when(dados.tirarDados()).thenReturn(new ResultadoTirada(1, 1), new ResultadoTirada(6, 6));
            }
        }

        Jugador jugador(int indice) { return partida.getJugadores().get(indice); }
        void iniciar() { partidas.avanzarTurno("GLOBAL"); }
        void tirarTodos() {
            if (partida.getEventoGlobal().estaResuelto()) return;
            int etapa = partida.getEventoGlobal().getEtapa();
            for (Jugador jugador : partida.getJugadores()) {
                if (partida.getEventoGlobal().getHabilitados().contains(jugador.getId())) {
                    eventos.tirar("GLOBAL", jugador.getId(), 5, etapa);
                }
            }
        }
        void confirmarTodos() {
            partida.getJugadores().stream().filter(participacion::estaActivo)
                    .forEach(jugador -> eventos.confirmar("GLOBAL", jugador.getId(), 5));
        }
    }
}
