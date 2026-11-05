const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const codigo = fs.readFileSync(path.join(__dirname, "../../main/webapp/resources/core/js/partida.js"), "utf8");
const popup = codigo.slice(codigo.indexOf("async function mostrarPopupEvento("), codigo.indexOf("function registrarPremioPorInicio("));
const tarjeta = codigo.slice(codigo.indexOf("async function mostrarTarjetaGlobal("), codigo.indexOf("// Agregar este helper"));

function preparar(almacen = new Map(), usuario = 1) {
    const elementos = new Map();
    for (const id of ["aviso-global", "aviso-evento", "aviso-evento-nombre", "aviso-evento-descripcion", "btn-continuar-evento"]) {
        const clases = new Set();
        elementos.set(id, {
            textContent: "", atributos: {},
            classList: { add: valor => clases.add(valor), remove: valor => clases.delete(valor), contains: valor => clases.has(valor) },
            setAttribute(nombre, valor) { this.atributos[nombre] = valor; },
            addEventListener(nombre, callback) { this.click = callback; },
            focus() {},
        });
    }
    const estado = {
        activo: true, resuelto: true, ronda: 5, seleccionado: 1,
        tarjeta: { nombre: "Changa", descripcion: "Ganaste $500" },
    };
    const confirmaciones = [];
    const contexto = vm.createContext({
        window: { usuarioSesionId: usuario }, document: { getElementById: id => elementos.get(id) },
        estadoEventoGlobalCliente: estado, peticionEventoGlobal: false, partidaFinalizada: false,
        codigoPartida: "PRUEBA", tarjetasGlobalesMostradas: new Set(), console,
        sessionStorage: { getItem: clave => almacen.get(clave), setItem: (clave, valor) => almacen.set(clave, valor) },
        debeEsperarPresentacionGlobal: () => elementos.get("aviso-evento").classList.contains("aviso-evento--visible"),
        mostrarEstadoGlobal: actual => {
            if (actual.activo) elementos.get("aviso-global").classList.add("aviso-evento--visible");
        },
        actualizarAvisoDeuda() {}, enviarAccionEventoGlobal: accion => confirmaciones.push(accion),
    });
    vm.runInContext(popup + tarjeta, contexto);
    elementos.get("aviso-global").classList.add("aviso-evento--visible");
    return { contexto, elementos, estado, confirmaciones, almacen };
}

test("dadoQueRecibioUnaTarjetaCuandoContinuaDaComoResultadoPopupSinSuperposicionAntesDeConfirmar", async () => {
    const caso = preparar();
    const pendiente = caso.contexto.continuarEventoGlobal();
    assert.equal(caso.elementos.get("aviso-global").classList.contains("aviso-evento--visible"), false);
    assert.equal(caso.elementos.get("aviso-evento").classList.contains("aviso-evento--visible"), true);
    assert.equal(caso.elementos.get("aviso-evento-nombre").textContent, "Changa");
    assert.equal(caso.elementos.get("aviso-evento-descripcion").textContent, "Ganaste $500");
    assert.equal(caso.estado.activo, true);
    assert.deepEqual(caso.confirmaciones, []);
    await caso.contexto.continuarEventoGlobal();
    await caso.contexto.mostrarTarjetaGlobal(caso.estado);
    assert.deepEqual(caso.confirmaciones, []);
    caso.elementos.get("btn-continuar-evento").click();
    await pendiente;
    assert.deepEqual(caso.confirmaciones, ["continuar"]);
    assert.equal(caso.elementos.get("aviso-evento").classList.contains("aviso-evento--visible"), false);
});

test("dadoQueOtroJugadorRecibioLaTarjetaCuandoConfirmaDaComoResultadoNingunPopupAjeno", async () => {
    const caso = preparar(new Map(), 2);
    await caso.contexto.continuarEventoGlobal();
    assert.equal(caso.elementos.get("aviso-evento").classList.contains("aviso-evento--visible"), false);
    assert.deepEqual(caso.confirmaciones, ["continuar"]);
});

test("dadoQueYaVioLaTarjetaCuandoRecargaOConsultaDaComoResultadoNingunaPresentacionDuplicada", async () => {
    const caso = preparar();
    const pendiente = caso.contexto.mostrarTarjetaGlobal(caso.estado);
    caso.elementos.get("btn-continuar-evento").click();
    await pendiente;
    await caso.contexto.mostrarTarjetaGlobal(caso.estado);
    const recargado = preparar(caso.almacen);
    await recargado.contexto.mostrarTarjetaGlobal(recargado.estado);
    assert.equal(recargado.elementos.get("aviso-evento").classList.contains("aviso-evento--visible"), false);
    assert.deepEqual(recargado.confirmaciones, []);
});

test("dadoQueRecargaAntesDeLeerLaTarjetaCuandoConsultaDaComoResultadoLaTarjetaPersistida", async () => {
    const caso = preparar();
    caso.contexto.mostrarTarjetaGlobal(caso.estado);
    const recargado = preparar(caso.almacen);
    const pendiente = recargado.contexto.mostrarTarjetaGlobal(recargado.estado);
    assert.equal(recargado.elementos.get("aviso-evento-nombre").textContent, "Changa");
    recargado.elementos.get("btn-continuar-evento").click();
    await pendiente;
    assert.deepEqual(recargado.confirmaciones, []);
});

test("dadoQueElPlazoGlobalVenceDuranteElPopupCuandoLeeLaTarjetaDaComoResultadoNingunaConfirmacionTardia", async () => {
    const caso = preparar();
    const pendiente = caso.contexto.continuarEventoGlobal();
    caso.estado.activo = false;
    caso.elementos.get("btn-continuar-evento").click();
    await pendiente;
    assert.deepEqual(caso.confirmaciones, []);
});

test("dadoQueElEventoCerroSinLeerLaTarjetaCuandoSePresentaDaComoResultadoElPopupSinNuevaAccion", async () => {
    const caso = preparar();
    caso.estado.activo = false;
    const pendiente = caso.contexto.mostrarTarjetaGlobal(caso.estado);
    assert.equal(caso.elementos.get("aviso-evento").classList.contains("aviso-evento--visible"), true);
    caso.elementos.get("btn-continuar-evento").click();
    await pendiente;
    assert.deepEqual(caso.confirmaciones, []);
});
