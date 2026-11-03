package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AplicadorConsecuenciaEventoGlobalTest {

    private Jugador crearJugador(Long id, Integer dinero, Integer posicion) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setDinero(dinero);
        jugador.setPosicionActual(posicion);
        return jugador;
    }

    private Partida crearPartida(Jugador... jugadores) {
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC");
        partida.setJugadores(List.of(jugadores));
        return partida;
    }

    private EventoGlobal crearEvento(TipoEventoGlobal tipo, TipoEfecto efecto, Integer valor) {
        EventoGlobal evento = new EventoGlobal();
        evento.setTipoEventoGlobal(tipo);
        evento.setTipoEfecto(efecto);
        evento.setValor(valor);
        return evento;
    }

    @Test
    public void dadoQueInundacionSeleccionaRamosMejiaCuandoSePrevisualizaEntoncesAnunciaTresCasillerosAtrasSinMoverLaFicha() {
        Jugador afectado = crearJugador(2L, 1000, 27);
        Partida partida = crearPartida(crearJugador(1L, 1000, 20), afectado);

        String detalle = AplicadorConsecuenciaEventoGlobal.previsualizarMovimiento(partida, afectado,
                crearEvento(TipoEventoGlobal.INUNDACION, TipoEfecto.MOVER_CASILLEROS, -3));

        assertEquals("retrocederá 3 casilleros, del casillero 27 al 24 cuando se reanude la partida.", detalle);
        assertEquals(27, afectado.getPosicionActual());
        assertEquals(1000, afectado.getDinero());
    }

    @Test
    public void dadoQueDosJugadoresEstanActivosCuandoSeEligeUnoAlAzarYDaConsecuenciaParaElSeleccionado() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 9);
        Partida partida = crearPartida(uno, dos);
        Random random = mock(Random.class);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);
        List<Long> jugadoresNotificados = new ArrayList<>();
        when(random.nextInt(2)).thenReturn(1);

        AplicadorConsecuenciaEventoGlobal.aplicarAlAzar(partida,
                crearEvento(TipoEventoGlobal.ACCIDENTE_SARMIENTO, TipoEfecto.PERDER_TURNO, 1),
                random, repositorio, null, (jugador, evento) -> jugador.setPierdeTurno(true),
                (jugador, detalle) -> jugadoresNotificados.add(jugador.getId()));

        assertTrue(dos.getPierdeTurno());
        assertEquals(List.of(2L), jugadoresNotificados);
        verify(repositorio).actualizarJugador(dos);
    }

    @Test
    public void dadoQuePiqueteEligeElMenorCuandoSeComparanTiradasYDaJugadorConMenosPuntos() {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 9);

        List<Jugador> resultado = AplicadorConsecuenciaEventoGlobal.obtenerJugadoresConResultadoElegido(
                List.of(uno, dos), Map.of(1L, 4, 2L, 8), TipoEventoGlobal.PIQUETE);

        assertEquals(List.of(uno), resultado);
    }

    @Test
    public void dadoQueAfipEligeElMayorCuandoSeComparanTiradasYDaJugadorConMasPuntos() {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 9);

        List<Jugador> resultado = AplicadorConsecuenciaEventoGlobal.obtenerJugadoresConResultadoElegido(
                List.of(uno, dos), Map.of(1L, 4, 2L, 8), TipoEventoGlobal.TRAMITES_AFIP);

        assertEquals(List.of(dos), resultado);
    }

    @Test
    public void dadoQueElGanadorDeAfipEstaAtrasCuandoRecibeElBeneficioYDaAvanceDeFicha() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        Jugador dos = crearJugador(2L, 1000, 20);
        Partida partida = crearPartida(uno, dos);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(partida, uno, crearEvento(TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0),
                repositorio, null, (jugador, evento) -> jugador.setPosicionActual(jugador.getPosicionActual() + evento.getValor()));

        assertEquals(20, uno.getPosicionActual());
        assertTrue(detalle.contains("del casillero 5 al 20"));
        verify(repositorio).actualizarJugador(uno);
    }

    @Test
    public void dadoQueElGanadorDeAfipYaEstaAdelantadoCuandoRecibeElBeneficioYDaMensajeSinMovimiento() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 25);
        Jugador dos = crearJugador(2L, 1000, 20);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno, dos), uno,
                crearEvento(TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0), repositorio, null,
                (jugador, evento) -> jugador.setPosicionActual(jugador.getPosicionActual() + evento.getValor()));

        assertEquals(25, uno.getPosicionActual());
        assertEquals("no avanza porque ya es el jugador más adelantado del tablero.", detalle);
        verify(repositorio).actualizarJugador(uno);
    }

    @Test
    public void dadoQueElGanadorYaEstaPrimeroCuandoSePrevisualizaTramitesEntoncesSeExplicaPorQueNoAvanza() {
        Jugador ganador = crearJugador(1L, 1000, 25);
        Jugador otro = crearJugador(2L, 1000, 9);

        String detalle = AplicadorConsecuenciaEventoGlobal.previsualizarMovimiento(crearPartida(ganador, otro), ganador,
                crearEvento(TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0));

        assertEquals("no avanza porque ya es el jugador más adelantado del tablero.", detalle);
        assertEquals(25, ganador.getPosicionActual());
        assertEquals(9, otro.getPosicionActual());
    }

    @Test
    public void dadoQueElGanadorEstaAtrasCuandoSePrevisualizaTramitesEntoncesSeAnunciaElDestinoSinMoverLaFicha() {
        Jugador ganador = crearJugador(1L, 1000, 5);
        Jugador otro = crearJugador(2L, 1000, 20);

        String detalle = AplicadorConsecuenciaEventoGlobal.previsualizarMovimiento(crearPartida(ganador, otro), ganador,
                crearEvento(TipoEventoGlobal.TRAMITES_AFIP, TipoEfecto.MOVER_CASILLEROS, 0));

        assertEquals("se moverá del casillero 5 al 20 cuando se reanude la partida.", detalle);
        assertEquals(5, ganador.getPosicionActual());
    }

    @Test
    public void dadoQueSarmientoEligeUnJugadorCuandoAplicaConsecuenciaYDaTurnoPerdidoPersistido() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 5);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.ACCIDENTE_SARMIENTO, TipoEfecto.PERDER_TURNO, 1), repositorio, null,
                (jugador, evento) -> jugador.setPierdeTurno(true));

        assertTrue(uno.getPierdeTurno());
        assertTrue(detalle.contains("próximo turno"));
        verify(repositorio).actualizarJugador(uno);
    }

    @Test
    public void dadoQueElJugadorTienePlataCuandoKevinLeRobaYDaSaldoDescontado() throws Exception {
        Jugador uno = crearJugador(1L, 1500, 5);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.ROBO, TipoEfecto.PERDER_PLATA, 1000), repositorio, null,
                (jugador, evento) -> jugador.setDinero(jugador.getDinero() - evento.getValor()));

        assertEquals(500, uno.getDinero());
        assertTrue(detalle.contains("$500"));
        verify(repositorio).actualizarJugador(uno);
    }

    @Test
    public void dadoQueElJugadorNoTienePlataCuandoKevinLeRobaYDaDeudaPendiente() throws Exception {
        Jugador uno = crearJugador(1L, 200, 5);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        when(quiebras.evaluarDeuda("ABC", 1L)).thenReturn(ResultadoDeuda.PENDIENTE);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.ROBO, TipoEfecto.PERDER_PLATA, 1000), repositorio, quiebras,
                (jugador, evento) -> jugador.setDinero(jugador.getDinero() - evento.getValor()));

        assertEquals(200, uno.getDinero());
        assertTrue(detalle.contains("vender propiedades"));
        verify(quiebras).registrarDeuda("ABC", uno, null, 1000, "EVENTO_GLOBAL_ROBO");
        verify(quiebras).evaluarDeuda("ABC", 1L);
    }

    @Test
    public void dadoQueElJugadorNoPuedePagarCuandoSeEvaluaElRoboYDaResultadoDeQuiebra() throws Exception {
        Jugador uno = crearJugador(1L, 200, 5);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        when(quiebras.evaluarDeuda("ABC", 1L)).thenReturn(ResultadoDeuda.QUIEBRA);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.ROBO, TipoEfecto.PERDER_PLATA, 1000), repositorio, quiebras,
                (jugador, evento) -> jugador.setDinero(jugador.getDinero() - evento.getValor()));

        assertTrue(detalle.contains("quiebra"));
        verify(quiebras).registrarDeuda("ABC", uno, null, 1000, "EVENTO_GLOBAL_ROBO");
        verify(quiebras).evaluarDeuda("ABC", 1L);
    }

    @Test
    public void dadoQuePiquetePenalizaAUnJugadorCuandoLoDevuelveAlInicioYDaPosicionCeroPersistida() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 2);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.PIQUETE, TipoEfecto.VOLVER_INICIO, 0), repositorio, null,
                (jugador, evento) -> jugador.setPosicionActual(0));

        assertEquals(0, uno.getPosicionActual());
        assertTrue(detalle.contains("del casillero 2 al Inicio (casillero 0)"));
        verify(repositorio).actualizarJugador(uno);
    }

    @Test
    public void dadoQuePiqueteEligeAUnJugadorEnInicioCuandoAplicaConsecuenciaYDaMensajeSinMovimiento() throws Exception {
        Jugador uno = crearJugador(1L, 1000, 0);
        RepositorioPartida repositorio = mock(RepositorioPartida.class);

        String detalle = AplicadorConsecuenciaEventoGlobal.aplicar(crearPartida(uno), uno,
                crearEvento(TipoEventoGlobal.PIQUETE, TipoEfecto.VOLVER_INICIO, 0), repositorio, null,
                (jugador, evento) -> jugador.setPosicionActual(0));

        assertEquals(0, uno.getPosicionActual());
        assertTrue(detalle.contains("ya estaba en Inicio"));
        verify(repositorio).actualizarJugador(uno);
    }
}
