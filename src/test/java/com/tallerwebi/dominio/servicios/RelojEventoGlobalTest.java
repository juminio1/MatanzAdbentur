package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RelojEventoGlobalTest {

    @Test
    public void dadoQueSeReprogramoCuandoLlegaElVencimientoAnteriorEntoncesSoloReconoceElActual() {
        RelojEventoGlobal reloj = new RelojEventoGlobal();
        Long anterior = reloj.iniciar("ABC", 8);
        Long actual = reloj.iniciar("ABC", 30);

        assertFalse(reloj.correspondeAlVencimiento("ABC", anterior));
        assertTrue(reloj.correspondeAlVencimiento("ABC", actual));
        reloj.limpiar("ABC");
        assertFalse(reloj.correspondeAlVencimiento("ABC", actual));
        assertFalse(reloj.correspondeAlVencimiento("ABC", null));
    }

    @Test
    public void dadoQueSeProgramaElRelojConTreintaSegundosCuandoSeConsultaYDaTiempoDisponible() {
        RelojEventoGlobal reloj = new RelojEventoGlobal();
        reloj.iniciar("ABC", 30);

        Integer segundos = reloj.obtenerSegundosRestantes("ABC");

        assertTrue(segundos > 0 && segundos <= 30);
        assertFalse(reloj.vencio("ABC"));
    }

    @Test
    public void dadoQueElRelojLlegoACeroCuandoSeCompruebaYDaVencido() {
        RelojEventoGlobal reloj = new RelojEventoGlobal();
        reloj.iniciar("ABC", 0);

        assertTrue(reloj.vencio("ABC"));
        assertEquals(0, reloj.obtenerSegundosRestantes("ABC"));
    }

    @Test
    public void dadoQueSeLimpioElRelojCuandoSeConsultaYDaSinTiempoRegistrado() {
        RelojEventoGlobal reloj = new RelojEventoGlobal();
        reloj.iniciar("ABC", 30);
        reloj.limpiar("ABC");

        assertFalse(reloj.vencio("ABC"));
        assertNull(reloj.obtenerSegundosRestantes("ABC"));
    }
}
