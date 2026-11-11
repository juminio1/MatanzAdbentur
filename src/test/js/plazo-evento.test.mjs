import test from "node:test";
import assert from "node:assert/strict";
import { esperarContinuacion } from "../../main/webapp/resources/core/js/plazo-evento.js";

function preparar() {
    const boton = new EventTarget();
    const eventos = new EventTarget();
    let ahora = 0;
    let vencer;
    const resultado = esperarContinuacion({ boton, eventos, vencimiento: 1000,
        ahora: () => ahora, programar: callback => { vencer = callback; return 1; }, cancelar: () => {} });
    return { boton, eventos, resultado, avanzar: ms => { ahora = ms; }, vencer: () => vencer() };
}

test("continuar dentro del plazo permite resolver la compra", async () => {
    const caso = preparar();
    caso.boton.dispatchEvent(new Event("click"));
    assert.equal(await caso.resultado, true);
});
test("el plazo vence aunque el reloj de pantalla esté detenido", async () => {
    const caso = preparar();
    caso.avanzar(1000);
    caso.vencer();
    assert.equal(await caso.resultado, false);
});
test("un cambio de turno cierra la espera sin permitir compra", async () => {
    const caso = preparar();
    caso.eventos.dispatchEvent(new Event("fin-plazo-turno"));
    assert.equal(await caso.resultado, false);
});
test("continuar cuando ya venció tampoco permite comprar", async () => {
    const caso = preparar();
    caso.avanzar(1000);
    caso.boton.dispatchEvent(new Event("click"));
    assert.equal(await caso.resultado, false);
});
