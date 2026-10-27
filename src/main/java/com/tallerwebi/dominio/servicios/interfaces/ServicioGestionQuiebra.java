package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;

public interface ServicioGestionQuiebra {

    void registrarDeuda(
            String codigoPartida,
            Jugador deudor,
            Jugador acreedor,
            Integer monto,
            String motivo);

    DeudaPendiente obtenerDeudaPendiente(String codigoPartida, Long idJugador);
    Boolean intentarResolverDeuda(String codigoPartida, Long idJugador);
    Boolean venderPropiedadYResolverDeuda(String codigoPartida,Long idJugador,Long idPropiedad) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    ResultadoDeuda evaluarDeuda(String codigoPartida,Long idJugador) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    Partida declararQuiebra(String codigoPartida, Long idJugador) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;
    ResultadoDeuda procesarAlquiler(String codigoPartida, Propiedad propiedad, Jugador jugador, Integer montoAlquiler) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException;


}
