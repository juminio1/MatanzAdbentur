import test from "node:test";
import assert from "node:assert/strict";
import { seleccionarTurnoMasReciente } from "../../main/webapp/resources/core/js/eventos-globales.js";

test("conserva el aviso de turno recibido mientras el evento está abierto", () => {
    const turno = { idJugadorActual: 2, rondaActual: 5, turnosFinalizadosEnRonda: 1 };
    assert.equal(seleccionarTurnoMasReciente(null, turno), turno);
});
test("una respuesta atrasada no borra el turno nuevo", () => {
    const nuevo = { idJugadorActual: 3, rondaActual: 6, turnosFinalizadosEnRonda: 0 };
    const viejo = { idJugadorActual: 2, rondaActual: 5, turnosFinalizadosEnRonda: 3 };
    assert.equal(seleccionarTurnoMasReciente(nuevo, viejo), nuevo);
});
test("ordena los turnos dentro de la misma ronda", () => {
    const nuevo = { idJugadorActual: 3, rondaActual: 5, turnosFinalizadosEnRonda: 2 };
    const viejo = { idJugadorActual: 2, rondaActual: 5, turnosFinalizadosEnRonda: 1 };
    assert.equal(seleccionarTurnoMasReciente(nuevo, viejo), nuevo);
});
