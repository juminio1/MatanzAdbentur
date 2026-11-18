package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.excepcion.JugadorInvalidoException;
import com.tallerwebi.dominio.excepcion.PropiedadInvalidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioAlquiler;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.tallerwebi.dominio.catalogo.CatalogoTablero.*;

// Calcula y cobra los alquileres: mejoras, barrio completo y paradas de bondi.
@Service
@Transactional
public class ServicioAlquilerImpl implements ServicioAlquiler {
    private static final List<Integer> ALQUILERES_POR_PARADAS = List.of(0, 20, 50, 100, 200);

    private final RepositorioPropiedad repositorioPropiedad;
    private final RepositorioPartida repositorioPartida;
    private final ServicioCasillero servicioCasillero;
    private final ServicioEconomia economia;
    private final ServicioParticipacion participacion;
    private final BarrioDePropiedad barrios;

    @Autowired
    public ServicioAlquilerImpl(RepositorioPropiedad repositorioPropiedad, RepositorioPartida repositorioPartida,
                                ServicioCasillero servicioCasillero, ServicioEconomia economia,
                                ServicioParticipacion participacion) {
        this.repositorioPropiedad = repositorioPropiedad;
        this.repositorioPartida = repositorioPartida;
        this.servicioCasillero = servicioCasillero;
        this.economia = economia;
        this.participacion = participacion;
        this.barrios = new BarrioDePropiedad(servicioCasillero);
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        ejecutarPagoAlquiler(propiedad, jugador, calcularAlquilerValidada(propiedad));
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        ejecutarPagoAlquiler(propiedad, jugador, montoAlquiler);
    }

    private void ejecutarPagoAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        if (montoAlquiler == null || montoAlquiler < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }
        // El alquiler automático causado por un evento global sí debe cobrarse
        // durante la pausa, pero las demás acciones ordinarias siguen bloqueadas.
        Partida partida = repositorioPartida.buscarPartidaPorJugador(jugador.getId());
        participacion.validarPartidaEnCurso(partida);
        if (!partida.getEventoGlobal().estaActivo()) {
            participacion.validarAccionOrdinaria(partida);
        }
        Propiedad propiedadPersistida = repositorioPropiedad.buscarPropiedadPorId(propiedad.getId());
        if (propiedadPersistida == null) propiedadPersistida = propiedad;
        Jugador propietario = propiedadPersistida.getPropietario();
        if (propietario == null || propietario.getId().equals(jugador.getId())) {
            return;
        }
        Jugador pagador = repositorioPartida.buscarJugadorPorId(jugador.getId());
        if (pagador == null) pagador = jugador;
        economia.restarDinero(pagador, montoAlquiler);
        jugador.setDinero(pagador.getDinero());
        repositorioPartida.actualizarJugador(pagador);
        economia.sumarDinero(propietario, montoAlquiler);
        repositorioPartida.actualizarJugador(propietario);
    }

    // Calcula el alquiler de la propiedad solo con sus mejoras.
    @Override
    public Integer calcularAlquiler(Propiedad propiedad) {
        validarPropiedad(propiedad);
        return calcularAlquilerValidada(propiedad);
    }

    private Integer calcularAlquilerValidada(Propiedad propiedad) {
        Integer alquilerBase = propiedad.getPrecioAlquiler();
        Integer nivelMejora = propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora();
        if (alquilerBase == null || alquilerBase < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }
        if (nivelMejora < 0) {
            throw new PropiedadInvalidaException("El nivel de mejora no es válido.");
        }
        if (nivelMejora == 0) return alquilerBase;
        return alquilerConMejora(alquilerBase, nivelMejora);
    }

    // Calcula el alquiler que se cobra al caer en el casillero: paradas, barrio completo y mejoras.
    @Override
    public Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad) {
        Long idPartida = casillero.getPartidaId();
        if (!POSICIONES_PARADA.contains(casillero.getPosicion())) return calcularAlquilerDeBarrio(casillero, propiedad);
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || idPartida == null) return ALQUILER_PARADA;
        Partida referencia = new Partida();
        referencia.setId(idPartida);
        Long cantidadParadas = servicioCasillero.obtenerCasillerosPropiedadDePartida(referencia).stream()
                .filter(c -> POSICIONES_PARADA.contains(c.getPosicion()) && c.getPropiedad() != null)
                .filter(c -> idPartida.equals(c.getPartidaId()))
                .filter(c -> c.getPropiedad().getPropietario() != null
                        && Objects.equals(propietario.getId(), c.getPropiedad().getPropietario().getId()))
                .count();
        return ALQUILERES_POR_PARADAS.get(
                Math.toIntExact(Math.min(cantidadParadas, ALQUILERES_POR_PARADAS.size() - 1)));
    }

    // Si el dueño tiene todo el barrio y la propiedad no tiene mejoras, el alquiler se duplica.
    private Integer calcularAlquilerDeBarrio(CasilleroPropiedad casillero, Propiedad propiedad) {
        boolean barrioCompleto = Optional.ofNullable(propiedad.getPropietario())
                .map(propietario -> barrios.esDuenioDeTodoElBarrio(propiedad, casillero, propietario))
                .orElse(false);
        return alquilerDeBarrio(calcularAlquiler(propiedad),
                Objects.requireNonNullElse(propiedad.getNivelMejora(), 0), barrioCompleto);
    }

    private void validarJugador(Jugador jugador) {
        if (jugador == null) {
            throw new JugadorInvalidoException("El jugador no puede ser nulo.");
        }
    }

    private void validarPropiedad(Propiedad propiedad) {
        if (propiedad == null) {
            throw new PropiedadInvalidaException("No se puede calcular el alquiler de una propiedad inexistente.");
        }
    }
}
