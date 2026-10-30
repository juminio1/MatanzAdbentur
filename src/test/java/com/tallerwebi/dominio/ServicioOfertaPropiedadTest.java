package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.OfertaInvalidaException;
import com.tallerwebi.dominio.excepcion.OfertaNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.servicios.ServicioOfertaPropiedadImpl;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad.Oferta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioOfertaPropiedad.OfertaAceptada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioOfertaPropiedadTest {

    private static final String CODIGO = "PARTIDA123";
    private static final Integer POSICION = 1;
    private static final Long ID_PROPIEDAD = 50L;

    private RelojManual reloj;
    private ServicioPropiedad servicioPropiedad;
    private ServicioOfertaPropiedadImpl servicio;
    private Partida partida;
    private Jugador vendedor;
    private Jugador comprador;
    private Propiedad propiedad;

    @BeforeEach
    void inicializar() {
        reloj = new RelojManual(Instant.parse("2026-10-27T20:00:00Z"));
        ServicioPartida servicioPartida = mock(ServicioPartida.class);
        ServicioCasillero servicioCasillero = mock(ServicioCasillero.class);
        servicioPropiedad = mock(ServicioPropiedad.class);
        servicio = new ServicioOfertaPropiedadImpl(servicioPartida, servicioCasillero, servicioPropiedad,
                new ServicioParticipacionImpl(), reloj);
        vendedor = crearJugador(1L, "Ana");
        comprador = crearJugador(2L, "Luna");
        partida = new Partida();
        partida.setId(10L);
        partida.setCodigoUnico(CODIGO);
        partida.setJugadores(List.of(vendedor, comprador));
        propiedad = new Propiedad(ID_PROPIEDAD, "Plaza Barrio Marina", 300, 50, vendedor,
                GrupoPropiedades.VILLA_LUZURIAGA);
        CasilleroPropiedad casillero = new CasilleroPropiedad(ID_PROPIEDAD, POSICION, "Plaza Barrio Marina",
                "Villa Luzuriaga", propiedad);
        when(servicioPartida.obtenerJugadorPorId(1L)).thenReturn(vendedor);
        when(servicioPartida.obtenerJugadorPorId(2L)).thenReturn(comprador);
        when(servicioCasillero.buscarCasilleroPropiedad(partida, POSICION)).thenReturn(casillero);
    }

    @Test
    void laOfertaGuardaLosDatosParaMostrarlaAlComprador() {
        Oferta oferta = servicio.crearOferta(partida, POSICION, 1L, 2L, 450);

        assertEquals(CODIGO, oferta.codigoPartida());
        assertEquals("Plaza Barrio Marina", oferta.nombrePropiedad());
        assertEquals("Ana", oferta.apodoVendedor());
        assertEquals(reloj.instant().plus(Duration.ofMinutes(5)), oferta.expira());
        assertEquals(List.of(oferta), servicio.ofertasPendientes(CODIGO, 2L));
    }

    @Test
    void laOfertaVenceALosCincoMinutosYNoSePuedeAceptar() {
        Oferta oferta = servicio.crearOferta(partida, POSICION, 1L, 2L, 450);

        reloj.avanzar(Duration.ofMinutes(5));

        assertTrue(servicio.ofertasPendientes(CODIGO, 2L).isEmpty());
        assertThrows(OfertaNoDisponibleException.class, () -> servicio.aceptar(partida, oferta.id(), 2L));
    }

    @Test
    void unaOfertaAceptadaNoSePuedeAceptarDeNuevo() {
        Oferta oferta = servicio.crearOferta(partida, POSICION, 1L, 2L, 450);

        OfertaAceptada aceptada = servicio.aceptar(partida, oferta.id(), 2L);

        assertEquals(oferta, aceptada.oferta());
        assertEquals(POSICION, aceptada.casillero().getPosicion());
        assertThrows(OfertaNoDisponibleException.class, () -> servicio.aceptar(partida, oferta.id(), 2L));
        verify(servicioPropiedad, times(1)).transferirPropiedadParaJugadores(ID_PROPIEDAD, 1L, 2L, 450);
    }

    @Test
    void soloElCompradorPuedeResponderLaOferta() {
        Oferta oferta = servicio.crearOferta(partida, POSICION, 1L, 2L, 450);

        assertThrows(OfertaNoDisponibleException.class, () -> servicio.rechazar(CODIGO, oferta.id(), 1L));
        assertThrows(OfertaNoDisponibleException.class, () -> servicio.aceptar(partida, oferta.id(), 1L));
        assertEquals(1, servicio.ofertasPendientes(CODIGO, 2L).size());
    }

    @Test
    void rechazaPreciosNegativosONulosYOfertasASiMismo() {
        assertThrows(OfertaInvalidaException.class, () -> servicio.crearOferta(partida, POSICION, 1L, 2L, -1));
        assertThrows(OfertaInvalidaException.class, () -> servicio.crearOferta(partida, POSICION, 1L, 2L, null));
        assertThrows(OfertaInvalidaException.class, () -> servicio.crearOferta(partida, POSICION, 1L, 1L, 450));
    }

    @Test
    void noSePuedeOfrecerUnaPropiedadAjena() {
        propiedad.setPropietario(comprador);

        assertThrows(PropiedadNoDisponibleException.class,
                () -> servicio.crearOferta(partida, POSICION, 1L, 2L, 450));
    }

    private Jugador crearJugador(Long id, String apodo) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(apodo);
        jugador.setDinero(1000);
        return jugador;
    }

    /** Reloj que solo avanza cuando el test lo indica. */
    private static final class RelojManual extends Clock {
        private Instant ahora;

        RelojManual(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
