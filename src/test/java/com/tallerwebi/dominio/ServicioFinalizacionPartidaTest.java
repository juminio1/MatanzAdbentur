package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import java.util.List;
import com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicioFinalizacionPartidaTest {

    @Test
    public void propiedadSinMejorasDeberiaTenerComoValorPatrimonialSuPrecioDeCompra() {
        //crea una propiedad de prueba.
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(0);

        //Crea el servicio de finalizar
        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(500, valorPatrimonial);
    }

    @Test
    public void propiedadConUnaMejoraDeberiaSumarSuCostoAlValorPatrimonial() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(1);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(700, valorPatrimonial);
    }

    @Test
    public void propiedadConDosMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(2);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1000, valorPatrimonial);
    }

    @Test
    public void propiedadConTresMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(3);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1500, valorPatrimonial);
    }

    @Test
    public void propiedadConCuatroMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(4);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(2500, valorPatrimonial);
    }

    @Test
    public void propiedadConCincoMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(5);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(4000, valorPatrimonial);
    }

    @Test
    public void propiedadConOtroPrecioDeCompraDeberiaUsarSuPrecioParaCalcularElPatrimonio() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(800);
        propiedad.setNivelMejora(2);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1300, valorPatrimonial);
    }

    @Test
    public void propiedadConNivelNuloDeberiaTenerComoValorPatrimonialSuPrecioDeCompra() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(null);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(500, valorPatrimonial);
    }

    @Test
    public void propiedadConNivelNegativoDeberiaLanzarIllegalArgumentException() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(-1);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        assertThrows(IllegalArgumentException.class,
                () -> servicioFinalizacionPartida.calcularValorPatrimonial(propiedad));
    }

    @Test
    public void propiedadConNivelSeisDeberiaLanzarIllegalArgumentException() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(6);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        assertThrows(IllegalArgumentException.class,
                () -> servicioFinalizacionPartida.calcularValorPatrimonial(propiedad));
    }

    @Test
    public void jugadorSinPropiedadesNiDeudasDeberiaConservarSuDinero() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1500);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of());
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(1500, patrimonio);
    }

    @Test
    public void jugadorConUnaPropiedadDeberiaSumarlaASuDinero() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(0);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(1500, patrimonio);
    }

    @Test
    public void jugadorConVariasPropiedadesDeberiaAcumularSusMejoras() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(1);
        Propiedad propiedad2 = new Propiedad();
        propiedad2.setPrecioCompra(800);
        propiedad2.setNivelMejora(2);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1, propiedad2));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(3000, patrimonio);
    }

    @Test
    public void jugadorSinDineroDeberiaConservarElValorDeSusPropiedades() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(0);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(0);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(500, patrimonio);
    }

    @Test
    public void jugadorConDeudaDeberiaRestarSoloElMontoPendiente() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of());
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(300);
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(deuda);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(700, patrimonio);
    }

    @Test
    public void jugadorConDeudaMayorASusBienesDeberiaTenerPatrimonioNegativo() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(100);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(3);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(2000);
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(deuda);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(-400, patrimonio);
    }

}
