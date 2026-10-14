package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Evento;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.infraestructura.RepositorioEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioEvento")
public class ServicioEventoImpl implements ServicioEvento {

    private final RepositorioEvento repositorioEvento;

    @Autowired
    public ServicioEventoImpl(RepositorioEvento repositorioEvento) {
        this.repositorioEvento = repositorioEvento;
    }

    @Override
    public Evento obtenerEventoAleatorio() {
        return repositorioEvento.obtenerEventoAleatorio();
    }

    @Override
    public void aplicarEvento(Jugador jugador, Evento evento) {
        if (evento == null || evento.getTipoEfecto() == null) {
            return;
        }

        switch (evento.getTipoEfecto()) {
            case GANAR_PLATA:
                jugador.setDinero(jugador.getDinero() + evento.getValor());
                break;
            case PERDER_PLATA:
                jugador.setDinero(jugador.getDinero() - evento.getValor());
                break;
            case PERDER_TURNO:
                jugador.setPierdeTurno(true);
                break;
            case MOVER_CASILLEROS:
                int nuevaPosicion = jugador.getPosicionActual() + evento.getValor();
                if (nuevaPosicion < 0) {
                    nuevaPosicion = 30 + nuevaPosicion;
                }
                jugador.setPosicionActual(nuevaPosicion);
                break;
            case IR_A_CASILLERO:
                jugador.setPosicionActual(evento.getValor());
                break;
        }
    }
}
