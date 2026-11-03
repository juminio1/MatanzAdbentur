package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.dominio.catalogo.CatalogoTablero;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioMovimientoImpl implements ServicioMovimiento {
    public static final int PREMIO_INICIO = 200;
    private final ServicioEconomia economia;
    private final RepositorioPartida repositorioPartida;
    private final ServicioParticipacion participacion;
    public ServicioMovimientoImpl(ServicioEconomia economia, RepositorioPartida repositorioPartida,
                                  ServicioParticipacion participacion) {
        this.economia = economia;
        this.repositorioPartida = repositorioPartida;
        this.participacion = participacion;
    }
    @Override
    public void moverJugador(Jugador jugador, Integer avance, Integer totalCasilleros) {
        if (jugador == null || jugador.getPosicionActual() == null || avance == null || totalCasilleros == null || totalCasilleros <= 0) {
            throw new IllegalArgumentException("Movimiento inválido");
        }
        participacion.validarPartidaEnCurso(repositorioPartida.buscarPartidaPorJugador(jugador.getId()));
        jugador.setPosicionActual(calcularPosicionCircular(jugador.getPosicionActual(), avance, totalCasilleros));
    }

    // Calcula el destino sin mover la ficha ni aplicar premios o consecuencias. 
    public static Integer calcularPosicionCircular(Integer posicion, Integer avance, Integer totalCasilleros) {
        return Math.floorMod(Math.addExact(posicion, avance), totalCasilleros);
    }

    @Override
    public int avanzar(Jugador jugador, int avance) {
        int premio = calcularPremioPorAvance(jugador.getPosicionActual(), avance);
        moverJugador(jugador, avance, CatalogoTablero.TOTAL_CASILLEROS);
        if (premio > 0) economia.sumarDinero(jugador, premio);
        return premio;
    }
    @Override
    public int avanzarAPosicion(Jugador jugador, int posicion) {
        if (posicion < 0 || posicion >= CatalogoTablero.TOTAL_CASILLEROS) throw new IllegalArgumentException("Posición inválida");
        return avanzar(jugador, Math.floorMod(posicion - jugador.getPosicionActual(), CatalogoTablero.TOTAL_CASILLEROS));
    }
    @Override
    public int calcularPremioPorAvance(int posicion, int avance) {
        int vueltas = Math.max(0, Math.floorDiv(Math.addExact(posicion, avance), CatalogoTablero.TOTAL_CASILLEROS));
        return Math.multiplyExact(vueltas, PREMIO_INICIO);
    }
}
