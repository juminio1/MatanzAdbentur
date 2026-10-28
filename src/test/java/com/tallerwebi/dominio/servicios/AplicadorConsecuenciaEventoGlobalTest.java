package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.List;
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
        assertTrue(detalle.contains("por eso no avanzó"));
        verify(repositorio).actualizarJugador(uno);
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
        verify(quiebras).registrarDeuda("ABC", uno, null, 1000, "EVENTO_RESTA");
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
        verify(quiebras).registrarDeuda("ABC", uno, null, 1000, "EVENTO_RESTA");
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
