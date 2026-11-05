package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
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
                    EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(cantidad, tipo, 0);
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
                    EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(cantidad, TipoEventoGlobal.ACCIDENTE_SARMIENTO, afectado);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(4, TipoEventoGlobal.INUNDACION, 0);
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
            EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(cantidad, TipoEventoGlobal.TRAMITES_AFIP, 0);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.ROBO, 0);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(4, TipoEventoGlobal.INUNDACION, 0);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(2, TipoEventoGlobal.INUNDACION, 0);
        caso.jugador(0).setPosicionActual(1);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(28, caso.jugador(0).getPosicionActual());
        assertEquals(2000, caso.jugador(0).getDinero());
    }

    @Test
    void dadoQueVenceElTiempoCuandoFaltanTiradasDaComoResultadoDadosAutomaticosSoloParaActivos() {
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(4, TipoEventoGlobal.INUNDACION, 0);
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
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(2, TipoEventoGlobal.PIQUETE, 0);
        caso.jugador(0).setPosicionActual(0);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(0, caso.jugador(0).getPosicionActual());
        assertTrue(caso.partida.getEventoGlobal().getConsecuencia().contains("ya está en Inicio"));
    }

    @Test
    void dadoQueAfipSeleccionaAlPrimeroCuandoSeResuelveDaComoResultadoUnaExplicacionSinMovimiento() {
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(2, TipoEventoGlobal.TRAMITES_AFIP, 0);
        caso.jugador(0).setPosicionActual(25);
        caso.iniciar();
        caso.tirarTodos();
        assertEquals(25, caso.jugador(0).getPosicionActual());
        assertTrue(caso.partida.getEventoGlobal().getConsecuencia().contains("no necesita avanzar"));
    }

    @Test
    void dadoQueHayDosPartidasCuandoUnaFinalizaDaComoResultadoEstadosAislados() {
        EscenarioPruebaEventoGlobal primera = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
        EscenarioPruebaEventoGlobal segunda = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.PIQUETE, 0);
        primera.iniciar();
        segunda.iniciar();
        primera.partida.setEstado(EstadoPartida.FINALIZADA);
        primera.eventos.participantesCambiaron(primera.partida);
        assertFalse(primera.partida.getEventoGlobal().estaActivo());
        assertTrue(segunda.partida.getEventoGlobal().estaActivo());
        assertFalse(primera.eventos.activarSiCorresponde(primera.partida));
    }

    @TestFactory
    Stream<DynamicTest> dadoQueLaRondaNoEsMultiploDeCincoCuandoSeIntentaActivarDaComoResultadoNingunEvento() {
        return Stream.of(4, 6, 9, 11).map(ronda -> DynamicTest.dynamicTest("Ronda " + ronda, () -> {
            // Preparación
            EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
            caso.partida.setRondaActual(ronda);

            // Ejecución
            Boolean activado = caso.eventos.activarSiCorresponde(caso.partida);

            // Verificación
            assertFalse(activado);
            assertFalse(caso.partida.getEventoGlobal().estaActivo());
            verify(caso.catalogo, never()).obtenerTodos();
        }));
    }

    @TestFactory
    Stream<DynamicTest> dadoQueSeCumpleOtroMultiploDeCincoCuandoSeIntentaActivarDaComoResultadoUnEventoNuevo() {
        return Stream.of(10, 15).map(ronda -> DynamicTest.dynamicTest("Ronda " + ronda, () -> {
            // Preparación
            EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
            caso.partida.setRondaActual(ronda);

            // Ejecución
            Boolean activado = caso.eventos.activarSiCorresponde(caso.partida);

            // Verificación
            assertTrue(activado);
            assertEquals(ronda, caso.partida.getEventoGlobal().getUltimaRonda());
            assertEquals(2400, caso.jugador(0).getDinero());
        }));
    }

    @Test
    void dadoQueElCatalogoEstaIncompletoCuandoSeIntentaActivarDaComoResultadoUnErrorSinAplicarConsecuencias() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
        caso.partida.setRondaActual(5);
        when(caso.catalogo.obtenerTodos()).thenReturn(List.of());

        // Ejecución
        assertThrows(IllegalStateException.class, () -> caso.eventos.activarSiCorresponde(caso.partida));

        // Verificación
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(2000, caso.jugador(0).getDinero());
        verifyNoInteractions(caso.temporizador);
    }

    @Test
    void dadoQueHayUnaDeudaPendienteCuandoSeIntentaActivarDaComoResultadoLaActivacionBloqueada() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.ROBO, 0);
        caso.partida.setRondaActual(5);
        when(caso.quiebras.obtenerDeudaPendiente("GLOBAL", 1L)).thenReturn(new DeudaPendiente());

        // Ejecución
        Boolean activado = caso.eventos.activarSiCorresponde(caso.partida);

        // Verificación
        assertFalse(activado);
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(2000, caso.jugador(0).getDinero());
        verifyNoInteractions(caso.temporizador);
    }

    @TestFactory
    Stream<DynamicTest> dadoQueFaltanJugadoresActivosCuandoSeIntentaActivarDaComoResultadoNingunEvento() {
        return Stream.of(0, 1).map(cantidad -> DynamicTest.dynamicTest("Activos " + cantidad, () -> {
            // Preparación
            EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(2, TipoEventoGlobal.ROBO, 0);
            caso.partida.setRondaActual(5);
            caso.jugador(1).setAbandono(true);
            caso.jugador(0).setEnBancarrota(cantidad == 0);

            // Ejecución
            Boolean activado = caso.eventos.activarSiCorresponde(caso.partida);

            // Verificación
            assertFalse(activado);
            assertFalse(caso.partida.getEventoGlobal().estaActivo());
            verifyNoInteractions(caso.temporizador);
        }));
    }

    @TestFactory
    Stream<DynamicTest> dadoQueElEventoEsAutomaticoCuandoUnJugadorIntentaTirarDaComoResultadoElRechazoSinDados() {
        return Stream.of(TipoEventoGlobal.ROBO, TipoEventoGlobal.FIAT_600, TipoEventoGlobal.ACCIDENTE_SARMIENTO)
                .map(tipo -> DynamicTest.dynamicTest(tipo.name(), () -> {
                    // Preparación
                    EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, tipo, 0);
                    caso.iniciar();
                    Integer saldo = caso.jugador(0).getDinero();

                    // Ejecución
                    assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 1L, 5, 1));

                    // Verificación
                    verifyNoInteractions(caso.dados);
                    assertEquals(saldo, caso.jugador(0).getDinero());
                    assertEquals(2000, caso.jugador(1).getDinero());
                    assertEquals(2000, caso.jugador(2).getDinero());
                    assertTrue(caso.partida.getEventoGlobal().getTiradas().isEmpty());
                }));
    }

    @Test
    void dadoQueLaSolicitudTieneRondaOEtapaAnteriorCuandoSeIntentaTirarDaComoResultadoElRechazoSinRegistrarDados() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();

        // Ejecución
        assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 1L, 4, 1));
        assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 1L, 5, 0));

        // Verificación
        verifyNoInteractions(caso.dados);
        assertTrue(caso.partida.getEventoGlobal().getTiradas().isEmpty());
        assertFalse(caso.partida.getEventoGlobal().estaResuelto());
    }

    @Test
    void dadoQueElPlazoEstaVencidoCuandoSeIntentaTirarDaComoResultadoElRechazoSinConsumirDados() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();
        ReflectionTestUtils.setField(caso.partida.getEventoGlobal(), "vencimientoEvento", java.time.Instant.EPOCH);

        // Ejecución
        assertThrows(IllegalStateException.class, () -> caso.eventos.tirar("GLOBAL", 1L, 5, 1));

        // Verificación
        verifyNoInteractions(caso.dados);
        assertTrue(caso.partida.getEventoGlobal().getTiradas().isEmpty());
    }

    @Test
    void dadoQueElEventoEsperaTiradasCuandoSeIntentaConfirmarDaComoResultadoElRechazoSinConfirmaciones() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.TRAMITES_AFIP, 0);
        caso.iniciar();

        // Ejecución
        assertThrows(IllegalStateException.class, () -> caso.eventos.confirmar("GLOBAL", 1L, 5));

        // Verificación
        assertTrue(caso.partida.getEventoGlobal().getConfirmados().isEmpty());
        assertTrue(caso.partida.getEventoGlobal().estaActivo());
        verify(caso.temporizador, never()).programarTemporizador(eq("GLOBAL"), anyInt(), any());
    }

    @Test
    void dadoQueUnJugadorYaConfirmoCuandoRepiteOEnviaUnaRondaAnteriorDaComoResultadoUnaSolaConfirmacion() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
        caso.iniciar();
        caso.eventos.confirmar("GLOBAL", 1L, 5);
        Long revision = caso.partida.getEventoGlobal().getRevision();

        // Ejecución
        caso.eventos.confirmar("GLOBAL", 1L, 5);
        caso.eventos.confirmar("GLOBAL", 2L, 4);

        // Verificación
        assertEquals(Set.of(1L), caso.partida.getEventoGlobal().getConfirmados());
        assertEquals(revision, caso.partida.getEventoGlobal().getRevision());
        assertEquals(2400, caso.jugador(0).getDinero());
        assertTrue(caso.partida.getEventoGlobal().estaActivo());
        verify(caso.temporizador, never()).programarTemporizador(eq("GLOBAL"), anyInt(), any());
    }

    @Test
    void dadoQueSoloQuedaUnEmpatadoActivoCuandoSeActualizanParticipantesDaComoResultadoSuSeleccionSinOtraTirada() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        caso.iniciar();
        caso.tirarTodos();
        caso.jugador(0).setAbandono(true);

        // Ejecución
        caso.eventos.participantesCambiaron(caso.partida);

        // Verificación
        assertEquals(2L, caso.partida.getEventoGlobal().getSeleccionado());
        assertTrue(caso.partida.getEventoGlobal().estaResuelto());
        assertEquals(10, caso.jugador(1).getPosicionActual());
        assertEquals(8, caso.jugador(0).getPosicionActual());
        verify(caso.dados, times(3)).tirarDados();
    }

    @Test
    void dadoQueTodosLosEmpatadosSeRetiranCuandoSeActualizanParticipantesDaComoResultadoElCierreSinConsecuencias() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(4, TipoEventoGlobal.INUNDACION, 0);
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(6, 6),
                new ResultadoTirada(1, 1), new ResultadoTirada(2, 2));
        caso.iniciar();
        caso.tirarTodos();
        caso.jugador(0).setAbandono(true);
        caso.jugador(1).setEnBancarrota(true);

        // Ejecución
        caso.eventos.participantesCambiaron(caso.partida);

        // Verificación
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertNull(caso.partida.getEventoGlobal().getSeleccionado());
        assertEquals(18, caso.jugador(2).getPosicionActual());
        assertEquals(23, caso.jugador(3).getPosicionActual());
        verify(caso.dados, times(4)).tirarDados();
        verify(caso.temporizador).programarTemporizador(eq("GLOBAL"), anyInt(), any());
    }

    @Test
    void dadoQueHayUnDesempateNuevoCuandoSeEjecutaElTemporizadorAnteriorDaComoResultadoLaEtapaActualSinCambios() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        when(caso.dados.tirarDados()).thenReturn(new ResultadoTirada(6, 6), new ResultadoTirada(6, 6), new ResultadoTirada(1, 1));
        caso.iniciar();
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimiento.capture());
        caso.tirarTodos();
        Long revision = caso.partida.getEventoGlobal().getRevision();

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        assertEquals(2, caso.partida.getEventoGlobal().getEtapa());
        assertEquals(revision, caso.partida.getEventoGlobal().getRevision());
        assertFalse(caso.partida.getEventoGlobal().estaResuelto());
        verify(caso.dados, times(3)).tirarDados();
        assertEquals(8, caso.jugador(0).getPosicionActual());
    }

    @Test
    void dadoQueElEventoYaCerroCuandoSeEjecutaSuTemporizadorDaComoResultadoUnaSolaReanudacion() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
        caso.iniciar();
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimiento.capture());
        caso.confirmarTodos();

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(2400, caso.jugador(0).getDinero());
        verify(caso.temporizador, times(1)).programarTemporizador(eq("GLOBAL"), anyInt(), any());
        verifyNoInteractions(caso.dados);
    }

    @Test
    void dadoQueLaPartidaNoExisteCuandoSeConsultaDaComoResultadoUnErrorDePartidaInexistente() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        when(caso.repositorio.buscarPartidaPorCodigoUnico("GLOBAL")).thenReturn(null);

        // Ejecución
        assertThrows(IllegalArgumentException.class, () -> caso.eventos.consultar("GLOBAL", 1L));

        // Verificación
        verify(caso.repositorio, never()).bloquearPartida(any());
        verifyNoInteractions(caso.dados, caso.temporizador);
    }

    @Test
    void dadoQueElJugadorEsAjenoALaPartidaCuandoConsultaDaComoResultadoElAccesoDenegado() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();
        Long revision = caso.partida.getEventoGlobal().getRevision();

        // Ejecución
        assertThrows(SecurityException.class, () -> caso.eventos.consultar("GLOBAL", 99L));

        // Verificación
        assertEquals(revision, caso.partida.getEventoGlobal().getRevision());
        assertTrue(caso.partida.getEventoGlobal().getTiradas().isEmpty());
        verifyNoInteractions(caso.dados);
    }

    @Test
    void dadoQueComenzoOtraRondaDeEventoCuandoLlegaUnTemporizadorViejoDaComoResultadoElNuevoEventoConservado() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.FIAT_600, 0);
        caso.iniciar();
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimiento.capture());
        caso.confirmarTodos();
        caso.partida.setRondaActual(10);
        caso.eventos.activarSiCorresponde(caso.partida);
        Long revision = caso.partida.getEventoGlobal().getRevision();

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        assertTrue(caso.partida.getEventoGlobal().estaActivo());
        assertEquals(10, caso.partida.getEventoGlobal().getUltimaRonda());
        assertEquals(revision, caso.partida.getEventoGlobal().getRevision());
        assertTrue(caso.partida.getEventoGlobal().getConfirmados().isEmpty());
        assertEquals(2800, caso.jugador(0).getDinero());
        verifyNoInteractions(caso.dados);
    }

    @Test
    void dadoQueElEventoYaTieneResultadoCuandoLlegaElTemporizadorDeTiradasDaComoResultadoElPlazoDeConfirmacionConservado() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimiento.capture());
        caso.tirarTodos();
        Long revision = caso.partida.getEventoGlobal().getRevision();
        java.time.Instant plazo = caso.partida.getEventoGlobal().getVencimiento();

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        assertTrue(caso.partida.getEventoGlobal().estaActivo());
        assertTrue(caso.partida.getEventoGlobal().estaResuelto());
        assertEquals(plazo, caso.partida.getEventoGlobal().getVencimiento());
        assertEquals(revision, caso.partida.getEventoGlobal().getRevision());
        assertEquals(5, caso.jugador(0).getPosicionActual());
        verify(caso.dados, times(3)).tirarDados();
    }

    @Test
    void dadoQueLaDeudaSiguePendienteConElPlazoVencidoCuandoSeConsultaDaComoResultadoLaPausaSinReprogramarTemporizadores() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.ROBO, 0);
        caso.jugador(0).setDinero(100);
        doAnswer(invocacion -> {
            when(caso.quiebras.obtenerDeudaPendiente("GLOBAL", 1L)).thenReturn(new DeudaPendiente());
            return null;
        }).when(caso.quiebras).registrarDeuda(eq("GLOBAL"), eq(caso.jugador(0)), isNull(), eq(500), eq("ROBO_EVENTO_GLOBAL"));
        caso.iniciar();
        caso.confirmarTodos();
        ReflectionTestUtils.setField(caso.partida.getEventoGlobal(), "vencimientoEvento", java.time.Instant.EPOCH);
        clearInvocations(caso.temporizador);

        // Ejecución
        Map<String, Object> estado = caso.eventos.consultar("GLOBAL", 1L);

        // Verificación
        assertEquals(true, estado.get("activo"));
        assertEquals(true, estado.get("deudaPendiente"));
        assertEquals(100, caso.jugador(0).getDinero());
        verifyNoInteractions(caso.temporizador, caso.dados);
        verify(caso.quiebras, times(1)).registrarDeuda(anyString(), any(), isNull(), eq(500), anyString());
    }

    @Test
    void dadoQueSeRetiranLosDemasJugadoresCuandoVenceElTemporizadorDaComoResultadoElCierreSinTiradasAutomaticas() {
        // Preparación
        EscenarioPruebaEventoGlobal caso = new EscenarioPruebaEventoGlobal(3, TipoEventoGlobal.INUNDACION, 0);
        caso.iniciar();
        ArgumentCaptor<Runnable> vencimiento = ArgumentCaptor.forClass(Runnable.class);
        verify(caso.temporizador).programarTemporizadorEvento(eq("evento:GLOBAL"), anyLong(), anyInt(), vencimiento.capture());
        caso.jugador(1).setAbandono(true);
        caso.jugador(2).setEnBancarrota(true);

        // Ejecución
        vencimiento.getValue().run();

        // Verificación
        assertFalse(caso.partida.getEventoGlobal().estaActivo());
        assertNull(caso.partida.getEventoGlobal().getSeleccionado());
        assertTrue(caso.partida.getEventoGlobal().getTiradas().isEmpty());
        assertEquals(8, caso.jugador(0).getPosicionActual());
        verifyNoInteractions(caso.dados);
        verify(caso.temporizador, atLeastOnce()).cancelarTemporizadorEvento(eq("evento:GLOBAL"), anyLong());
    }

    
}
