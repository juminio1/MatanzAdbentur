package com.tallerwebi.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ServicioDadoTest {

    private ServicioDado servicioDadoMock;

    @BeforeEach
    public void init() {
        // Creamos el Mock de la interfaz ServicioDado
        servicioDadoMock = mock(ServicioDado.class);
    }

    @Test
    public void alTirarLosDadosDebeRetornarElResultadoEsperadoMockeado() {
        // 1. PREPARACIÓN (Given / Arrange)
        ResultadoTirada resultadoEsperado = new ResultadoTirada(4, 5);

        // Entrenamos el mock para que cuando se ejecute tirarDados(), devuelva nuestro objeto controlado
        when(servicioDadoMock.tirarDados()).thenReturn(resultadoEsperado);

        // 2. EJECUCIÓN (When / Act)
        ResultadoTirada resultadoObtenido = servicioDadoMock.tirarDados();

        // 3. VERIFICACIÓN (Then / Assert)
        assertNotNull(resultadoObtenido);

        // Verificamos con assertTrue que los dados estén en el rango 1-6
        assertTrue(resultadoObtenido.getDado1() >= 1 && resultadoObtenido.getDado1() <= 6);
        assertTrue(resultadoObtenido.getDado2() >= 1 && resultadoObtenido.getDado2() <= 6);

        // Verificamos los valores exactos mockeados y la suma
        assertEquals(4, resultadoObtenido.getDado1());
        assertEquals(5, resultadoObtenido.getDado2());
        assertEquals(9, resultadoObtenido.getSuma());

        // Verificamos que el método del mock haya sido invocado exactamente 1 vez
        verify(servicioDadoMock, times(1)).tirarDados();
    }
}