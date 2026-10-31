package com.tallerwebi.dominio.resultados;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.enums.FaseEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EstadoEventoGlobalTest {

    @Test
    public void dadoQueElEstadoUsaUnaFaseEnumCuandoSeSerializaEntoncesConservaLosNombresYLosDatosDelJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Set<String> camposEsperados = Set.of("codigoPartida", "fase", "tipo", "descripcion", "etapa",
                "segundosRestantes", "participantes", "tiradas", "idJugadorElegido", "detalle",
                "posiciones", "saldos", "confirmados");

        for (FaseEventoGlobal fase : FaseEventoGlobal.values()) {
            EstadoEventoGlobal estado = new EstadoEventoGlobal("ABC", fase, TipoEventoGlobal.INUNDACION,
                    "Evento de prueba", 1, 8, List.of(1L, 2L), Map.of(1L, 9, 2L, 4), 1L,
                    "Detalle del resultado", Map.of(1L, 10, 2L, 15), Map.of(1L, 500, 2L, 900), List.of(2L));

            String json = mapper.writeValueAsString(estado);
            JsonNode contenido = mapper.readTree(json);
            Set<String> campos = new HashSet<>();
            contenido.fieldNames().forEachRemaining(campos::add);

            assertEquals(fase.name(), contenido.path("fase").asText());
            assertEquals(camposEsperados, campos);
            assertEquals(estado, mapper.readValue(json, EstadoEventoGlobal.class));
        }
    }

    @Test
    public void dadoQueSeUsaElConstructorOriginalCuandoSeCreaElEstadoEntoncesConservaLosValoresPorDefecto() {
        EstadoEventoGlobal estado = new EstadoEventoGlobal("ABC", FaseEventoGlobal.INICIADO,
                TipoEventoGlobal.PIQUETE, "Piquete", 0, 30, List.of(1L), Map.of(), null);

        assertEquals(FaseEventoGlobal.INICIADO, estado.fase());
        assertEquals(List.of(1L), estado.participantes());
        assertNull(estado.detalle());
        assertEquals(Map.of(), estado.posiciones());
        assertEquals(Map.of(), estado.saldos());
        assertEquals(List.of(), estado.confirmados());
    }

    @Test
    public void dadoQueSeUsaElConstructorSinConfirmacionesCuandoSeCreaElEstadoEntoncesConservaLosMapasDelResultado() {
        EstadoEventoGlobal estado = new EstadoEventoGlobal("ABC", FaseEventoGlobal.FINALIZADO,
                TipoEventoGlobal.ROBO, "Robo", 0, 60, List.of(1L), Map.of(), 1L,
                "Detalle del robo", Map.of(1L, 10), Map.of(1L, 500));

        assertEquals(FaseEventoGlobal.FINALIZADO, estado.fase());
        assertEquals("Detalle del robo", estado.detalle());
        assertEquals(Map.of(1L, 10), estado.posiciones());
        assertEquals(Map.of(1L, 500), estado.saldos());
        assertEquals(List.of(), estado.confirmados());
    }
}
