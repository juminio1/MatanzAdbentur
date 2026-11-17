package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class MapeadorPropiedadTest {
    @Test
    void tantoCatalogoComoPropiedadMuestranElPrecioDeVentaEntregadoPorDominio() {
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        ServicioPropiedad propiedades = mock(ServicioPropiedad.class);
        when(casilleros.obtenerDatosCatalogoPropiedad(1))
                .thenReturn(List.of("Plaza", "Barrio", "VILLA_LUZURIAGA"));
        when(propiedades.calcularPrecioVenta(150)).thenReturn(123);
        MapeadorPropiedad mapeador = new MapeadorPropiedad(casilleros, propiedades);
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(900);
        propiedad.setPrecioAlquiler(50);
        when(propiedades.calcularPrecioVenta(propiedad)).thenReturn(321);
        CasilleroPropiedad casillero = new CasilleroPropiedad();
        casillero.setPosicion(1);

        assertEquals(123, mapeador.desdeCatalogo(1).get("precioVenta"));
        assertEquals(321, mapeador.desdePropiedad(casillero, propiedad).get("precioVenta"));
    }

    @Test
    void dadoUnaPropiedadMejorableCuandoLaMapeoDaComoResultadoElCostoDeLaProximaMejora() {
        MapeadorPropiedad mapeador = mapeadorConCatalogo(1, List.of("Plaza", "Barrio", "VILLA_LUZURIAGA"));
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(2);

        var dto = mapeador.desdePropiedad(casilleroEn(1), propiedad);

        assertEquals(250, dto.get("costoMejora"));
        assertEquals(5, dto.get("nivelMaximoMejora"));
        assertEquals(75, mapeador.desdeCatalogo(1).get("costoMejora"));
    }

    @Test
    void dadoUnaPropiedadEnElNivelMaximoCuandoLaMapeoDaComoResultadoSinCostoDeMejora() {
        MapeadorPropiedad mapeador = mapeadorConCatalogo(1, List.of("Plaza", "Barrio", "VILLA_LUZURIAGA"));
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(5);

        assertNull(mapeador.desdePropiedad(casilleroEn(1), propiedad).get("costoMejora"));
    }

    @Test
    void dadoUnaParadaCuandoLaMapeoDaComoResultadoSinCostoDeMejora() {
        MapeadorPropiedad mapeador = mapeadorConCatalogo(6, List.of("Colectivo 242", "Parada de bondi", ""));
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(250);
        propiedad.setNivelMejora(0);

        assertNull(mapeador.desdePropiedad(casilleroEn(6), propiedad).get("costoMejora"));
        assertNull(mapeador.desdeCatalogo(6).get("costoMejora"));
    }

    private static MapeadorPropiedad mapeadorConCatalogo(Integer posicion, List<String> datos) {
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        when(casilleros.obtenerDatosCatalogoPropiedad(posicion)).thenReturn(datos);
        return new MapeadorPropiedad(casilleros, mock(ServicioPropiedad.class));
    }

    private static CasilleroPropiedad casilleroEn(Integer posicion) {
        CasilleroPropiedad casillero = new CasilleroPropiedad();
        casillero.setPosicion(posicion);
        return casillero;
    }
}
