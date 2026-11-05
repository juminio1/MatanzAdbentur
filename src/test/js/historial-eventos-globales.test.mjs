import test from "node:test";
import assert from "node:assert/strict";
import { crearHistorialEventosGlobales } from "../../main/webapp/resources/core/js/historial-eventos-globales.js";

const base = cambios => ({ revision: 1, ronda: 5, etapa: 1, activo: true, resuelto: false,
    nombre: "INUNDACION", descripcion: "Retrocede tres casilleros.", tiradas: {},
    jugadores: [{ id: 1, apodo: "Melina" }, { id: 2, apodo: "Pepe" }], ...cambios });
function historial() {
    const filas = [];
    return { filas, recibir: crearHistorialEventosGlobales((texto, id) => filas.push({ texto, id })) };
}

test("registra inicio, tiradas, consecuencia y cierre una sola vez aunque se repitan las consultas", () => {
    const h = historial();
    const estados = [base(), base({ revision: 2, tiradas: { "1:1": 43 } }),
        base({ revision: 3, tiradas: { "1:1": 43, "1:2": 22 }, resuelto: true, seleccionado: 1,
            consecuencia: "Melina: se mueve del casillero 8 al 5. Alquiler: $200." }),
        base({ revision: 4, activo: false, etapa: 0, resuelto: true, seleccionado: 1,
            tiradas: { "1:1": 43, "1:2": 22 },
            consecuencia: "Melina: se mueve del casillero 8 al 5. Alquiler: $200." })];
    estados.forEach(e => { h.recibir(e); h.recibir(structuredClone(e)); });
    assert.equal(h.filas.length, 5);
    assert.match(h.filas[1].texto, /Melina sacó 4 y 3 \(7\)/);
    assert.equal(h.filas[1].id, 1);
    assert.match(h.filas[3].texto, /Alquiler: \$200/);
    assert.equal(h.filas[3].id, 1);
    assert.match(h.filas[4].texto, /Finalizó/);
});

test("recupera todas las etapas de un desempate desde una sola notificación", () => {
    const h = historial();
    h.recibir(base({ revision: 10, etapa: 3, tiradas: { "3:2": 54, "2:1": 33,
        "1:2": 66, "2:2": 33, "1:1": 66 } }));
    const textos = h.filas.map(f => f.texto);
    assert.equal(textos.filter(t => t.includes("vuelven a tirar")).length, 2);
    assert.ok(textos.findIndex(t => t.includes("desempate 1;")) < textos.findIndex(t => t.includes("sacó 3 y 3")));
    assert.match(textos.at(-1), /Pepe sacó 5 y 4 \(9\).*desempate 2/);
});

for (const nombre of ["ROBO", "FIAT_600", "ACCIDENTE_SARMIENTO", "TRAMITES_AFIP", "PIQUETE", "INUNDACION"]) {
    test(`registra el resultado de ${nombre} aunque llegue ya cerrado y sin tiradas`, () => {
        const h = historial();
        const e = base({ nombre, activo: false, etapa: 0, resuelto: true, seleccionado: 2,
            descripcion: "Afecta a [nombre del jugador].", consecuencia: "Pepe: efecto aplicado." });
        h.recibir(e); h.recibir(e);
        assert.equal(h.filas.length, 3);
        assert.match(h.filas[0].texto, /Afecta a Pepe/);
        assert.equal(h.filas[1].id, 2);
        assert.match(h.filas[1].texto, /Pepe: efecto aplicado/);
    });
}

test("ignora respuestas atrasadas y vuelve a registrar el mismo evento en otra ronda", () => {
    const h = historial();
    h.recibir(base({ revision: 10, ronda: 10 }));
    h.recibir(base({ revision: 2, ronda: 5 }));
    assert.equal(h.filas.length, 1);
    h.recibir(base({ revision: 11, ronda: 15 }));
    assert.equal(h.filas.length, 2);
});

test("no registra un resultado incompleto ni duplica la tarjeta incluida en su consecuencia", () => {
    const h = historial();
    h.recibir(base({ resuelto: true, seleccionado: 1, consecuencia: "" }));
    h.recibir(base({ revision: 2, resuelto: true, seleccionado: 1,
        consecuencia: "Melina: recibió «Changa»: ganó $500.", tarjeta: { nombre: "Changa", descripcion: "Ganó $500." } }));
    assert.equal(h.filas.length, 2);
    assert.match(h.filas[1].texto, /ganó \$500/);
});

test("refleja pausa por deuda y su resolución sin repetirlas en cada actualización", () => {
    const h = historial();
    h.recibir(base({ deudaPendiente: true }));
    h.recibir(base({ revision: 2, deudaPendiente: true }));
    h.recibir(base({ revision: 3, deudaPendiente: false }));
    h.recibir(base({ revision: 4, deudaPendiente: false }));
    assert.equal(h.filas.length, 3);
    assert.match(h.filas[1].texto, /en pausa/);
    assert.match(h.filas[2].texto, /deuda fue resuelta/);
});

test("no genera entradas antes del primer evento", () => {
    const h = historial();
    h.recibir(null);
    h.recibir(base({ ronda: 0, nombre: null, activo: false }));
    assert.equal(h.filas.length, 0);
});
