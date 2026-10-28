package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.infraestructura.RepositorioEvento;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ServicioEventoVolverInicioTest {

    @Test
    public void dadoQuePiqueteTieneUnValorDistintoDeCeroCuandoSeAplicaYDaJugadorEnInicio() throws Exception {
        ServicioEventoImpl servicio = new ServicioEventoImpl(mock(RepositorioEvento.class), mock(ServicioDado.class),
                mock(ApplicationEventPublisher.class), mock(GestorTemporizadorPartida.class), mock(RepositorioPartida.class));
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(2);
        EventoGlobal evento = new EventoGlobal();
        evento.setTipoEfecto(TipoEfecto.VOLVER_INICIO);
        evento.setValor(15);

        servicio.aplicarEvento(jugador, evento);

        assertEquals(0, jugador.getPosicionActual());
    }

    @Test
    public void dadoQueUnEventoTieneUnDestinoEspecificoCuandoSeAplicaYDaElCasilleroIndicado() throws Exception {
        ServicioEventoImpl servicio = new ServicioEventoImpl(mock(RepositorioEvento.class), mock(ServicioDado.class),
                mock(ApplicationEventPublisher.class), mock(GestorTemporizadorPartida.class), mock(RepositorioPartida.class));
        Jugador jugador = new Jugador();
        jugador.setPosicionActual(2);
        EventoGlobal evento = new EventoGlobal();
        evento.setTipoEfecto(TipoEfecto.IR_A_CASILLERO);
        evento.setValor(15);

        servicio.aplicarEvento(jugador, evento);

        assertEquals(15, jugador.getPosicionActual());
    }
}
