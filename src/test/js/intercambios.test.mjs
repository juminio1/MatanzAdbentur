import test from "node:test";
import assert from "node:assert/strict";
import { propiedadesParaIntercambiar, tieneIntercambioPendiente } from "../../main/webapp/resources/core/js/intercambios.js";

test("el selector incluye sólo las propiedades del jugador elegido", () => {
    const propiedades = [{ posicion: 4, propietarioId: 1 }, { posicion: 5, propietarioId: "2" }, { posicion: 7, propietarioId: null }];
    assert.deepEqual(propiedadesParaIntercambiar(propiedades, "2"), [propiedades[1]]);
});
test("sin jugador elegido no se ofrecen propiedades libres", () => {
    assert.deepEqual(propiedadesParaIntercambiar([{ posicion: 4, propietarioId: null }], ""), []);
});

test("una propuesta pendiente bloquea la propiedad ofrecida", () => {
    assert.equal(tieneIntercambioPendiente([{ posicionOfrecida: 4, vence: 5000 }], "4", 1000), true);
});
test("la reserva deja libres las otras propiedades", () => {
    assert.equal(tieneIntercambioPendiente([{ posicionOfrecida: 4, vence: 5000 }], 5, 1000), false);
});
test("el bloqueo se libera exactamente al vencer la propuesta", () => {
    assert.equal(tieneIntercambioPendiente([{ posicionOfrecida: 4, vence: 5000 }], 4, 5000), false);
});
