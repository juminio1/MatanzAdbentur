package com.tallerwebi.dominio.obvservadores;

import com.tallerwebi.dominio.eventos.EventoGlobalFinalizado;
import com.tallerwebi.dominio.observadores.ObservadorReanudarPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ObservadorReanudarPartidaTest {

    @Test
    public void dadoQueFinalizaUnEventoGlobalCuandoElObservadorRecibeLaNotificacionYDaLaReanudacionDelTurno() {

        // Preparación
        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);

        ObservadorReanudarPartida observador = new ObservadorReanudarPartida(servicioPartidaMock);

        EventoGlobalFinalizado evento = new EventoGlobalFinalizado("MATANZA123");

        // Ejecución
        observador.reanudarPartida(evento);

        // Verificación
        verify(servicioPartidaMock, times(1)).iniciarTemporizadorTurnoSiNoExiste("MATANZA123");

        verifyNoMoreInteractions(servicioPartidaMock);
    }

    @Test
    public void dadoQueFinalizanEventosDeDistintasPartidasCuandoElObservadorRecibeAmbasNotificacionesYDaLaReanudacionDeCadaPartida() {

        // Preparación
        ServicioPartida servicioPartidaMock = mock(ServicioPartida.class);

        ObservadorReanudarPartida observador =new ObservadorReanudarPartida(servicioPartidaMock);

        EventoGlobalFinalizado primerEvento = new EventoGlobalFinalizado("SALA-001");

        EventoGlobalFinalizado segundoEvento = new EventoGlobalFinalizado("SALA-002");

        // Ejecución
        observador.reanudarPartida(primerEvento);
        observador.reanudarPartida(segundoEvento);

        // Verificación
        verify(servicioPartidaMock, times(1)).iniciarTemporizadorTurnoSiNoExiste("SALA-001");

        verify(servicioPartidaMock, times(1)).iniciarTemporizadorTurnoSiNoExiste("SALA-002");

        verifyNoMoreInteractions(servicioPartidaMock);
    }
}