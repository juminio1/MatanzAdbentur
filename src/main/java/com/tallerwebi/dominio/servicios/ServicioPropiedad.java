package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;

public interface ServicioPropiedad {
    record CompraPreparada(String codigoPartida, Jugador jugador, CasilleroPropiedad casillero) {}

    CompraPreparada prepararCompra(String codigoPartida, Integer posicion)
            throws EstadoPartidaInvalidoException;
    void comprarPropiedad(Long idPropiedad, Jugador jugador);
    void comprarPropiedadParaJugador(Long idPropiedad, Long idJugador);
    Integer venderPropiedadParaJugador(Long idPropiedad, Long idJugador);
    void transferirPropiedadParaJugadores(Long idPropiedad, Long idVendedor, Long idComprador, Integer precio);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador);
    void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler);
    Propiedad obtenerPropiedadExistente(Long idPropiedad);
    Boolean estaDisponible(Propiedad propiedad);
    Integer calcularAlquiler(Propiedad propiedad);
    Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad);
    void mejorarPropiedad(Long idPropiedad, Jugador jugador);
}
