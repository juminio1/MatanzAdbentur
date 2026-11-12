package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

//Reglas compartidas de participación, independientes de HTTP y persistencia.
@Service
public class ServicioParticipacionImpl implements ServicioParticipacion {
    @Autowired
    private ObjectProvider<GestorTriviaPartida> gestorTrivia;

    @Override
    public boolean estaBloqueadaPorTrivia(Partida partida) {
        if (partida == null || gestorTrivia == null) return false;
        GestorTriviaPartida gestor = gestorTrivia.getIfAvailable();
        return gestor != null && gestor.estaBloqueandoPartida(partida.getCodigoUnico());
    }

    @Override
    public void validarPartidaEnCurso(Partida partida) {
        if (partida == null || partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw new IllegalStateException("La partida no esta en curso.");
        }
    }

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
