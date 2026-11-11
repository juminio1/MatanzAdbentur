package com.tallerwebi.dominio.entidades;

import static org.junit.jupiter.api.Assertions.*;

import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EstadoEventoGlobalTest {
    @Test
    void dadoQueHayUnResultadoAnteriorCuandoIniciaOtroEventoDaComoResultadoElEstadoLimpioParaLaNuevaRonda() {
        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal();
        estado.iniciar(new EventoGlobal(), 5, Set.of(1L, 2L));
        estado.registrar(1L, 6, 4);
        estado.resolver(1L, "Resultado anterior");
        estado.guardarTarjeta(new EventoCasillero());
        estado.ofrecerCompra(5);
        estado.confirmar(1L);
        estado.cerrar();
        Long revision = estado.getRevision();
        EventoGlobal siguiente = new EventoGlobal();

        // Ejecución
        estado.iniciar(siguiente, 10, Set.of(2L, 3L));

        // Verificación
        assertSame(siguiente, estado.getEvento());
        assertTrue(estado.estaActivo());
        assertFalse(estado.estaResuelto());
        assertEquals(10, estado.getUltimaRonda());
        assertEquals(1, estado.getEtapa());
        assertEquals(Set.of(2L, 3L), estado.getHabilitados());
        assertTrue(estado.getTiradas().isEmpty());
        assertTrue(estado.getConfirmados().isEmpty());
        assertNull(estado.getTarjeta());
        assertNull(estado.getPosicionCompra());
        assertEquals("", estado.getConsecuencia());
        assertEquals(revision + 1, estado.getRevision());
    }

    @Test
    void dadoQueUnJugadorSeRetiraCuandoSeExcluyenInactivosDaComoResultadoSuExclusionConElHistorialConservado() {
        // Preparación
        EstadoEventoGlobal estado = new EstadoEventoGlobal();
        estado.iniciar(new EventoGlobal(), 5, Set.of(1L, 2L, 3L));
        estado.registrar(1L, 6, 4);
        estado.registrar(2L, 2, 3);
        estado.confirmar(1L);
        estado.confirmar(2L);
        Long revision = estado.getRevision();

        // Ejecución
        estado.excluirInactivos(Set.of(2L, 3L));
        estado.excluirInactivos(Set.of(2L, 3L));

        // Verificación
        assertEquals(Set.of(2L, 3L), estado.getHabilitados());
        assertEquals(Set.of(2L), estado.getConfirmados());
        assertEquals(Map.of("1:1", 64, "1:2", 23), estado.getTiradas());
        assertEquals(revision + 1, estado.getRevision());
    }
}
