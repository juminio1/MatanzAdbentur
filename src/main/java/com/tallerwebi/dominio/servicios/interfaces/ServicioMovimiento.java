package com.tallerwebi.dominio.servicios.interfaces;
import com.tallerwebi.dominio.entidades.Jugador;
public interface ServicioMovimiento {
    void moverJugador(Jugador jugador, Integer avance, Integer totalCasilleros);
    int avanzar(Jugador jugador, int avance);
    int avanzarAPosicion(Jugador jugador, int posicion);
    int calcularPremioPorAvance(int posicion, int avance);
}
