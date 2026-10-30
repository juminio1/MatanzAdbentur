package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.OfertaInvalidaException;
import com.tallerwebi.dominio.excepcion.OfertaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.OfertaNoPermitidaException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda las ofertas de venta entre jugadores mientras están pendientes.
 * Las ofertas viven en memoria y vencen a los cinco minutos.
 */
@Service("servicioOfertaPropiedad")
public class ServicioOfertaPropiedadImpl implements ServicioOfertaPropiedad {

    private static final Duration VIGENCIA = Duration.ofMinutes(5);
    private static final String OFERTA_NO_DISPONIBLE = "La oferta ya no está disponible.";

    private final ServicioPartida servicioPartida;
    private final ServicioCasillero servicioCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final ServicioParticipacion participacion;
    private final Clock reloj;
    private final Map<String, Oferta> ofertas = new ConcurrentHashMap<>();

    @Autowired
    public ServicioOfertaPropiedadImpl(ServicioPartida servicioPartida, ServicioCasillero servicioCasillero,
                                       ServicioPropiedad servicioPropiedad, ServicioParticipacion participacion) {
        this(servicioPartida, servicioCasillero, servicioPropiedad, participacion, Clock.systemUTC());
    }

    public ServicioOfertaPropiedadImpl(ServicioPartida servicioPartida, ServicioCasillero servicioCasillero,
                                       ServicioPropiedad servicioPropiedad, ServicioParticipacion participacion,
                                       Clock reloj) {
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.participacion = participacion;
        this.reloj = reloj;
    }

    @Override
    public Oferta crearOferta(Partida partida, Integer posicion, Long idVendedor, Long idComprador, Integer precio) {
        if (!participantesValidos(partida, idVendedor, idComprador) || precio == null || precio < 0) {
            throw new OfertaInvalidaException("La oferta no es válida.");
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (!perteneceAlVendedor(casillero, idVendedor)) {
            throw new PropiedadNoDisponibleException("Solo podés ofrecer una propiedad que te pertenece.");
        }
        Jugador vendedor = servicioPartida.obtenerJugadorPorId(idVendedor);
        Oferta oferta = new Oferta(UUID.randomUUID().toString(), partida.getCodigoUnico(), posicion,
                idVendedor, idComprador, precio, reloj.instant().plus(VIGENCIA),
                casillero.getNombre(), vendedor.getApodo());
        ofertas.put(oferta.id(), oferta);
        return oferta;
    }

    @Override
    public List<Oferta> ofertasPendientes(String codigoPartida, Long idComprador) {
        Instant ahora = reloj.instant();
        ofertas.values().removeIf(oferta -> !oferta.expira().isAfter(ahora));
        return ofertas.values().stream()
                .filter(oferta -> oferta.codigoPartida().equals(codigoPartida)
                        && oferta.idComprador().equals(idComprador))
                .sorted(Comparator.comparing(Oferta::expira))
                .toList();
    }

    @Override
    public void rechazar(String codigoPartida, String idOferta, Long idComprador) {
        Oferta oferta = obtenerVigente(codigoPartida, idOferta, idComprador);
        ofertas.remove(oferta.id(), oferta);
    }

    @Override
    public OfertaAceptada aceptar(Partida partida, String idOferta, Long idComprador) {
        Oferta oferta = obtenerVigente(partida.getCodigoUnico(), idOferta, idComprador);
        // Evita que dos respuestas a la misma oferta transfieran la propiedad dos veces.
        synchronized (oferta) {
            if (!ofertas.containsKey(oferta.id())) throw new OfertaNoDisponibleException(OFERTA_NO_DISPONIBLE);
            if (!puedeParticipar(partida, idComprador)) {
                throw new OfertaNoPermitidaException("No podés aceptar ofertas en esta partida.");
            }
            CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, oferta.posicion());
            if (casillero == null || casillero.getPropiedad() == null) {
                throw new PropiedadNoDisponibleException("La propiedad ya no está disponible.");
            }
            servicioPropiedad.transferirPropiedadParaJugadores(casillero.getPropiedad().getId(),
                    oferta.idVendedor(), oferta.idComprador(), oferta.precio());
            ofertas.remove(oferta.id(), oferta);
            return new OfertaAceptada(oferta, casillero);
        }
    }

    private Oferta obtenerVigente(String codigoPartida, String idOferta, Long idComprador) {
        Oferta oferta = idOferta == null ? null : ofertas.get(idOferta);
        if (oferta == null || !oferta.codigoPartida().equals(codigoPartida)
                || !oferta.idComprador().equals(idComprador) || !oferta.expira().isAfter(reloj.instant())) {
            throw new OfertaNoDisponibleException(OFERTA_NO_DISPONIBLE);
        }
        return oferta;
    }

    private boolean participantesValidos(Partida partida, Long idVendedor, Long idComprador) {
        return idVendedor != null && idComprador != null && !idVendedor.equals(idComprador)
                && puedeParticipar(partida, idVendedor) && puedeParticipar(partida, idComprador);
    }

    private boolean puedeParticipar(Partida partida, Long idJugador) {
        Jugador jugador = idJugador == null ? null : servicioPartida.obtenerJugadorPorId(idJugador);
        return participacion.permiteAdministrarPropiedades(partida, jugador, idJugador);
    }

    private boolean perteneceAlVendedor(CasilleroPropiedad casillero, Long idVendedor) {
        return casillero != null && casillero.getPropiedad() != null
                && casillero.getPropiedad().getPropietario() != null
                && Objects.equals(idVendedor, casillero.getPropiedad().getPropietario().getId());
    }
}
