package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ControladorEventoGlobalTest {
    @Test
    void dadoQueNoHaySesionCuandoSeConsultaElEventoDaComoResultadoAccesoNoAutorizado() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.consultar("ABC", new MockHttpServletRequest()));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void dadoQueSeEnviaOtroIdCuandoSeTiraDaComoResultadoLaIdentidadDeLaSesion() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        ServicioEvento eventos = mock(ServicioEvento.class);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setApodo("Ana");
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        when(partidas.obtenerPartidaParaVisualizacion("ABC")).thenReturn(partida);
        when(eventos.tirar("ABC", 10L, 5, 2)).thenReturn(Map.of("activo", true));
        var controlador = new ControladorEventoGlobal(eventos, partidas, new ServicioParticipacionImpl());
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.getSession().setAttribute("NOMBRE", "Ana");
        solicitud.addParameter("idJugador", "20");
        assertEquals(HttpStatus.OK, controlador.tirar("ABC", 5, 2, solicitud).getStatusCode());
        verify(eventos).tirar("ABC", 10L, 5, 2);
    }
}
