
package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

public class ServicioRuletaTest {

    private Random generadorAleatorioMock;
    private ServicioRuleta servicioRuleta;


    @BeforeEach
    public void init() {
        this.generadorAleatorioMock = mock(Random.class);
        this.servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);
    }

    @Test
    public void deberiaSortearDificultadFacil() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(24);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.FACIL, dificultad);
    }

    @Test
    public void deberiaSortearDificultadMedia() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(60);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.MEDIO, dificultad);
    }

    @Test
    public void deberiaSortearDificultadDificil() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(85);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.DIFICIL, dificultad);
    }

    @Test
    public void deberiaSortearDificultadFacilCuandoElNumeroEsCero() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(0);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.FACIL, dificultad);
    }

    @Test
    public void deberiaSortearDificultadFacilCuandoElNumeroEsCuarentaYNueve() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(49);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.FACIL, dificultad);
    }

    @Test
    public void deberiaSortearDificultadMediaCuandoElNumeroEsCincuenta() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(50);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.MEDIO, dificultad);
    }

    @Test
    public void deberiaSortearDificultadMediaCuandoElNumeroEsSetentaYNueve() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(79);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.MEDIO, dificultad);
    }

    @Test
    public void deberiaSortearDificultadDificilCuandoElNumeroEsOchenta() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(80);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.DIFICIL, dificultad);
    }

    @Test
    public void deberiaSortearDificultadDificilCuandoElNumeroEsNoventaYNueve() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(99);

        DificultadRuleta dificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.DIFICIL, dificultad);
    }

    @Test
    public void deberiaUtilizarElGeneradorAleatorioParaSortearDificultad() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(25);

        servicioRuleta.sortearDificultad();

        verify(generadorAleatorioMock, times(1)).nextInt(100);
    }

    @Test
    public void deberiaSortearUnaNuevaDificultadEnCadaInvocacion() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(20, 65, 90);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        DificultadRuleta primeraDificultad = servicioRuleta.sortearDificultad();

        DificultadRuleta segundaDificultad = servicioRuleta.sortearDificultad();

        DificultadRuleta terceraDificultad = servicioRuleta.sortearDificultad();

        assertEquals(DificultadRuleta.FACIL, primeraDificultad);
        assertEquals(DificultadRuleta.MEDIO, segundaDificultad);
        assertEquals(DificultadRuleta.DIFICIL, terceraDificultad);

        verify(generadorAleatorioMock, times(3)).nextInt(100);
    }
}
