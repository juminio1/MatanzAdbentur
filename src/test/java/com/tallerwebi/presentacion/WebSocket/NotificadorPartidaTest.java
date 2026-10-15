package com.tallerwebi.presentacion.WebSocket;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

public class NotificadorPartidaTest {

    //Verifica que notificarCambioDeTurno envíe correctamente la información del turno al canal WebSocket.
    @Test
    public void dadoUnCambioDeTurnoQuieroNotificarloPorWebSocket() {
        // Preparación
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        NotificadorPartida notificador = new NotificadorPartida(template);

        TurnoDTO turno = new TurnoDTO();

        // Ejecución
        notificador.notificarCambioDeTurno("ABC123", turno);

        // Verificación
        verify(template).convertAndSend(
                "/topic/partida/ABC123/turno",
                turno
        );
    }

    //Este test verifica el envío del movimiento
    @Test
    public void dadoUnMovimientoQuieroNotificarloPorWebSocket() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        NotificadorPartida notificador = new NotificadorPartida(template);

        RespuestaMovimientoDTO movimiento = new RespuestaMovimientoDTO();
        movimiento.setIdJugador(59L);
        movimiento.setDado1(3);
        movimiento.setDado2(2);
        movimiento.setSuma(5);
        movimiento.setNuevaPosicion(8);

        // Ejecución
        notificador.notificarMovimiento("ABC123", movimiento);

        // Verificación
        verify(template).convertAndSend(
                "/topic/partida/ABC123",
                movimiento
        );
    }

    @Test
    public void dadoUnPagoDeAlquilerNotificaAlPagadorLosDatosDelCobro() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        Jugador pagador = jugador(1L, "Ana");
        Jugador propietario = jugador(2L, "Luna");

        NotificadorPartida.notificarAlquiler(template, "ABC123", pagador, propietario, "UNLaM", 50);

        verify(template).convertAndSend("/topic/partida/ABC123/alquiler", Map.of(
                "idJugadorPagador", 1L,
                "pagador", "Ana",
                "propietario", "Luna",
                "propiedad", "UNLaM",
                "monto", 50
        ));
    }

    @Test
    public void dadoUnSaldoDePartidaNotificaElDineroDeCadaJugador() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ServicioPartida servicioPartida = mock(ServicioPartida.class);
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugadorConSaldo(1L, 125), jugadorConSaldo(2L, 300)));
        when(servicioPartida.obtenerPartida("ABC123")).thenReturn(partida);

        NotificadorPartida.notificarSaldos(template, servicioPartida, "ABC123");

        verify(template).convertAndSend("/topic/partida/ABC123/dinero",
                Map.of("idJugador", 1L, "dinero", 125));
        verify(template).convertAndSend("/topic/partida/ABC123/dinero",
                Map.of("idJugador", 2L, "dinero", 300));
    }

    @Test
    public void noNotificaSaldosSiLaPartidaNoExiste() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ServicioPartida servicioPartida = mock(ServicioPartida.class);
        when(servicioPartida.obtenerPartida("ABC123")).thenReturn(null);

        NotificadorPartida.notificarSaldos(template, servicioPartida, "ABC123");

        verifyNoInteractions(template);
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
