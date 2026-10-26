package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Reglas compartidas de participación, independientes de HTTP y persistencia. */
@Service
public class ServicioParticipacionImpl implements ServicioParticipacion {
    @Override
    public boolean estaActivo(Jugador jugador) {
        return jugador != null && !Boolean.TRUE.equals(jugador.getEnBancarrota()) && !Boolean.TRUE.equals(jugador.getAbandono());
    }
    @Override
    public boolean pertenece(Partida partida, Long idJugador) {
        return partida != null && idJugador != null && partida.getJugadores() != null
                && partida.getJugadores().stream().anyMatch(j -> idJugador.equals(j.getId()));
    }
    @Override
    public boolean permiteAdministrarPropiedades(Partida partida, Jugador jugador, Long idJugador) {
        return estaActivo(jugador) && Objects.equals(jugador.getId(), idJugador)
                && pertenece(partida, idJugador);
    }
}
