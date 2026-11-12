package com.tallerwebi.infraestructura.temporizadores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class GestorTemporizadorPausaTest {
    @Test
    void conservaElTiempoYConsumeLaPausaUnaSolaVez() {
        GestorTemporizadorPartida gestor = new GestorTemporizadorPartida(mock(PlatformTransactionManager.class));
        try {
            assertFalse(gestor.estaPausado("partida"));
            gestor.programarTemporizador("partida", 17, () -> fail("No debe vencer durante la pausa"));
            gestor.pausar("partida");
            assertTrue(gestor.estaPausado("partida"));
            Integer restantes = gestor.obtenerSegundosRestantes("partida");
            assertTrue(restantes > 0 && restantes <= 17);
            gestor.pausar("partida");
            assertEquals(restantes, gestor.obtenerSegundosRestantes("partida"));
            assertEquals(restantes, gestor.consumirPausa("partida"));
            assertFalse(gestor.estaPausado("partida"));
            assertNull(gestor.consumirPausa("partida"));
            assertNull(gestor.obtenerSegundosRestantes("partida"));
        } finally {
            gestor.destroy();
        }
    }

    @Test
    void noIniciaOtroRelojMientrasSeResuelveLaDeudaYConservaElTiempoAlReanudar() {
        GestorTemporizadorPartida gestor = new GestorTemporizadorPartida(mock(PlatformTransactionManager.class));
        try {
            gestor.pausar("sin-reloj");
            assertEquals(60, gestor.obtenerSegundosRestantes("sin-reloj"));
            assertFalse(gestor.programarTemporizadorSiNoExiste("sin-reloj", 1,
                    () -> fail("No debe programar un vencimiento mientras está pausado")));
            Integer restantes = gestor.consumirPausa("sin-reloj");
            assertTrue(gestor.programarTemporizadorSiNoExiste("sin-reloj", restantes,
                    () -> fail("El tiempo reanudado todavía no venció")));
            assertTrue(gestor.obtenerSegundosRestantes("sin-reloj") > 0);
        } finally {
            gestor.destroy();
        }
    }
}
