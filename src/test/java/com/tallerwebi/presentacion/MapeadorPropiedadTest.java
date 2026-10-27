package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.presentacion.mapeadores.MapeadorPropiedad;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MapeadorPropiedadTest {
    @Test
    void tantoCatalogoComoPropiedadMuestranElPrecioDeVentaEntregadoPorDominio() {
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        ServicioPropiedad propiedades = mock(ServicioPropiedad.class);
        when(casilleros.obtenerDatosCatalogoPropiedad(1))
                .thenReturn(List.of("Plaza", "Barrio", "VILLA_LUZURIAGA"));
        when(propiedades.calcularPrecioVenta(300)).thenReturn(123);
        when(propiedades.calcularPrecioVenta(900)).thenReturn(321);
        MapeadorPropiedad mapeador = new MapeadorPropiedad(casilleros, propiedades);
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(900);
        propiedad.setPrecioAlquiler(50);
        CasilleroPropiedad casillero = new CasilleroPropiedad();
        casillero.setPosicion(1);

        assertEquals(123, mapeador.desdeCatalogo(1).get("precioVenta"));
        assertEquals(321, mapeador.desdePropiedad(casillero, propiedad).get("precioVenta"));
    }
}
