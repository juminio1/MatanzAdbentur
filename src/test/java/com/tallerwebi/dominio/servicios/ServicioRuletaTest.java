package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

public class ServicioRuletaTest {

    private Random generadorAleatorioMock;

     @BeforeEach
    public void init() {

        this.generadorAleatorioMock = mock(Random.class);
    
    }

    @Test
    public void deberiaSortearDificultadFacil() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(24);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("FACIL", dificultad);
    }

    @Test
    public void deberiaSortearDificultadMedia() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(60);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("MEDIO", dificultad);
    }


    @Test
    public void deberiaSortearDificultadDificil() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(85);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("DIFICIL", dificultad);
    }

    @Test 
    public void deberiaSortearDificultadFacilCuandoElNumeroEsCero(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(0);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("FACIL", dificultad);
    }

    @Test 
    public void deberiaSortearDificultadFacilCuandoElNumeroEsCuarentaYNueve(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(49);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("FACIL", dificultad);

    }

    @Test 
    public void deberiaSortearDificultadMediaCuandoElNumeroEsCincuenta(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(50);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("MEDIO", dificultad);

    }

    @Test 
    public void deberiaSortearDificultadMediaCuandoElNumeroEsSetentaYNueve(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(79);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("MEDIO", dificultad);

    }

    @Test 
    public void deberiaSortearDificultadDificilCuandoElNumeroEsOchenta(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(80);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("DIFICIL", dificultad);

    }

    @Test 
    public void deberiaSortearDificultadDificilCuandoElNumeroEsNoventaYNueve(){

        when(generadorAleatorioMock.nextInt(100)).thenReturn(99);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String dificultad = servicioRuleta.sortearDificultad();

        assertEquals("DIFICIL", dificultad);

    }

    
    @Test
    public void deberiaUtilizarElGeneradorAleatorioParaSortearDificultad() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(25);

        ServicioRuleta servicioRuleta =
                new ServicioRuletaImpl(generadorAleatorioMock);

        servicioRuleta.sortearDificultad();

        verify(generadorAleatorioMock, times(1)).nextInt(100);
    }


        
    @Test
    public void deberiaSortearUnaNuevaDificultadEnCadaInvocacion() {

        when(generadorAleatorioMock.nextInt(100)).thenReturn(20, 65, 90);

        ServicioRuleta servicioRuleta = new ServicioRuletaImpl(generadorAleatorioMock);

        String primeraDificultad = servicioRuleta.sortearDificultad();
        String segundaDificultad = servicioRuleta.sortearDificultad();
        String terceraDificultad = servicioRuleta.sortearDificultad();

        assertEquals("FACIL", primeraDificultad);
        assertEquals("MEDIO", segundaDificultad);
        assertEquals("DIFICIL", terceraDificultad);

        verify(generadorAleatorioMock, times(3)).nextInt(100);
    }




}

