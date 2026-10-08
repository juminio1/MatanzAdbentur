package com.tallerwebi.dominio;


import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
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
    //repositorio falso para usar en el test
    private RepositorioPropiedad repositorioPropiedadMock;
    private ServicioPropiedad servicioPropiedad;

    @BeforeEach
    public void init() {
        repositorioPropiedadMock = mock(RepositorioPropiedad.class);
        servicioPropiedad = new ServicioPropiedadImpl(repositorioPropiedadMock);
    }

    @Test
    public void jugadorDeberiaComprarUnaPropiedadDisponible() {
        Long idPropiedad = 1L;
        Jugador jugador = new Jugador();
        jugador.setDinero(1000);
        //creo una propiedad
        Propiedad propiedad = new Propiedad(1L, 500, 100, null);//Mockito: cuando se busca la propiedad 1L, devuelve la propiedad creada anteriormente
        when(repositorioPropiedadMock.buscarPropiedadPorId(idPropiedad)).thenReturn(propiedad);
        //llama al metodo comprar propiedad.
        servicioPropiedad.comprarPropiedad(idPropiedad, jugador);

        assertFalse(propiedad.estaDisponible());
        assertEquals(jugador, propiedad.getPropietario());
        assertEquals(500, jugador.getDinero());
    }

}
