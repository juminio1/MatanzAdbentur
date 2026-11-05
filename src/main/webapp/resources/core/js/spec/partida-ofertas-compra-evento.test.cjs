const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const fuente = fs.readFileSync(path.join(__dirname, "../../main/webapp/resources/core/js/partida.js"), "utf8");
const lector = fuente.slice(fuente.indexOf("async function leerRespuestaJson("), fuente.indexOf("async function enviarOfertaVenta("));
const compra = fuente.slice(fuente.indexOf("async function mostrarCompraDeEvento("), fuente.indexOf("// Agregar este helper"));
const ofertas = fuente.slice(fuente.indexOf("function mostrarOfertaRecibida("), fuente.indexOf("async function responderOfertaVenta("));
const modales = fuente.slice(fuente.indexOf("function esModalPermitidoDuranteEvento("), fuente.indexOf("function bloquearAccionesFinalizadas("));

function respuesta(datos, estado = 200) {
    return { ok: estado < 400, status: estado, text: async () => JSON.stringify(datos) };
}

function preparar() {
    const elementos = new Map();
    for (const id of ["modal-compra-evento", "compra-evento-detalle", "compra-evento-mensaje", "btn-compra-evento-aceptar",
        "btn-compra-evento-rechazar", "modal-oferta-recibida", "oferta-recibida-detalle"]) {
        elementos.set(id, {
            id, open: false, textContent: "", disabled: false,
            close() { this.open = false; }, showModal() { this.open = true; },
        });
    }
    const solicitudes = [];
    const saldos = [];
    const estado = { activo: false, seleccionado: 1, ronda: 5, compraPendiente: 5 };
    const contexto = vm.createContext({
        window: { usuarioSesionId: 1, turnoActualId: 2 }, tiempoRestante: 0,
        estadoEventoGlobalCliente: estado, partidaFinalizada: false, basePath: "", codigoPartida: "PRUEBA",
        comprasGlobalesMostradas: new Set(), compraEventoActual: null, ofertaActiva: null,
        modalOfertaRecibida: elementos.get("modal-oferta-recibida"), mensajeOfertaRecibida: {}, console,
        document: {
            getElementById: id => elementos.get(id),
            querySelector: () => [...elementos.values()].find(elemento => elemento.open) ?? null,
        },
        debeEsperarPresentacionGlobal: () => false,
        fetch: async (url, opciones) => {
            solicitudes.push({ url, opciones });
            return opciones?.method === "POST" ? respuesta({ idJugador: 1, dineroJugador: 1700 })
                : respuesta({ nombre: "UNLaM", disponible: true, precioCompra: 300 });
        },
        actualizarEtiquetaPrecioPropiedad() {}, actualizarDineroJugador: (...valores) => saldos.push(valores),
        sincronizarEventoGlobal() {}, finalizarTurno() { throw new Error("No debe avanzar el turno"); },
    });
    vm.runInContext(lector + compra + ofertas + modales, contexto);
    return { contexto, elementos, solicitudes, saldos, estado };
}

test("dadoQueElEventoSigueActivoCuandoHayCompraPendienteDaComoResultadoNingunaCompraOrdinariaHabilitada", async () => {
    const caso = preparar();
    caso.estado.activo = true;
    await caso.contexto.mostrarCompraDeEvento(caso.estado);
    assert.equal(caso.solicitudes.length, 0);
    assert.equal(caso.elementos.get("modal-compra-evento").open, false);
});

test("dadoQueElEventoTerminoCuandoSeConsultaVariasVecesDaComoResultadoUnaSolaOportunidadFueraDeTurno", async () => {
    const caso = preparar();
    await caso.contexto.mostrarCompraDeEvento(caso.estado);
    await caso.contexto.mostrarCompraDeEvento(caso.estado);
    assert.equal(caso.solicitudes.length, 1);
    assert.equal(caso.elementos.get("modal-compra-evento").open, true);
    assert.match(caso.elementos.get("compra-evento-detalle").textContent, /UNLaM.*\$300/);
    await caso.contexto.responderCompraDeEvento(true);
    await caso.contexto.responderCompraDeEvento(true);
    assert.equal(caso.solicitudes.length, 2);
    assert.deepEqual(JSON.parse(caso.solicitudes[1].opciones.body), { ronda: 5, aceptar: true });
    assert.deepEqual(caso.saldos, [[1, 1700]]);
    assert.equal(caso.elementos.get("modal-compra-evento").open, false);
});

test("dadoQueLaCompraEsOpcionalCuandoSeRechazaDaComoResultadoNingunDescuentoNiAvanceDeTurno", async () => {
    const caso = preparar();
    await caso.contexto.mostrarCompraDeEvento(caso.estado);
    await caso.contexto.responderCompraDeEvento(false);
    assert.deepEqual(JSON.parse(caso.solicitudes[1].opciones.body), { ronda: 5, aceptar: false });
    assert.deepEqual(caso.saldos, []);
    assert.equal(caso.elementos.get("modal-compra-evento").open, false);
});

test("dadoQueElServidorRechazaLaCompraCuandoSeRecibeJsonDaComoResultadoUnMensajeYLaPosibilidadDeRechazar", async () => {
    const caso = preparar();
    await caso.contexto.mostrarCompraDeEvento(caso.estado);
    caso.contexto.fetch = async () => respuesta({ error: "Dinero insuficiente" }, 409);
    await caso.contexto.responderCompraDeEvento(true);
    assert.equal(caso.elementos.get("compra-evento-mensaje").textContent, "Dinero insuficiente");
    assert.equal(caso.elementos.get("btn-compra-evento-rechazar").disabled, false);
    assert.deepEqual(caso.saldos, []);
});

test("dadoQueElServidorDevuelveHtmlCuandoSeLeeLaRespuestaDaComoResultadoUnErrorComprensible", async () => {
    const caso = preparar();
    await assert.rejects(caso.contexto.leerRespuestaJson({ status: 500, text: async () => "<html><head>Error</head></html>" }),
        /El servidor no pudo completar la solicitud \(HTTP 500\)/);
});

test("dadoQueElEventoEstaActivoCuandoLlegaUnaOfertaDaComoResultadoSoloElPopupAutorizadoParaDeuda", () => {
    const caso = preparar();
    caso.estado.activo = true;
    const oferta = { apodoVendedor: "Ana", nombrePropiedad: "UNLaM", precio: 600, paraDeuda: false };
    caso.contexto.mostrarOfertaRecibida(oferta);
    assert.equal(caso.elementos.get("modal-oferta-recibida").open, false);
    caso.contexto.mostrarOfertaRecibida({ ...oferta, paraDeuda: true });
    assert.equal(caso.elementos.get("modal-oferta-recibida").open, true);
});
