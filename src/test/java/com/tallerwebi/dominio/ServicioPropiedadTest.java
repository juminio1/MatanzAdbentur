package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPropiedad;
import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicioPropiedadTest {
    private RepositorioPropiedad repositorioPropiedadMock;
    private ServicioPartida servicioPartidaMock;
    private ServicioPropiedad servicioPropiedad;

    @BeforeEach
    public void init() {
        repositorioPropiedadMock = mock(RepositorioPropiedad.class);
        servicioPartidaMock = mock(ServicioPartida.class);
        servicioPropiedad = new ServicioPropiedadImpl(repositorioPropiedadMock, servicioPartidaMock);
    }

    @Test
    public void jugadorDeberiaComprarUnaPropiedadDisponible() {
        Long idPropiedad = 1L;
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        Propiedad propiedad = new Propiedad(1L, 500, 100, null);
        when(repositorioPropiedadMock.buscarPropiedadPorId(idPropiedad)).thenReturn(propiedad);
        // Simula el descuento del dinero al comprar.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQuePaga = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQuePaga.setDinero(jugadorQuePaga.getDinero() - monto);
            return null;
        }).when(servicioPartidaMock).restarDinero(jugador, 500);
        servicioPropiedad.comprarPropiedad(idPropiedad, jugador);
        assertFalse(propiedad.estaDisponible());
        assertEquals(jugador, propiedad.getPropietario());
        assertEquals(500, jugador.getDinero());
    }

    @Test
    public void jugadorDeberiaPagarAlquilerAlPropietario() {
        Jugador visitante = new Jugador();
        visitante.setId(1);
        visitante.setDinero(1000);
        Jugador propietario = new Jugador();
        propietario.setId(2);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L, 500, 100, propietario);
        // Simula el descuento al visitante.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQuePaga = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQuePaga.setDinero(jugadorQuePaga.getDinero() - monto);
            return null;
        }).when(servicioPartidaMock).restarDinero(visitante, 100);
        // Simula el ingreso del alquiler al propietario.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQueCobra = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQueCobra.setDinero(jugadorQueCobra.getDinero() + monto);
            return null;
        }).when(servicioPartidaMock).sumarDinero(propietario, 100);
        servicioPropiedad.pagarAlquiler(propiedad, visitante);
        assertEquals(900, visitante.getDinero());
        assertEquals(600, propietario.getDinero());
    }

    @Test
    public void propietarioNoDeberiaPagarAlquilerPorSuPropiedad() {
        Jugador propietario = new Jugador();
        propietario.setId(1);
        propietario.setDinero(1000);
        Propiedad propiedad = new Propiedad(1L, 500, 100, propietario);
        servicioPropiedad.pagarAlquiler(propiedad, propietario);
        assertEquals(1000, propietario.getDinero());
    }
}
