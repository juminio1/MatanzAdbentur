package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.ResultadoDeuda;

public interface ServicioGestionQuiebra {

    void registrarDeuda(
            String codigoPartida,
            Jugador deudor,
            Jugador acreedor,
            Integer monto,
            String motivo);

    DeudaPendiente obtenerDeudaPendiente(String codigoPartida, Long idJugador);
    Boolean intentarResolverDeuda(String codigoPartida, Long idJugador);
    Boolean venderPropiedadYResolverDeuda(String codigoPartida,Long idJugador,Long idPropiedad);
    ResultadoDeuda evaluarDeuda(String codigoPartida,Long idJugador);
    Partida declararQuiebra(String codigoPartida, Long idJugador);
    ResultadoDeuda procesarAlquiler(String codigoPartida, Propiedad propiedad, Jugador jugador, Integer montoAlquiler);


}
