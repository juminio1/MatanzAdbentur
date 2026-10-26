package com.tallerwebi.dominio;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.infraestructura.RepositorioPartida;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;

import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.PropiedadNoMejorableException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import com.tallerwebi.dominio.excepcion.PropiedadInvalidaException;

import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;

import com.tallerwebi.infraestructura.RepositorioPropiedad;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioPropiedadOperacionesTest {
    private final ServicioEconomia economia = org.mockito.Mockito.spy(new ServicioEconomiaImpl());
    private RepositorioPropiedad repositorio;
    private RepositorioPartida partidas;
    private ServicioCasillero casilleros;
    private ServicioPropiedadImpl servicio;
    private Jugador vendedor;
    private Jugador comprador;
    private Propiedad propiedad;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioPropiedad.class);
        partidas = mock(RepositorioPartida.class);
        casilleros = mock(ServicioCasillero.class);
        servicio = new ServicioPropiedadImpl(repositorio,
                partidas,
                casilleros,
                economia,
                new ServicioTurnoImpl());
        vendedor = jugador(1L, 1000);
        comprador = jugador(2L, 800);
        propiedad = new Propiedad(10L, "Barrio Marina", 300, 50, vendedor,
                GrupoPropiedades.VILLA_LUZURIAGA);
        when(repositorio.buscarPropiedadPorId(10L)).thenReturn(propiedad);
        when(partidas.buscarJugadorPorId(1L)).thenReturn(vendedor);
        when(partidas.buscarJugadorPorId(2L)).thenReturn(comprador);
    }

    @Test
    void venderAlBancoDevuelveLaMitadYEliminaDuenioYMejoras() {
        propiedad.setNivelMejora(3);

        assertEquals(150, servicio.venderPropiedadParaJugador(10L, 1L));

        assertNull(propiedad.getPropietario());
        assertEquals(0, propiedad.getNivelMejora());
        verify(economia).sumarDinero(vendedor, 150);
        verify(partidas).actualizarJugador(vendedor);
        verify(repositorio).guardarPropiedad(propiedad);
    }

    @Test
    void ventaAjenaOSinPropietarioNoModificaPropiedadNiSaldos() {
        propiedad.setNivelMejora(3);
        var ajena = assertThrows(PropiedadNoDisponibleException.class,
                () -> servicio.venderPropiedadParaJugador(10L, 2L));
        assertEquals("Solo puedes vender una propiedad que te pertenece.", ajena.getMessage());
        assertSame(vendedor, propiedad.getPropietario());
        propiedad.setPropietario(null);
        assertThrows(PropiedadNoDisponibleException.class,
                () -> servicio.venderPropiedadParaJugador(10L, 1L));
        assertEquals(3, propiedad.getNivelMejora());
        verify(economia, never()).sumarDinero(any(), any());
        verify(partidas, never()).actualizarJugador(any());
        verify(repositorio, never()).guardarPropiedad(any());
    }

    @Test
    void precioDeVentaInvalidoNoEliminaDuenioNiMejoras() {
        propiedad.setNivelMejora(3);
        for (Integer precio : new Integer[]{null, -1}) {
            propiedad.setPrecioCompra(precio);
            var error = assertThrows(PropiedadInvalidaException.class,
                    () -> servicio.venderPropiedadParaJugador(10L, 1L));
            assertEquals("El precio de compra de la propiedad no es válido.", error.getMessage());
            assertSame(vendedor, propiedad.getPropietario());
            assertEquals(3, propiedad.getNivelMejora());
        }
        verify(economia, never()).sumarDinero(any(), any());
        verify(repositorio, never()).guardarPropiedad(any());
    }

    @Test
    void ventaDePrecioImparConservaLaDivisionEntera() {
        propiedad.setPrecioCompra(301);
        assertEquals(150, servicio.venderPropiedadParaJugador(10L, 1L));
        verify(economia).sumarDinero(vendedor, 150);
    }

    @Test
    void transferirCambiaDuenioConservaMejorasYCobraElPrecioAcordado() {
        propiedad.setNivelMejora(2);

        servicio.transferirPropiedadParaJugadores(10L, 1L, 2L, 450);

        assertSame(comprador, propiedad.getPropietario());
        assertEquals(2, propiedad.getNivelMejora());
        verify(economia).restarDinero(comprador, 450);
        verify(economia).sumarDinero(vendedor, 450);
        verify(partidas).actualizarJugador(comprador);
        verify(partidas).actualizarJugador(vendedor);
        verify(repositorio).guardarPropiedad(propiedad);
    }

    @Test
    void transferenciaSinFondosNoCambiaElDuenioNiLosSaldos() {
        assertThrows(DineroInsuficienteException.class,
                () -> servicio.transferirPropiedadParaJugadores(10L, 1L, 2L, 900));

        assertSame(vendedor, propiedad.getPropietario());
        verify(economia, never()).restarDinero(any(), anyInt());
        verify(economia, never()).sumarDinero(any(), anyInt());
        verify(repositorio, never()).guardarPropiedad(any());
    }

    @Test
    void alquilerDeParadasCuentaSoloLasDelDuenioEnLaMismaPartida() {
        List<CasilleroPropiedad> paradas = new ArrayList<>();
        int[] posiciones = {6, 12, 17, 24};
        int[] alquileres = {20, 50, 100, 200};
        for (int indice = 0; indice < posiciones.length; indice++) {
            Propiedad parada = new Propiedad(20L + indice, "Parada", 300, 50, vendedor, null);
            parada.setNivelMejora(5);
            paradas.add(casillero(parada, posiciones[indice], 100L));
            CasilleroPropiedad otraPartida = casillero(propiedad, 6, 200L);
            Propiedad ajena = new Propiedad(30L, "Colectivo", 300, 50, comprador, null);
            List<CasilleroPropiedad> tablero = new ArrayList<>(paradas);
            tablero.add(otraPartida);
            tablero.add(casillero(ajena, 12, 100L));
            when(casilleros.obtenerTodosLosCasilleros()).thenReturn(new ArrayList<>(tablero));

            assertEquals(alquileres[indice], servicio.calcularAlquiler(paradas.get(0),
                    paradas.get(0).getPropiedad()));
        }
    }

    @Test
    void mejorarLocalidadCompletaCobraSoloLaMejoraElegida() {
        Propiedad vecina = new Propiedad(11L, "Stella Maris", 300, 50, vendedor,
                GrupoPropiedades.VILLA_LUZURIAGA);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(List.of(
                casillero(propiedad, 1, 100L), casillero(vecina, 2, 100L)));

        servicio.mejorarPropiedad(10L, vendedor);

        assertEquals(1, propiedad.getNivelMejora());
        assertEquals(0, vecina.getNivelMejora());
        assertEquals(200, servicio.calcularAlquiler(propiedad));
        verify(economia).restarDinero(vendedor, 200);
        verify(partidas).actualizarJugador(vendedor);
    }

    @Test
    void propiedadDeOtraPartidaNoCompletaLaLocalidad() {
        Propiedad vecina = new Propiedad(11L, "Stella Maris", 300, 50, vendedor,
                GrupoPropiedades.VILLA_LUZURIAGA);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(List.of(
                casillero(propiedad, 1, 100L), casillero(vecina, 2, 200L)));

        assertThrows(PropiedadNoMejorableException.class,
                () -> servicio.mejorarPropiedad(10L, vendedor));

        assertEquals(0, propiedad.getNivelMejora());
        verify(economia, never()).restarDinero(any(), anyInt());
    }

    private Jugador jugador(Long id, int dinero) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setDinero(dinero);
        return jugador;
    }

    private CasilleroPropiedad casillero(Propiedad propiedad, int posicion, Long partidaId) {
        CasilleroPropiedad casillero = new CasilleroPropiedad(propiedad.getId(), posicion,
                propiedad.getNombre(), "Localidad", propiedad);
        casillero.setPartidaId(partidaId);
        return casillero;
    }
}
