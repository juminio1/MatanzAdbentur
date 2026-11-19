import test from "node:test";
import assert from "node:assert/strict";
import { crearCargaPropiedades } from "../../main/webapp/resources/core/js/propiedades-vista.js";

const pendiente = () => { let resolver; let rechazar; const promesa = new Promise((ok, error) => { resolver = ok; rechazar = error; }); return { promesa, resolver, rechazar }; };

test("una mejora y su notificación simultánea producen una sola lista con el nivel más reciente", async () => {
    const vieja = pendiente(); const nueva = pendiente();
    const consultas = [vieja, nueva];
    const listas = [];
    const cargar = crearCargaPropiedades(() => consultas.shift().promesa, lista => listas.push(lista), assert.fail);
    const primera = cargar();
    await Promise.resolve();
    const segunda = cargar(true);
    vieja.resolver([{ posicion: 4, nivelMejora: 1 }]);
    nueva.resolver([{ posicion: 4, nivelMejora: 2 }]);
    await Promise.all([primera, segunda]);
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
    const primera = cargar();
    await Promise.resolve();
    const segunda = cargar(true);
    vieja.rechazar(new Error("Respuesta antigua"));
    nueva.resolver([]);
    await Promise.all([primera, segunda]);
    assert.deepEqual(listas, [[]]); assert.equal(errores.length, 0);
});

test("solicitudes simultáneas del modal y WebSocket comparten una consulta", async () => {
    let consultas = 0;
    const cargar = crearCargaPropiedades(async () => { consultas++; return []; }, () => {}, assert.fail);
    await Promise.all([cargar(), cargar(true), cargar(true), cargar()]);
    assert.equal(consultas, 1);
});

test("una consulta en curso se comparte si no hay cambios nuevos", async () => {
    const respuesta = pendiente();
    let consultas = 0;
    const cargar = crearCargaPropiedades(() => { consultas++; return respuesta.promesa; }, () => {}, assert.fail);
    const primera = cargar();
    await Promise.resolve();
    const segunda = cargar();
    respuesta.resolver([]);
    await Promise.all([primera, segunda]);
    assert.equal(consultas, 1);
});

test("varios cambios durante una consulta descartan sus datos y requieren un solo refresco adicional", async () => {
    const vieja = pendiente();
    let consultas = 0;
    const listas = [];
    const barrio = [9, 11, 13].map(posicion => ({ posicion, alquiler: 80, propietarioId: 3 }));
    const cargar = crearCargaPropiedades(() => ++consultas === 1 ? vieja.promesa : Promise.resolve(barrio), lista => listas.push(lista), assert.fail);
    const primera = cargar();
    await Promise.resolve();
    const cambios = [cargar(true), cargar(true), cargar(true)];
    vieja.resolver([{ posicion: 11, alquiler: 40, propietarioId: 3 }]);
    await Promise.all([primera, ...cambios]);
    assert.equal(consultas, 2);
    assert.deepEqual(listas, [barrio]);
});

test("completar, mejorar y desarmar el barrio aplica el catálogo completo conservando los DTO", async () => {
    const catalogo = alquiler => [9, 11, 13].map(posicion => ({ posicion, alquiler, propietarioId: 3, grupo: "ISIDRO_CASANOVA", nivelMejora: 0 }));
    const completo = catalogo(80);
    const mejorado = completo.map(p => p.posicion === 9 ? { ...p, alquiler: 120, nivelMejora: 1 } : p);
    const vendido = catalogo(40).map(p => p.posicion === 9 ? { ...p, propietarioId: null } : p);
    const negociado = catalogo(40).map(p => p.posicion === 13 ? { ...p, propietarioId: 2 } : p);
    const respuestas = [completo, mejorado, vendido, negociado];
    const listas = [];
    const cargar = crearCargaPropiedades(async () => respuestas[listas.length], lista => listas.push(lista), assert.fail);
    for (let i = 0; i < respuestas.length; i++) await cargar(true);
    assert.deepEqual(listas, respuestas);
});

test("informa un error vigente y permite volver a consultar", async () => {
    let consultas = 0;
    const errores = [];
    const listas = [];
    const error = new Error("Sin conexión");
    const cargar = crearCargaPropiedades(async () => { if (++consultas === 1) throw error; return []; }, lista => listas.push(lista), e => errores.push(e));
    await cargar(true);
    await cargar();
    assert.deepEqual(errores, [error]);
    assert.deepEqual(listas, [[]]);
});
