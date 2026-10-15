package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;

public interface ServicioPropiedad {
    void comprarPropiedad(Long idPropiedad, Jugador jugador);
    void comprarPropiedadParaJugador(Long idPropiedad, Long idJugador);
    Integer venderPropiedadParaJugador(Long idPropiedad, Long idJugador);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler);
    Propiedad obtenerPropiedadExistente(Long idPropiedad);
    boolean estaDisponible(Propiedad propiedad);
    Integer calcularAlquiler(Propiedad propiedad);
    Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad);
    void mejorarPropiedad(Long idPropiedad, Jugador jugador);
}
