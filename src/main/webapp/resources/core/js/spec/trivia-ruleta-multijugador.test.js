import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";
import { configurarRuleta } from "../ruleta-demo.js";
import { conectarTriviaPartida } from "../trivia-partida.js";
import { crearModalTrivia } from "../trivia.js";

const HTML = readFileSync(new URL("../../../../WEB-INF/views/thymeleaf/partida.html", import.meta.url), "utf8");
const HTML_TRIVIA = HTML.match(/<dialog id="trivia"[\s\S]*?<\/dialog>/)[0];
const HTML_RULETA = HTML.match(/<section id="panel-ruleta"[\s\S]*?id="btn-cerrar-ruleta"[\s\S]*?<\/section>/)[0];
const CODIGO = "94CC4A";
const TOPIC = `/topic/partida/${CODIGO}/trivia`;

// Dos documentos, modales, controladores y suscripciones distintos.
// Solo se comparte el mensaje serializado, como en una publicacion STOMP.
describe("Sincronizacion multijugador de ruleta y trivia", () => {
    let clientes;
    let suscripciones;
    let estado;
    let pregunta;
    let esperando;
    let sorteada;

    beforeEach(() => {
        jasmine.clock().install();
        jasmine.clock().mockDate(new Date("2026-11-12T12:00:00Z"));
        clientes = [];
        suscripciones = [];
        pregunta = {
            id: 1, ronda: 1, totalRondas: 5, dificultad: "FACIL",
            enunciado: "Cual es la cabecera de La Matanza?",
            opciones: ["Ramos Mejia", "San Justo", "Moron", "Haedo"],
            segundosParaResponder: 20, venceEn: Date.now() + 20000,
        };
        estado = {
            codigoPartida: CODIGO, ronda: 1, totalRondas: 5, dificultad: "FACIL",
            finalizada: false, premioPendiente: false, yaRespondio: false,
            enJuego: [{ id: 1, apodo: "Ana" }, { id: 2, apodo: "Pepe" }],
            venceEn: pregunta.venceEn,
        };
        esperando = {
            codigoPartida: CODIGO, activacionId: "ACT-1", idJugador: 1,
            estado: "ESPERANDO", dificultad: null, venceEn: Date.now() + 10000,
        };
        sorteada = { ...esperando, estado: "SORTEADA", dificultad: "FACIL" };
    });

    afterEach(() => {
        for (const cliente of clientes) cliente.ventana.close();
        jasmine.clock().uninstall();
    });

    // El codigo productivo usa document/window globales. Cada entrada sincrona
    // se ejecuta con los del cliente correspondiente y luego los restaura.
    function enVentana(ventana, accion) {
        const anteriores = { window: globalThis.window, document: globalThis.document, CustomEvent: globalThis.CustomEvent };
        globalThis.window = ventana;
        globalThis.document = ventana.document;
        globalThis.CustomEvent = ventana.CustomEvent;
        try {
            return accion();
        } finally {
            for (const nombre of Object.keys(anteriores)) {
                if (anteriores[nombre] === undefined) delete globalThis[nombre];
                else globalThis[nombre] = anteriores[nombre];
            }
        }
    }

    function crearCliente(id, {
        consultarRuleta = async () => null,
        solicitarSorteo = jasmine.createSpy("sortear").and.resolveTo(sorteada),
        estadoRecuperado = null,
    } = {}) {
        const ventana = new JSDOM(HTML_TRIVIA + HTML_RULETA, {
            url: `http://localhost/spring/partida?codigo=${CODIGO}`,
        }).window;
        ventana.usuarioSesionId = id;
        const documento = ventana.document;
        const dialogo = documento.getElementById("trivia");
        // Igual que trivia.test.js: JSDOM no implementa la capa modal del navegador.
        dialogo.showModal = function () { this.open = true; };
        dialogo.close = function () {
            this.open = false;
            this.dispatchEvent(new ventana.Event("close"));
        };
        const modalReal = crearModalTrivia(dialogo);
        const modal = {};
        for (const nombre of Object.keys(modalReal)) {
            modal[nombre] = function (...argumentos) {
                return enVentana(ventana, () => modalReal[nombre](...argumentos));
            };
        }
        const api = {
            obtenerEstado: jasmine.createSpy("obtenerEstado").and.resolveTo(estadoRecuperado),
            obtenerPregunta: jasmine.createSpy("obtenerPregunta").and.resolveTo(pregunta),
        };
        const recibido = jasmine.createSpy("mensajeTrivia");
        const stomp = {
            subscribe: jasmine.createSpy("subscribe").and.callFake(function (topic, recibir) {
                suscripciones.push({ topic, recibir: function (body) {
                    recibido(body);
                    enVentana(ventana, () => recibir({ body }));
                } });
            }),
        };
        // No se simula la logica de ruleta: solo el motor grafico de Spin Wheel.
        const rueda = { items: [{ value: "FACIL" }], onRest: null };
        enVentana(ventana, function () {
            configurarRuleta(documento, solicitarSorteo, () => rueda, () => {}, consultarRuleta);
            conectarTriviaPartida({ cliente: stomp, codigo: CODIGO, idJugador: id, documento, modal, api });
        });
        const cliente = { ventana, documento, dialogo, api, stomp, recibido,
            panel: documento.getElementById("panel-ruleta") };
        clientes.push(cliente);
        return cliente;
    }

    function publicarPregunta() {
        const body = JSON.stringify({ tipo: "RONDA_NUEVA", estado, pregunta });
        for (const suscripcion of suscripciones) {
            if (suscripcion.topic === TOPIC) suscripcion.recibir(body);
        }
    }

    function publicarRuleta(cliente, datos) {
        enVentana(cliente.ventana, function () {
            cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent("ruleta:estado", { detail: datos }));
        });
    }

    function comprobarPregunta(cliente) {
        expect(cliente.dialogo.open).toBeTrue();
        expect(cliente.documento.getElementById("trivia-enunciado").textContent).toBe(pregunta.enunciado);
        const opciones = [...cliente.documento.querySelectorAll(".trivia__opcion")].map(boton => boton.textContent.slice(1));
        expect(opciones).toEqual(pregunta.opciones);
        expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 1 de 5");
    }

    async function procesarPendientes() {
        await new Promise(resolve => setImmediate(resolve));
    }

    it("dosClientesMuestranLaMismaPreguntaYOcultanSusRuletas", () => {
        const ana = crearCliente(1);
        const pepe = crearCliente(2);
        expect(ana.documento).not.toBe(pepe.documento);
        expect(ana.stomp).not.toBe(pepe.stomp);
        publicarRuleta(ana, esperando);
        publicarRuleta(pepe, esperando);
        expect(ana.panel.hidden).toBeFalse();
        expect(pepe.panel.hidden).toBeFalse();

        publicarPregunta();

        for (const cliente of clientes) {
            expect(cliente.stomp.subscribe).toHaveBeenCalledOnceWith(TOPIC, jasmine.any(Function));
            comprobarPregunta(cliente);
            expect(cliente.panel.hidden).toBeTrue();
        }
        expect(ana.recibido.calls.first().args[0]).toBe(pepe.recibido.calls.first().args[0]);
    });

    it("unaConsultaHttpDeRuletaAtrasadaNoDebeReabrirElPanel", async () => {
        let resolver;
        const consulta = new Promise(resolve => { resolver = resolve; });
        const ana = crearCliente(1, { consultarRuleta: () => consulta });
        const pepe = crearCliente(2);
        publicarPregunta();
        expect(ana.panel.hidden).toBeTrue();

        resolver(sorteada);
        await procesarPendientes();

        comprobarPregunta(ana);
        comprobarPregunta(pepe);
        expect(ana.panel.hidden).withContext("GET /ruleta/estado atrasado reabrio la ruleta de Ana").toBeTrue();
        expect(pepe.panel.hidden).toBeTrue();
    });

    it("unEventoViejoDeRuletaNoDebeReabrirElPanel", () => {
        const ana = crearCliente(1);
        const pepe = crearCliente(2);
        publicarRuleta(ana, esperando);
        publicarRuleta(pepe, esperando);
        publicarPregunta();
        expect(ana.panel.hidden).toBeTrue();

        publicarRuleta(ana, sorteada);

        comprobarPregunta(ana);
        comprobarPregunta(pepe);
        expect(ana.panel.hidden).withContext("SORTEADA de la activacion anterior reabrio la ruleta").toBeTrue();
        expect(pepe.panel.hidden).toBeTrue();
    });

    it("unaRespuestaHttpDelSorteoAtrasadaNoDebeReabrirElPanel", async () => {
        let resolver;
        const respuesta = new Promise(resolve => { resolver = resolve; });
        const solicitar = jasmine.createSpy("sortear").and.returnValue(respuesta);
        const ana = crearCliente(1, { solicitarSorteo: solicitar });
        const pepe = crearCliente(2);
        publicarRuleta(ana, esperando);
        publicarRuleta(pepe, esperando);
        enVentana(ana.ventana, () => ana.documento.getElementById("btn-girar-ruleta").click());
        expect(solicitar).toHaveBeenCalledOnceWith("/spring", CODIGO, "ACT-1");
        publicarPregunta();

        resolver(sorteada);
        await procesarPendientes();

        comprobarPregunta(ana);
        comprobarPregunta(pepe);
        expect(ana.panel.hidden).withContext("POST sortear atrasado reabrio la ruleta").toBeTrue();
        expect(pepe.panel.hidden).toBeTrue();
    });

    it("permiteLaSiguienteActivacionSinReabrirLaQueYaInicioTrivia", () => {
        const ana = crearCliente(1);
        publicarRuleta(ana, esperando);
        publicarRuleta(ana, sorteada);
        publicarPregunta();
        expect(ana.panel.hidden).toBeTrue();

        const siguiente = { ...esperando, activacionId: "ACT-2" };
        publicarRuleta(ana, siguiente);
        expect(ana.panel.hidden).toBeFalse();
        expect(ana.documento.getElementById("btn-girar-ruleta").disabled).toBeFalse();

        // Una respuesta de ACT-1 no debe reemplazar la nueva activacion.
        publicarRuleta(ana, sorteada);
        expect(ana.documento.getElementById("btn-girar-ruleta").disabled).toBeFalse();
        publicarRuleta(ana, { ...siguiente, estado: "SORTEADA", dificultad: "FACIL" });
        expect(ana.documento.getElementById("ruleta-resultado").textContent).toBe("Girando...");
    });

    it("unClienteQueLlegaTardeRecuperaLaPreguntaSinHaberRecibidoElEvento", async () => {
        const ana = crearCliente(1);
        publicarPregunta();
        const pepe = crearCliente(2, { estadoRecuperado: estado });

        await procesarPendientes();

        expect(pepe.recibido).not.toHaveBeenCalled();
        expect(pepe.api.obtenerEstado).toHaveBeenCalledTimes(1);
        expect(pepe.api.obtenerPregunta).toHaveBeenCalledTimes(1);
        comprobarPregunta(ana);
        comprobarPregunta(pepe);
        expect(ana.panel.hidden).toBeTrue();
        expect(pepe.panel.hidden).toBeTrue();
    });
});
