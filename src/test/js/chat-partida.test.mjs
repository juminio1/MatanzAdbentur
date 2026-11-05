import test from "node:test";
import assert from "node:assert/strict";
import { combinarMensajes } from "../../main/webapp/resources/core/js/chat-partida.js";

test("la respuesta HTTP y el websocket del mismo envío no duplican el mensaje", () => {
    const mensaje = { id: 3, texto: "Hola" };
    assert.deepEqual(combinarMensajes([mensaje], [{ ...mensaje }]), [mensaje]);
});
test("una consulta atrasada se combina sin borrar los mensajes recién recibidos", () => {
    const recibidos = [{ id: 3 }, { id: 4 }];
    assert.deepEqual(combinarMensajes(recibidos, [{ id: 1 }, { id: 2 }, { id: 3 }]).map(m => m.id), [1, 2, 3, 4]);
});
test("limita la vista a los últimos cien mensajes en orden", () => {
    const mensajes = Array.from({ length: 105 }, (_, i) => ({ id: i + 1 }));
    const resultado = combinarMensajes([], mensajes.reverse());
    assert.equal(resultado.length, 100);
    assert.equal(resultado[0].id, 6);
    assert.equal(resultado.at(-1).id, 105);
});
