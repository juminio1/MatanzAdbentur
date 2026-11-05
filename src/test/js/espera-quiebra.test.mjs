import test from "node:test";
import assert from "node:assert/strict";
import { sincronizarEsperaQuiebra } from "../../main/webapp/resources/core/js/deuda.js";

function cartel() {
    return { open: false, aperturas: 0, cierres: 0,
        showModal() { this.open = true; this.aperturas++; },
        close() { this.open = false; this.cierres++; } };
}
const estado = (deuda, estado = "actualizado", pausado = true, finalizada = false) =>
    ({ pausado, finalizada, estadoDeuda: { estado, deuda } });

test("dos jugadores: el deudor puede resolver y solo el otro espera", () => {
    const deudor = cartel();
    const otro = cartel();
    sincronizarEsperaQuiebra(deudor, estado(null, "desconocido"));
    sincronizarEsperaQuiebra(otro, estado(null, "desconocido"));
    assert.equal(deudor.open, false);
    assert.equal(otro.open, false);
    sincronizarEsperaQuiebra(deudor, estado({ montoPendiente: 100, motivo: "ALQUILER" }));
    sincronizarEsperaQuiebra(otro, estado(null));
    assert.equal(deudor.aperturas, 0);
    assert.equal(otro.open, true);
});

test("una multa pendiente tambien es deuda propia", () => {
    const aviso = cartel();
    sincronizarEsperaQuiebra(aviso, estado({ montoPendiente: 100, motivo: "EVENTO_RESTA" }));
    assert.equal(aviso.open, false);
});

test("si aparece una deuda propia cierra la espera antes de resolver", () => {
    const aviso = cartel();
    sincronizarEsperaQuiebra(aviso, estado(null));
    sincronizarEsperaQuiebra(aviso, estado(null, "desconocido"));
    assert.equal(aviso.open, false);
    assert.equal(aviso.cierres, 1);
    sincronizarEsperaQuiebra(aviso, estado({ montoPendiente: 100 }));
    assert.equal(aviso.aperturas, 1);
});

test("una consulta fallida no abre una espera sobre informacion vieja", () => {
    const aviso = cartel();
    sincronizarEsperaQuiebra(aviso, estado(null, "error"));
    assert.equal(aviso.open, false);
});

test("la espera termina al reanudar la partida o finalizarla", () => {
    const aviso = cartel();
    sincronizarEsperaQuiebra(aviso, estado(null));
    sincronizarEsperaQuiebra(aviso, estado(null));
    assert.equal(aviso.aperturas, 1);
    sincronizarEsperaQuiebra(aviso, estado(null, "actualizado", false));
    assert.equal(aviso.open, false);
    sincronizarEsperaQuiebra(aviso, estado(null));
    sincronizarEsperaQuiebra(aviso, estado(null, "actualizado", true, true));
    assert.equal(aviso.open, false);
});
