package com.tallerwebi.presentacion.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class TurnoDTOTest {

    @Test
    public void dadoQueSeCreaUnTurnoDTOVacioCuandoSeConsultanSusDatosDaComoResultadoValoresNulos() {
        TurnoDTO turnoDTO = new TurnoDTO();

        assertNull(turnoDTO.getCodigoPartida());
        assertNull(turnoDTO.getIdJugadorActual());
        assertNull(turnoDTO.getApodoJugadorActual());
        assertNull(turnoDTO.getOrden());
        assertNull(turnoDTO.getMensaje());
        assertNull(turnoDTO.getRondaActual());
    }

    @Test
    public void dadoQueSeCreaUnTurnoDTOConDatosCuandoSeConsultanDaComoResultadoLosValoresDelConstructor() {
        TurnoDTO turnoDTO = new TurnoDTO(
                "MATANZA123", 2, "Kevin", 1, "Es el turno de Kevin"
        );

        assertEquals("MATANZA123", turnoDTO.getCodigoPartida());
        assertEquals(2, turnoDTO.getIdJugadorActual());
        assertEquals("Kevin", turnoDTO.getApodoJugadorActual());
        assertEquals(1, turnoDTO.getOrden());
        assertEquals("Es el turno de Kevin", turnoDTO.getMensaje());
        assertNull(turnoDTO.getRondaActual());
    }

    @Test
    public void dadoQueExisteUnTurnoDTOCuandoSeAsignanSusDatosDaComoResultadoLosValoresActualizados() {
        TurnoDTO turnoDTO = new TurnoDTO();

        turnoDTO.setCodigoPartida("PARTIDA456");
        turnoDTO.setIdJugadorActual(4);
        turnoDTO.setApodoJugadorActual("Messi");
        turnoDTO.setOrden(3);
        turnoDTO.setMensaje("Es el turno de Messi");
        turnoDTO.setRondaActual(2);

        assertEquals("PARTIDA456", turnoDTO.getCodigoPartida());
        assertEquals(4, turnoDTO.getIdJugadorActual());
        assertEquals("Messi", turnoDTO.getApodoJugadorActual());
        assertEquals(3, turnoDTO.getOrden());
        assertEquals("Es el turno de Messi", turnoDTO.getMensaje());
        assertEquals(2, turnoDTO.getRondaActual());
    }
}
