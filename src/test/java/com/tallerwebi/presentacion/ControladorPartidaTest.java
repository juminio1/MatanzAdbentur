package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.servicios.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioDado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ControladorPartidaTest {

    private ControladorPartida controladorPartida;
    private ServicioDado servicioDadoMock;

    @BeforeEach
    public void init() {
        servicioDadoMock = mock(ServicioDado.class);
        controladorPartida = new ControladorPartida(servicioDadoMock);
    }

    @Test
    public void tirarDadosDeberiaDevolverResultadoDeTirada() {

        // preparacion
        ResultadoTirada resultadoEsperado = new ResultadoTirada(4, 5);

        when(servicioDadoMock.tirarDados())
                .thenReturn(resultadoEsperado);

        // ejecucion
        ResultadoTirada resultado =
                controladorPartida.tirarDados();

        // validacion
        assertThat(resultado, equalTo(resultadoEsperado));

        verify(servicioDadoMock, times(1))
                .tirarDados();
    }
}
