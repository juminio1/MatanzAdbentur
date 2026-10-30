package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Partida;

import java.time.Instant;
import java.util.List;

/** Ofertas de venta de propiedades entre jugadores de una misma partida. */
public interface ServicioOfertaPropiedad {

    // Oferta pendiente de respuesta. Vence en "expira".
    record Oferta(String id, String codigoPartida, Integer posicion, Long idVendedor, Long idComprador,
                  Integer precio, Instant expira, String nombrePropiedad, String apodoVendedor) {}

    // Oferta aceptada: la propiedad ya se transfirió al comprador.
    record OfertaAceptada(Oferta oferta, CasilleroPropiedad casillero) {}

    Oferta crearOferta(Partida partida, Integer posicion, Long idVendedor, Long idComprador, Integer precio);

    List<Oferta> ofertasPendientes(String codigoPartida, Long idComprador);

    void rechazar(String codigoPartida, String idOferta, Long idComprador);

    OfertaAceptada aceptar(Partida partida, String idOferta, Long idComprador);
}
