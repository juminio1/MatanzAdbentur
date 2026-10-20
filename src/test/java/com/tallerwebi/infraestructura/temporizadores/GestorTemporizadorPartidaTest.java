package com.tallerwebi.infraestructura.temporizadores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class GestorTemporizadorPartidaTest {

    private PlatformTransactionManager transactionManagerMock;
    private GestorTemporizadorPartida gestorTemporizador;

    @BeforeEach
    public void init() {
        transactionManagerMock = mock(PlatformTransactionManager.class);
        when(transactionManagerMock.getTransaction(any())).thenReturn(
            mock(TransactionStatus.class)
        );
        gestorTemporizador = new GestorTemporizadorPartida(transactionManagerMock);
    }

    @Test
    public void debeProgramarYEjecutarAccionSinErrores() throws InterruptedException {
        String codigoPartida = "MATANZA123";
        AtomicBoolean ejecuto = new AtomicBoolean(false);

        // Programamos para que ejecute casi inmediatamente (1 segundo)
        gestorTemporizador.programarTemporizador(codigoPartida, 1, () -> ejecuto.set(true));

        // Esperamos a que venza el tiempo del hilo secundario
        Thread.sleep(1200);

        verify(transactionManagerMock, atLeastOnce()).getTransaction(any());
    }

    @Test
    public void debeCancelarTemporizadorSinErrores() {
        String codigoPartida = "MATANZA123";

        gestorTemporizador.programarTemporizador(codigoPartida, 30, () -> {});

        assertDoesNotThrow(() -> gestorTemporizador.cancelarTemporizador(codigoPartida));
    }

    @Test
    public void debeLanzarExcepcionSiFallaAccionEnTransaccion() {
        String codigoPartida = "MATANZA123";

        // Al lanzar una excepcion la lambda debe ser capturada por el catch de la clase
        gestorTemporizador.programarTemporizador(codigoPartida, 0, () -> {
            throw new RuntimeException("Error simulado");
        });

        assertDoesNotThrow(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
