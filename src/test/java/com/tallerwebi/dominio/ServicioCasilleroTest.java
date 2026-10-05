package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.TipoEvento;
import org.junit.jupiter.api.Test;

import static com.tallerwebi.dominio.enums.TipoEvento.RESTA;
import static com.tallerwebi.dominio.enums.TipoEvento.SUMA;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServicioCasilleroTest {

    @Test
    public void usarGettersYSetterDeCasillero(){

        Casillero casillero = new Casillero(12L,2,"as","asdf");

        casillero.setDescripcion("asd");
        casillero.setId(123L);
        casillero.setNombre("si");
        casillero.setPosicion(1);

        assertEquals("asd" , casillero.getDescripcion());
        assertEquals(123L , casillero.getId());
        assertEquals("si" , casillero.getNombre());
        assertEquals(1 , casillero.getPosicion());
    }

    @Test
    public void usarGettersYSetterDeCasilleroEvento(){

        CasilleroEvento casillero = new CasilleroEvento(123L,2,"as","asdf", TipoEvento.SUMA, 2345);

        casillero.setTipoEvento(RESTA);
        casillero.setMonto(123);

        assertEquals(RESTA , casillero.getTipoEvento());
        assertEquals(123 , casillero.getMonto());
    }@Test
    public void usarGettersYSetterDePropiedad(){
        Jugador si = new Jugador();

        si.setId(1);

        Propiedad propiedad = new Propiedad(1L,12,23,si);
        Jugador no = new Jugador();

        no.setId(2);

        propiedad.setId(1L);
        propiedad.setPrecioCompra(123);
        propiedad.setPrecioAlquiler(123);
        propiedad.setPropietario(no);

        assertEquals(1L , propiedad.getId());
        assertEquals(123 , propiedad.getPrecioAlquiler());
        assertEquals(123 , propiedad.getPrecioCompra());
        assertEquals(2 , propiedad.getPropietario().getId());
    }
}
