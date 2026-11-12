import test from "node:test";
import assert from "node:assert/strict";
import { crearCargaPropiedades } from "../../main/webapp/resources/core/js/propiedades-vista.js";

const pendiente = () => { let resolver; let rechazar; const promesa = new Promise((ok, error) => { resolver = ok; rechazar = error; }); return { promesa, resolver, rechazar }; };

test("una mejora y su notificación simultánea producen una sola lista con el nivel más reciente", async () => {
    const vieja = pendiente(); const nueva = pendiente();
    const consultas = [vieja, nueva];
    const listas = [];
    const cargar = crearCargaPropiedades(() => consultas.shift().promesa, lista => listas.push(lista), assert.fail);
    const primera = cargar(); const segunda = cargar();
    nueva.resolver([{ posicion: 4, nivelMejora: 2 }]); await segunda;
    vieja.resolver([{ posicion: 4, nivelMejora: 1 }]); await primera;
    assert.deepEqual(listas, [[{ posicion: 4, nivelMejora: 2 }]]);
});
test("cada casillero aparece una sola vez aunque venga repetido del servidor", async () => {
    let lista;
    await crearCargaPropiedades(async () => [{ posicion: 4 }, { posicion: "4" }, { posicion: 5 }], datos => { lista = datos; }, assert.fail)();
    assert.equal(lista.length, 2);
});
test("un error atrasado no reemplaza el resultado de una consulta nueva", async () => {
    const vieja = pendiente(); const nueva = pendiente(); const consultas = [vieja, nueva];
    const listas = []; const errores = [];
    const cargar = crearCargaPropiedades(() => consultas.shift().promesa, lista => listas.push(lista), error => errores.push(error));
    const primera = cargar(); const segunda = cargar();
    nueva.resolver([]); await segunda;
    vieja.rechazar(new Error("Respuesta antigua")); await primera;
    assert.deepEqual(listas, [[]]); assert.equal(errores.length, 0);
});
