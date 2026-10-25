
package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.Mockito.*;

public class NotificadorPartidaTest {

    private SimpMessagingTemplate templateMock;
    private ServicioPartida servicioPartidaMock;
    private NotificadorPartida notificadorPartida;

    @BeforeEach
    public void setUp() {
        templateMock = mock(SimpMessagingTemplate.class);
        servicioPartidaMock = mock(ServicioPartida.class);
        notificadorPartida = new NotificadorPartida(templateMock);
    }

    @Test
    public void dadoUnCambioDeTurnoQuieroNotificarloPorWebSocket() {
        // Preparación
        TurnoDTO turno = new TurnoDTO();
        // Ejecución
        notificadorPartida.notificarCambioDeTurno("ABC123", turno);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/turno", turno);
    }

    @Test
    public void dadoUnMovimientoQuieroNotificarloPorWebSocket() {
        // Preparación
        RespuestaMovimientoDTO movimiento = new RespuestaMovimientoDTO();
        movimiento.setIdJugador(59L);
        movimiento.setDado1(3);
        movimiento.setDado2(2);
        movimiento.setSuma(5);
        movimiento.setNuevaPosicion(8);
        // Ejecución
        notificadorPartida.notificarMovimiento("ABC123", movimiento);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123", movimiento);
    }

    @Test
    public void dadoUnPagoDeAlquilerNotificaAlPagadorLosDatosDelCobro() {
        // Preparación
        Jugador pagador = jugador(1L, "Ana");
        Jugador propietario = jugador(2L, "Luna");
        // Ejecución
        NotificadorPartida.notificarAlquiler(templateMock, "ABC123", pagador, propietario, "UNLaM", 50);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/alquiler", Map.of("idJugadorPagador", 1L, "pagador", "Ana", "propietario", "Luna", "propiedad", "UNLaM", "monto", 50));
    }

    @Test
    public void dadoUnSaldoDePartidaNotificaElDineroDeCadaJugador() {
        // Preparación
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugadorConSaldo(1L, 125), jugadorConSaldo(2L, 300)));
        when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(partida);
        // Ejecución
        NotificadorPartida.notificarSaldos(templateMock, servicioPartidaMock, "ABC123");
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/dinero", Map.of("idJugador", 1L, "dinero", 125, "enBancarrota", false));
        verify(templateMock).convertAndSend("/topic/partida/ABC123/dinero", Map.of("idJugador", 2L, "dinero", 300, "enBancarrota", false));
        verify(templateMock, times(2)).convertAndSend(eq("/topic/partida/ABC123/dinero"), any(Map.class));
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeNotificanLosSaldosEntoncesNoSeEnviaNingunMensaje() {
        // Preparación
        when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(null);
        // Ejecución
        NotificadorPartida.notificarSaldos(templateMock, servicioPartidaMock, "ABC123");
        // Verificación
        verifyNoInteractions(templateMock);
    }

    @Test
    public void dadoQueLaPartidaNoTieneListaDeJugadoresCuandoSeNotificanLosSaldosEntoncesNoSeEnviaNingunMensaje() {
        // Preparación
        Partida partida = new Partida();
        partida.setJugadores(null);
        when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(partida);
        // Ejecución
        NotificadorPartida.notificarSaldos(templateMock, servicioPartidaMock, "ABC123");
        // Verificación
        verifyNoInteractions(templateMock);
    }

    @Test
    public void dadoQueUnJugadorAbandonaYQuedaUnoSoloCuandoSeNotificaEntoncesSeComunicaLaVictoria() {
        // Preparación
        Jugador jugadorQueAbandona = jugador(1L, "Prueba3");
        jugadorQueAbandona.setAbandono(true);
        Jugador jugadorRestante = jugador(2L, "Messi10");
        jugadorRestante.setAbandono(false);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setJugadores(List.of(jugadorQueAbandona, jugadorRestante));
        // Ejecución
        notificadorPartida.notificarAbandonoPorWebSocket(partida, jugadorQueAbandona);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/abandono", Map.of("idJugador", 1L, "apodo", "Prueba3", "victoriaPorAbandono", true, "idGanador", 2L));
    }

    @Test
    public void dadoQueUnJugadorAbandonaYQuedanVariosJugadoresCuandoSeNotificaEntoncesNoSeComunicaLaVictoria() {
        // Preparación
        Jugador jugadorQueAbandona = jugador(1L, "Prueba3");
        jugadorQueAbandona.setAbandono(true);
        Jugador jugadorDos = jugador(2L, "Messi10");
        jugadorDos.setAbandono(false);
        Jugador jugadorTres = jugador(3L, "Luna");
        jugadorTres.setAbandono(false);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setJugadores(List.of(jugadorQueAbandona, jugadorDos, jugadorTres));
        // Ejecución
        notificadorPartida.notificarAbandonoPorWebSocket(partida, jugadorQueAbandona);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/abandono", Map.of("idJugador", 1L, "apodo", "Prueba3", "victoriaPorAbandono", false));
    }

    @Test
    public void dadoQueYaExisteUnJugadorAbandonadoCuandoOtroAbandonaEntoncesSeComunicaLaVictoriaDelUnicoRestante() {
        // Preparación
        Jugador jugadorQueAbandona = jugador(1L, "Prueba3");
        jugadorQueAbandona.setAbandono(true);
        Jugador jugadorAbandonado = jugador(2L, "Prueba4");
        jugadorAbandonado.setAbandono(true);
        Jugador jugadorRestante = jugador(3L, "Messi10");
        jugadorRestante.setAbandono(false);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setJugadores(List.of(jugadorQueAbandona, jugadorAbandonado, jugadorRestante));
        // Ejecución
        notificadorPartida.notificarAbandonoPorWebSocket(partida, jugadorQueAbandona);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/abandono", Map.of("idJugador", 1L, "apodo", "Prueba3", "victoriaPorAbandono", true, "idGanador", 3L));
    }

    @Test
    public void dadoQueTodosLosJugadoresAbandonaronCuandoSeNotificaElAbandonoEntoncesNoSeComunicaUnGanador() {
        // Preparación
        Jugador jugadorQueAbandona = jugador(1L, "Prueba3");
        jugadorQueAbandona.setAbandono(true);
        Jugador jugadorAbandonado = jugador(2L, "Prueba4");
        jugadorAbandonado.setAbandono(true);
        Partida partida = new Partida();
        partida.setCodigoUnico("ABC123");
        partida.setJugadores(List.of(jugadorQueAbandona, jugadorAbandonado));
        // Ejecución
        notificadorPartida.notificarAbandonoPorWebSocket(partida, jugadorQueAbandona);
        // Verificación
        verify(templateMock).convertAndSend("/topic/partida/ABC123/abandono", Map.of("idJugador", 1L, "apodo", "Prueba3", "victoriaPorAbandono", false));
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        return jugador;
    }

    private Jugador jugadorConSaldo(Long id, Integer dinero) {
        Jugador jugador = jugador(id, "Jugador " + id);
        jugador.setDinero(dinero);
        return jugador;
    }
}
