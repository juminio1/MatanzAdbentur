package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.PropiedadInvalidaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioAlquilerTest {

    private ServicioCasillero casilleros;
    private ServicioAlquilerImpl servicio;
    private Jugador duenio;
    private Jugador otro;

    @BeforeEach
    void setUp() {
        casilleros = mock(ServicioCasillero.class);
        servicio = new ServicioAlquilerImpl(mock(RepositorioPropiedad.class), mock(RepositorioPartida.class),
                casilleros, mock(ServicioEconomia.class), mock(ServicioParticipacion.class));
        duenio = new Jugador();
        duenio.setId(1L);
        otro = new Jugador();
        otro.setId(2L);
    }

    @Test
    void dadaUnaPropiedadMejoradaCuandoCalculoElAlquilerDaComoResultadoElBaseMultiplicado() {
        Propiedad propiedad = propiedad(10L, 20, duenio, GrupoPropiedades.VILLA_LUZURIAGA);
        propiedad.setNivelMejora(2);
        assertEquals(100, servicio.calcularAlquiler(propiedad));
    }

    @Test
    void dadaUnaPropiedadNulaCuandoCalculoElAlquilerDaComoResultadoUnError() {
        assertThrows(PropiedadInvalidaException.class, () -> servicio.calcularAlquiler((Propiedad) null));
    }

    @Test
    void dadoElBarrioCompletoSinMejorasCuandoCalculoElAlquilerDelCasilleroDaComoResultadoElDoble() {
        Propiedad propiedad = propiedad(10L, 20, duenio, GrupoPropiedades.VILLA_LUZURIAGA);
        Propiedad vecina = propiedad(11L, 20, duenio, GrupoPropiedades.VILLA_LUZURIAGA);
        CasilleroPropiedad casillero = casillero(propiedad, 1);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero, casillero(vecina, 2)));

        assertEquals(40, servicio.calcularAlquiler(casillero, propiedad));
    }

    @Test
    void dadoElBarrioDeOtroDuenioCuandoCalculoElAlquilerDelCasilleroDaComoResultadoElBase() {
        Propiedad propiedad = propiedad(10L, 20, duenio, GrupoPropiedades.VILLA_LUZURIAGA);
        Propiedad vecina = propiedad(11L, 20, otro, GrupoPropiedades.VILLA_LUZURIAGA);
        CasilleroPropiedad casillero = casillero(propiedad, 1);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(List.of(casillero, casillero(vecina, 2)));

        assertEquals(20, servicio.calcularAlquiler(casillero, propiedad));
    }

    @Test
    void dadasDosParadasDelMismoDuenioCuandoCalculoElAlquilerDaComoResultadoElDeDosParadas() {
        Propiedad parada = propiedad(20L, 20, duenio, null);
        Propiedad otraParada = propiedad(21L, 20, duenio, null);
        CasilleroPropiedad casillero = casillero(parada, 6);
        when(casilleros.obtenerCasillerosPropiedadDePartida(any()))
                .thenReturn(List.of(casillero, casillero(otraParada, 12)));

        assertEquals(50, servicio.calcularAlquiler(casillero, parada));
    }

    @Test
    void dadaUnaParadaSinDuenioCuandoCalculoElAlquilerDaComoResultadoElBaseDeParada() {
        Propiedad parada = propiedad(20L, 20, null, null);
        assertEquals(20, servicio.calcularAlquiler(casillero(parada, 6), parada));
    }

    private Propiedad propiedad(Long id, int alquiler, Jugador propietario, GrupoPropiedades grupo) {
        return new Propiedad(id, "Propiedad " + id, 150, alquiler, propietario, grupo);
    }

    private CasilleroPropiedad casillero(Propiedad propiedad, int posicion) {
        CasilleroPropiedad casillero = new CasilleroPropiedad(propiedad.getId(), posicion,
                propiedad.getNombre(), "Localidad", propiedad);
        casillero.setPartidaId(100L);
        return casillero;
    }
}
