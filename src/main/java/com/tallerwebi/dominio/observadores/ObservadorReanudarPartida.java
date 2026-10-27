package com.tallerwebi.dominio.observadores;

import com.tallerwebi.dominio.eventos.EventoGlobalFinalizado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component //Registra el observador en Spring
public class ObservadorReanudarPartida {

    private final ServicioPartida servicioPartida;

    public ObservadorReanudarPartida(ServicioPartida servicioPartida) {
        this.servicioPartida = servicioPartida;
    }

    @EventListener //Esto lo que hace es q el metodo se ejecute cuandoo se publica un EventoGlobalFinalizado
    public void reanudarPartida(EventoGlobalFinalizado evento) {

        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(evento.codigoPartida());
    }
}