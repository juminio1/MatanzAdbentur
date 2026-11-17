package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tallerwebi.dominio.catalogo.CatalogoTablero.posicionesDelGrupo;

// Responde si un jugador tiene todas las propiedades del barrio de una propiedad en su partida.
// Lo usan el cálculo del alquiler (barrio completo) y las mejoras (requieren el barrio completo).
public class BarrioDePropiedad {
    private final ServicioCasillero servicioCasillero;

    public BarrioDePropiedad(ServicioCasillero servicioCasillero) {
        this.servicioCasillero = servicioCasillero;
    }

    public boolean esDuenioDeTodoElBarrio(Propiedad propiedad, CasilleroPropiedad objetivo, Jugador jugador) {
        Set<Integer> posiciones = posicionesDelGrupo(propiedad.getGrupo());
        if (posiciones.isEmpty()) return false;
        if (objetivo == null || objetivo.getPartidaId() == null
                || !posiciones.contains(objetivo.getPosicion())) return false;
        Set<Integer> posicionesPropias = servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> Objects.equals(objetivo.getPartidaId(), casillero.getPartidaId()))
                .filter(casillero -> perteneceAJugador(casillero.getPropiedad(), jugador))
                .map(CasilleroPropiedad::getPosicion)
                .collect(Collectors.toSet());
        return posicionesPropias.containsAll(posiciones);
    }

    public CasilleroPropiedad buscarCasilleroDePropiedad(Long idPropiedad) {
        if (idPropiedad == null) return null;
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> casillero.getPropiedad() != null
                        && idPropiedad.equals(casillero.getPropiedad().getId()))
                .findFirst().orElse(null);
    }

    private boolean perteneceAJugador(Propiedad propiedad, Jugador jugador) {
        return propiedad != null && propiedad.getPropietario() != null
                && Objects.equals(jugador.getId(), propiedad.getPropietario().getId());
    }
}
