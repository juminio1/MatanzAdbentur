package com.tallerwebi.dominio.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventoSalaActualizadaTest {

    @Test
    void conservaLaInstantaneaAunqueCambienLasColeccionesOriginales() {
        List<String> usuarios = new ArrayList<>(List.of("Ana"));
        List<String> fichas = new ArrayList<>(List.of("ROJO"));
        Map<String, String> asignadas = new HashMap<>(Map.of("Ana", "ROJO"));
        EventoSalaActualizada evento = new EventoSalaActualizada("SALA", usuarios, fichas, asignadas, "Ana");

        usuarios.clear();
        fichas.clear();
        asignadas.clear();

        assertEquals(List.of("Ana"), evento.usernames());
        assertEquals(List.of("ROJO"), evento.fichasOcupadas());
        assertEquals(Map.of("Ana", "ROJO"), evento.fichasAsignadas());
        assertThrows(UnsupportedOperationException.class, () -> evento.usernames().clear());
        assertThrows(UnsupportedOperationException.class, () -> evento.fichasOcupadas().clear());
        assertThrows(UnsupportedOperationException.class, () -> evento.fichasAsignadas().clear());
    }
}
