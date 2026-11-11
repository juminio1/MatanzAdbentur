import test from "node:test";
import assert from "node:assert/strict";
import { validarRespuestaDeuda } from "../../main/webapp/resources/core/js/deuda.js";

test("las alertas de prueba sin cobro ya no son deudas válidas", () => {
    const datos = { tieneDeudaPendiente: true, montoPendiente: 0, motivo: "SALDO_MINIMO_PRUEBA", acreedor: null };
    assert.throws(() => validarRespuestaDeuda(datos));
});
test("un alquiler real mantiene la validación de importe positivo", () => {
    assert.throws(() => validarRespuestaDeuda({ tieneDeudaPendiente: true, montoPendiente: 0, motivo: "ALQUILER", acreedor: null }));
});
test("no acepta alertas de saldo con montos negativos", () => {
    assert.throws(() => validarRespuestaDeuda({ tieneDeudaPendiente: true, montoPendiente: -1, motivo: "SALDO_MINIMO_PRUEBA", acreedor: null }));
});
