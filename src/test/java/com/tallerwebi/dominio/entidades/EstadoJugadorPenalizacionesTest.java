package com.tallerwebi.dominio.entidades;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EstadoJugadorPenalizacionesTest {

    @Test
    void dadoQueRecibioDosPenalizacionesCuandoConsumeUnaDaComoResultadoQueSiguePenalizado() {
        EstadoJugador estado = new EstadoJugador();
        estado.setPierdeTurno(true);
        estado.setPierdeTurno(true);
        estado.setPierdeTurno(false);
        assertTrue(estado.getPierdeTurno());
        estado.setPierdeTurno(false);
        assertFalse(estado.getPierdeTurno());
    }

    @Test
    void dadoQueTienePenalizacionesCuandoSeIniciaOtraPartidaDaComoResultadoNingunaPendiente() {
        EstadoJugador estado = new EstadoJugador();
        estado.setPierdeTurno(true);
        estado.setPierdeTurno(true);
        estado.limpiarTurnosPenalizados();
        assertFalse(estado.getPierdeTurno());
    }
}
