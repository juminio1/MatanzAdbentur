package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AplicadorConsecuenciaEventoGlobalTest {
    @Test
    void dadoQueElSeleccionadoEstaInactivoCuandoSeAplicaLaConsecuenciaDaComoResultadoElRechazoSinEfectos() {
        // Preparación
        ServicioEconomia economia = mock(ServicioEconomia.class);
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        Jugador jugador = jugador(1L, 8);
        jugador.setAbandono(true);
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(
                economia, movimiento, quiebras, new ServicioParticipacionImpl());

        // Ejecución
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> aplicador.aplicar(partida(jugador), evento(TipoEventoGlobal.ROBO, 500), jugador));

        // Verificación
        assertEquals(2000, jugador.getDinero());
        assertEquals(8, jugador.getPosicionActual());
        verifyNoInteractions(economia, movimiento, quiebras);
    }

    @Test
    void dadoQueInundacionTerminaEnPropiedadAjenaCuandoResuelveCaidaDaComoResultadoAlquiler() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        ServicioPropiedad propiedades = mock(ServicioPropiedad.class);
        ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
        ServicioEventoCasillero tarjetas = mock(ServicioEventoCasillero.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = jugador(1L, 8);
        Jugador propietario = jugador(2L, 9);
        Propiedad propiedad = mock(Propiedad.class);
        CasilleroPropiedad casillero = mock(CasilleroPropiedad.class);
        Partida partida = partida(jugador, propietario);
        when(participacion.estaActivo(jugador)).thenReturn(true);
        when(casilleros.buscarCasilleroPropiedad(partida, 5)).thenReturn(casillero);
        when(casillero.getPropiedad()).thenReturn(propiedad);
        when(propiedad.getPropietario()).thenReturn(propietario);
        when(propiedad.getNombre()).thenReturn("UNLaM");
        when(propiedades.calcularAlquiler(casillero, propiedad)).thenReturn(200);
        when(quiebras.procesarAlquiler("GLOBAL", propiedad, jugador, 200))
                .thenReturn(ResultadoDeuda.RESUELTA);
        doAnswer(invocacion -> {
            jugador.setPosicionActual(5);
            return null;
        }).when(movimiento).moverJugador(eq(jugador), eq(-3), anyInt());
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(
                mock(ServicioEconomia.class), movimiento, quiebras, participacion,
                casilleros, propiedades, tarjetas);

        String detalle = aplicador.aplicar(partida, evento(TipoEventoGlobal.INUNDACION, -3), jugador);

        assertEquals(5, jugador.getPosicionActual());
        assertTrue(detalle.contains("Alquiler: $200"));
        verify(quiebras).procesarAlquiler("GLOBAL", propiedad, jugador, 200);
    }

    @Test
    void dadoQueAfipTerminaEnEventoCuandoResuelveCaidaDaComoResultadoTarjeta() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        ServicioEventoCasillero tarjetas = mock(ServicioEventoCasillero.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador elegido = jugador(1L, 1);
        Jugador adelantado = jugador(2L, 3);
        Partida partida = partida(elegido, adelantado);
        when(participacion.estaActivo(any(Jugador.class))).thenReturn(true);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(
                List.of(new CasilleroEvento(null, 3, "EVENTOS", "Tarjeta", null, null)));
        EventoCasillero tarjeta = mock(EventoCasillero.class);
        when(tarjeta.getNombre()).thenReturn("Changa de finde");
        when(tarjeta.getDescripcion()).thenReturn("Ganaste $500.");
        when(tarjetas.aplicarSiCorresponde("GLOBAL", elegido))
                .thenReturn(Optional.of(new ResultadoEvento(tarjeta, 0)));
        doAnswer(invocacion -> {
            elegido.setPosicionActual(3);
            return null;
        }).when(movimiento).moverJugador(eq(elegido), eq(2), anyInt());
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(
                mock(ServicioEconomia.class), movimiento, mock(ServicioGestionQuiebra.class),
                participacion, casilleros, mock(ServicioPropiedad.class), tarjetas);

        String detalle = aplicador.aplicar(partida, evento(TipoEventoGlobal.TRAMITES_AFIP, 0), elegido);

        assertTrue(detalle.contains("Changa de finde"));
        assertSame(tarjeta, partida.getEventoGlobal().getTarjeta());
        verify(tarjetas).aplicarSiCorresponde("GLOBAL", elegido);
    }

    @Test
    void dadoQueInundacionTerminaEnComisariaCuandoResuelveCaidaDaComoResultadoPreso() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = jugador(1L, 11);
        Partida partida = partida(jugador);
        when(participacion.estaActivo(jugador)).thenReturn(true);
        doAnswer(invocacion -> {
            jugador.setPosicionActual(8);
            return null;
        }).when(movimiento).moverJugador(eq(jugador), eq(-3), anyInt());
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(
                mock(ServicioEconomia.class), movimiento, mock(ServicioGestionQuiebra.class),
                participacion, casilleros, mock(ServicioPropiedad.class), mock(ServicioEventoCasillero.class));

        String detalle = aplicador.aplicar(partida, evento(TipoEventoGlobal.INUNDACION, -3), jugador);

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
        assertTrue(detalle.contains("comisaría"));
    }

    @Test
    void dadoQueJugadorEstaPresoCuandoHayMovimientoGlobalDaComoResultadoNoMover() {
        ServicioMovimiento movimiento = mock(ServicioMovimiento.class);
        ServicioParticipacion participacion = mock(ServicioParticipacion.class);
        Jugador jugador = jugador(1L, 8);
        jugador.getEstadoCarcel().encarcelar();
        when(participacion.estaActivo(jugador)).thenReturn(true);
        AplicadorConsecuenciaEventoGlobal aplicador = new AplicadorConsecuenciaEventoGlobal(
                mock(ServicioEconomia.class), movimiento, mock(ServicioGestionQuiebra.class), participacion,
                mock(ServicioCasillero.class), mock(ServicioPropiedad.class),
                mock(ServicioEventoCasillero.class));

        String detalle = aplicador.aplicar(partida(jugador), evento(TipoEventoGlobal.INUNDACION, -3), jugador);

        assertTrue(detalle.contains("preso"));
        assertEquals(8, jugador.getPosicionActual());
        verifyNoInteractions(movimiento);
    }

    @Test
    void movimientoGlobalQueAvanzaCruzandoInicioNoOtorgaPremio() {
        // Valor positivo deliberado para cubrir cualquier desplazamiento global.
        comprobarMovimientoSinPremio(TipoEventoGlobal.INUNDACION, 3, 28, 1);
    }

    @Test
    void piqueteQueEnviaDirectamenteAInicioNoOtorgaPremio() {
        comprobarMovimientoSinPremio(TipoEventoGlobal.PIQUETE, 0, 28, 0);
    }

    private void comprobarMovimientoSinPremio(TipoEventoGlobal tipo, int valor, int origen, int destino) {
        Jugador jugador = jugador(1L, origen);
        Partida partida = partida(jugador);
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        var repositorio = mock(com.tallerwebi.infraestructura.RepositorioPartida.class);
        when(repositorio.buscarPartidaPorJugador(1L)).thenReturn(partida);
        ServicioEconomia economia = new ServicioEconomiaImpl();
        ServicioMovimiento movimiento = new ServicioMovimientoImpl(economia, repositorio,
                new ServicioParticipacionImpl());
        var aplicador = new AplicadorConsecuenciaEventoGlobal(economia, movimiento,
                mock(ServicioGestionQuiebra.class), new ServicioParticipacionImpl());

        String detalle = aplicador.aplicar(partida, evento(tipo, valor), jugador);

        assertEquals(destino, jugador.getPosicionActual());
        assertEquals(2000, jugador.getDinero());
        org.junit.jupiter.api.Assertions.assertFalse(detalle.contains("Recibió $200"));
    }

    private Jugador jugador(Long id, int posicion) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo("Jugador " + id);
        jugador.setPosicionActual(posicion);
        jugador.setDinero(2000);
        return jugador;
    }

    private Partida partida(Jugador... jugadores) {
        Partida partida = new Partida();
        partida.setCodigoUnico("GLOBAL");
        partida.setJugadores(List.of(jugadores));
        return partida;
    }

    private EventoGlobal evento(TipoEventoGlobal tipo, int valor) {
        EventoGlobal evento = new EventoGlobal();
        evento.setTipoEventoGlobal(tipo);
        evento.setValor(valor);
        return evento;
    }
}
