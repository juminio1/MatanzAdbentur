import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";
import { configurarRuleta } from "../ruleta-demo.js";
import { conectarTriviaPartida } from "../trivia-partida.js";
import { crearModalTrivia } from "../trivia.js";

const HTML = readFileSync(new URL("../../../../WEB-INF/views/thymeleaf/partida.html", import.meta.url), "utf8");
const HTML_TRIVIA = HTML.match(/<dialog id="trivia"[\s\S]*?<\/dialog>/)[0];
const HTML_RULETA = HTML.match(/<section id="panel-ruleta"[\s\S]*?<button type="button" id="btn-girar-ruleta"[\s\S]*?<\/section>/)[0];
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
    let efectos;
    let sonidoCobro;

    beforeAll(async () => {
        const ventana = new JSDOM('<body></body>').window;
        const anteriores = {window:globalThis.window,document:globalThis.document};
        ventana.Howl = function (configuracion) {
            const sonido = {play:jasmine.createSpy("sonido")};
            if (configuracion.src[0].includes("cobro-monedas")) sonidoCobro = sonido.play;
            return sonido;
        };
        globalThis.window = ventana;
        globalThis.document = ventana.document;
        try { efectos = await import("../efectos-compra.js?prueba-trivia"); }
        finally {
            for (const clave of Object.keys(anteriores)) {
                if (anteriores[clave] === undefined) delete globalThis[clave];
                else globalThis[clave] = anteriores[clave];
            }
            ventana.close();
        }
    });

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
        sonidoCobro.calls.reset();
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
        historialRecuperado = [],
        conFicha = false,
        fotogramas = null,
        esperarTrasCierre,
        actualizarSaldo,
        animarGanancia,
        confirmarPresentacion = async () => {},
    } = {}) {
        const ventana = new JSDOM(HTML_TRIVIA + HTML_RULETA, {
            url: `http://localhost/spring/partida?codigo=${CODIGO}`,
        }).window;
        ventana.usuarioSesionId = id;
        const documento = ventana.document;
        if (conFicha) documento.body.insertAdjacentHTML("beforeend", '<div class="casillero"><span class="ficha" data-jugador-id="1"></span></div>');
        if (fotogramas) ventana.requestAnimationFrame = callback => fotogramas.push(callback);
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
            obtenerHistorial: jasmine.createSpy("obtenerHistorial").and.resolveTo(historialRecuperado),
            obtenerEstado: jasmine.createSpy("obtenerEstado").and.resolveTo(estadoRecuperado),
            obtenerPregunta: jasmine.createSpy("obtenerPregunta").and.resolveTo(pregunta),
        };
        const recibido = jasmine.createSpy("mensajeTrivia");
        const registrarEnHistorial = jasmine.createSpy("registrarEnHistorial");
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
            configurarRuleta(documento, solicitarSorteo, () => rueda, () => {}, consultarRuleta, confirmarPresentacion);
            conectarTriviaPartida({ cliente: stomp, codigo: CODIGO, idJugador: id, documento, modal, api, registrarEnHistorial, esperarTrasCierre, actualizarSaldo, animarGanancia });
        });
        const cliente = { ventana, documento, dialogo, api, stomp, recibido, registrarEnHistorial, rueda,
            panel: documento.getElementById("panel-ruleta") };
        clientes.push(cliente);
        return cliente;
    }

    function publicarPregunta(completarRuleta = true) {
        if (completarRuleta) {
            for (const cliente of clientes) {
                if (cliente.panel.dataset.presentacionPendiente === "true") {
                    publicarRuleta(cliente, sorteada);
                    enVentana(cliente.ventana, () => cliente.rueda.onRest?.({currentIndex:0}));
        enVentana(cliente.ventana,() => jasmine.clock().tick(3000));
                }
            }
        }
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

    it("noTieneBotonYContinuaAutomaticamenteTresSegundosDespuesDeOnRest", () => {
        const cliente = crearCliente(1);
        expect(cliente.documento.getElementById("btn-cerrar-ruleta")).toBeNull();
        publicarRuleta(cliente,esperando); publicarRuleta(cliente,sorteada);
        publicarPregunta(false);
        jasmine.clock().tick(5000);
        expect(cliente.dialogo.open).toBeFalse();
        enVentana(cliente.ventana,() => cliente.rueda.onRest({currentIndex:0}));
        jasmine.clock().tick(2999);
        expect(cliente.panel.hidden).toBeFalse();
        expect(cliente.dialogo.open).toBeFalse();
        enVentana(cliente.ventana,() => jasmine.clock().tick(1));
        comprobarPregunta(cliente);
    });

    it("unaCancelacionCierraLaRuletaDeTodosYDescartaPreguntasYEstadosEnEspera", () => {
        const ana = crearCliente(1,{conFicha:true});
        const pepe = crearCliente(2,{conFicha:true});
        for (const cliente of [ana,pepe]) publicarRuleta(cliente,esperando);
        publicarPregunta(false);
        for (const cliente of [ana,pepe]) {
            publicarRuleta(cliente,{...sorteada,estado:"CANCELADA"});
            publicarRuleta(cliente,{...sorteada,estado:"CANCELADA"});
            publicarRuleta(cliente,esperando);
            publicarRuleta(cliente,sorteada);
            enVentana(cliente.ventana,() => cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent(
                "tablero:movimiento-finalizado",{detail:{idJugador:1,enTrivia:true}})));
            expect(cliente.panel.hidden).toBeTrue();
            expect(cliente.panel.dataset.presentacionPendiente).toBe("false");
            expect(cliente.dialogo.open).toBeFalse();
        }
        jasmine.clock().tick(45000);
        for (const cliente of [ana,pepe]) expect(cliente.dialogo.open).toBeFalse();
    });

    it("unaCancelacionAtrasadaNoCierraLaNuevaActivacionNiSuTrivia", () => {
        const cliente = crearCliente(1);
        publicarRuleta(cliente,esperando);
        publicarRuleta(cliente,{...sorteada,estado:"CANCELADA"});
        expect(cliente.panel.hidden).toBeTrue();
        expect(cliente.panel.dataset.presentacionPendiente).toBe("false");
        publicarRuleta(cliente,{...esperando,activacionId:"ACT-NUEVA"});
        publicarRuleta(cliente,{...sorteada,activacionId:"ACT-NUEVA"});
        publicarRuleta(cliente,{...sorteada,estado:"CANCELADA"});
        expect(cliente.panel.hidden).toBeFalse();
        expect(cliente.panel.dataset.presentacionPendiente).toBe("true");
        publicarPregunta(false);
        expect(cliente.dialogo.open).toBeFalse();
        enVentana(cliente.ventana,() => cliente.rueda.onRest({currentIndex:0}));
        enVentana(cliente.ventana,() => jasmine.clock().tick(3000));
        comprobarPregunta(cliente);
        publicarRuleta(cliente,{...sorteada,estado:"CANCELADA"});
        comprobarPregunta(cliente);
        for (const suscripcion of suscripciones) {
            suscripcion.recibir(JSON.stringify({tipo:"RONDA_CERRADA",estado,rondaCerrada:1,opcionCorrecta:1,eliminados:[]}));
            suscripcion.recibir(JSON.stringify({tipo:"RONDA_NUEVA",estado:{...estado,ronda:2},
                pregunta:{...pregunta,id:2,ronda:2}}));
        }
        publicarRuleta(cliente,{...sorteada,activacionId:"ACT-ANTIGUA-DESCONOCIDA",estado:"CANCELADA"});
        jasmine.clock().tick(2500);
        expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 2 de 5");
    });

    it("recuperaLaCancelacionPorHttpSinReabrirLaActivacionFallida", async () => {
        const cliente = crearCliente(1,{consultarRuleta:async () => ({...sorteada,estado:"CANCELADA"})});
        await procesarPendientes();
        publicarRuleta(cliente,esperando);
        publicarRuleta(cliente,sorteada);
        expect(cliente.panel.hidden).toBeTrue();
        expect(cliente.panel.dataset.presentacionPendiente).toBe("false");
        expect(cliente.dialogo.open).toBeFalse();
    });

    it("esperaALosDosClientesYUsaUnUnicoVencimientoAlRecibirLaHabilitacionDelServidor", () => {
        const listos = new Set();
        const confirmar = jugador => () => {
            listos.add(jugador);
            if (listos.size === 2) {
                estado = {...estado,inicioCoordinado:true};
                pregunta = {...pregunta,venceEn:Date.now()+20000};
                publicarPregunta(false);
            }
            return Promise.resolve();
        };
        const ana = crearCliente(1,{confirmarPresentacion:confirmar(1)});
        const pepe = crearCliente(2,{confirmarPresentacion:confirmar(2)});
        for (const cliente of [ana,pepe]) { publicarRuleta(cliente,esperando); publicarRuleta(cliente,sorteada); }
        enVentana(ana.ventana,() => ana.rueda.onRest({currentIndex:0}));
        jasmine.clock().tick(1000);
        enVentana(pepe.ventana,() => pepe.rueda.onRest({currentIndex:0}));
        enVentana(ana.ventana,() => jasmine.clock().tick(2000));
        expect(listos.size).toBe(1);
        expect(ana.dialogo.open).toBeFalse(); expect(pepe.dialogo.open).toBeFalse();
        enVentana(pepe.ventana,() => jasmine.clock().tick(1000));
        comprobarPregunta(ana); comprobarPregunta(pepe);
        expect(ana.documento.getElementById("trivia-segundos").textContent).toBe("20");
        expect(pepe.documento.getElementById("trivia-segundos").textContent).toBe("20");
        for (const suscripcion of suscripciones) {
            suscripcion.recibir(JSON.stringify({tipo:"RONDA_CERRADA",rondaCerrada:1,opcionCorrecta:1,eliminados:[],estado}));
            suscripcion.recibir(JSON.stringify({tipo:"RONDA_NUEVA",estado:{...estado,ronda:2},
                pregunta:{...pregunta,ronda:2,id:2,venceEn:Date.now()+22500}}));
        }
        for (const cliente of [ana,pepe]) {
            expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 1 de 5");
            expect(cliente.documento.querySelectorAll(".trivia__opcion--correcta").length).toBe(1);
        }
        jasmine.clock().tick(2499);
        expect(ana.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 1 de 5");
        jasmine.clock().tick(1);
        for (const cliente of [ana,pepe]) {
            expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 2 de 5");
            expect(cliente.documento.getElementById("trivia-segundos").textContent).toBe("20");
        }
    });

    it("elInicioAutorizadoLiberaUnClienteAtrasadoYLaReconexionNoReiniciaElPlazo", async () => {
        const cliente = crearCliente(1,{conFicha:true});
        publicarRuleta(cliente,esperando); publicarRuleta(cliente,sorteada);
        estado = {...estado,inicioCoordinado:true};
        publicarPregunta(false);
        comprobarPregunta(cliente);
        cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent("tablero:movimiento-finalizado",{detail:{idJugador:1,enTrivia:true}}));
        publicarRuleta(cliente,sorteada);
        expect(cliente.panel.hidden).toBeTrue();
        jasmine.clock().tick(5000);
        const reconectado = crearCliente(2,{estadoRecuperado:estado});
        await procesarPendientes();
        comprobarPregunta(reconectado);
        expect(reconectado.documento.getElementById("trivia-segundos").textContent).toBe("15");
    });

    it("unaReconexionDuranteLaPausaEsperaElInicioAutorizadoSinReiniciarElPlazo", async () => {
        estado = {...estado,ronda:2,inicioCoordinado:true};
        pregunta = {...pregunta,ronda:2,id:2,venceEn:Date.now()+22500};
        estado = {...estado,venceEn:pregunta.venceEn};
        const vencimiento = pregunta.venceEn;
        jasmine.clock().tick(1000);
        const cliente = crearCliente(2,{estadoRecuperado:estado});
        await procesarPendientes();
        expect(cliente.dialogo.open).toBeFalse();
        jasmine.clock().tick(1499);
        expect(cliente.dialogo.open).toBeFalse();
        jasmine.clock().tick(1);
        expect(cliente.dialogo.open).toBeTrue();
        expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 2 de 5");
        expect(cliente.documento.getElementById("trivia-segundos").textContent).toBe("20");
        expect(pregunta.venceEn).toBe(vencimiento);
    });

    it("esperaElCierreVisualYPresentaMontoYSonidoJuntosDentroDelTableroUnaSolaVez", async () => {
        const fotogramas = [];
        const orden = [];
        let cliente;
        cliente = crearCliente(1,{fotogramas,
            actualizarSaldo:(id,saldo) => {
                const elemento = cliente.documento.querySelector(".dinero");
                elemento.textContent = "$" + saldo;
                enVentana(cliente.ventana,() => efectos.reproducirCambioDeSaldo(1000,saldo,{silencioso:true}));
            },
            animarGanancia:(id,monto) => enVentana(cliente.ventana,() => efectos.mostrarMontoFlotante(
                cliente.documento.getElementById(String(id)),`+$${monto}`,"gana",{sonido:true,sobreTablero:true,monedas:true})),
        });
        cliente.documento.body.insertAdjacentHTML("beforeend",'<article id="1"><span class="dinero">$1000</span></article>');
        cliente.documento.querySelector(".dinero").getBoundingClientRect = () => ({left:100,top:-200,width:80,height:20});
        let terminarCierre;
        const cierreVisual = new Promise(resolve => { terminarCierre = () => { orden.push("cierre-terminado"); resolve(); }; });
        cliente.dialogo.getAnimations = () => cliente.dialogo.open ? [] : [{finished:cierreVisual}];
        let terminarMonto;
        cliente.ventana.HTMLElement.prototype.animate = function () {
            orden.push(this.classList.contains("moneda-alquiler") ? "moneda" : "monto");
            return {finished:new Promise(resolve => { terminarMonto = resolve; })};
        };
        cliente.ventana.HTMLElement.prototype.showPopover = function () { this.dataset.capaSuperior = "visible"; };
        const premio = {...estado,idTorneo:"GANANCIA-DOM",finalizada:true,idGanador:1,ganador:"Ana",tipoPremio:"PLATA",
            premioEntregado:true,premioPendiente:false,montoPremio:100,saldoGanador:1100};
        const fotogramasEspectador = [];
        let espectador;
        espectador = crearCliente(2,{fotogramas:fotogramasEspectador,
            actualizarSaldo:(id,saldo) => { espectador.documento.getElementById(String(id)).querySelector(".dinero").textContent = "$" + saldo; },
            animarGanancia:(id,monto) => enVentana(espectador.ventana,() => efectos.mostrarMontoFlotante(
                espectador.documento.getElementById(String(id)),`+$${monto}`,"gana",{sonido:true,sobreTablero:true,monedas:true})),
        });
        espectador.documento.body.insertAdjacentHTML("beforeend",'<article id="1"><span class="dinero">$1000</span></article><article id="2"><span class="dinero">$1000</span></article>');
        espectador.documento.getElementById("1").querySelector(".dinero").getBoundingClientRect = () => ({left:300,top:80,width:80,height:20});
        espectador.ventana.HTMLElement.prototype.animate = function () { return {finished:new Promise(() => {})}; };
        espectador.ventana.HTMLElement.prototype.showPopover = function () { this.dataset.capaSuperior = "visible"; };
        const enviar = tipo => {
            for (const suscripcion of suscripciones) suscripcion.recibir(JSON.stringify({tipo,estado:premio}));
        };
        enviar("PREMIO_ENTREGADO");
        expect(cliente.dialogo.open).toBeTrue();
        expect(cliente.documento.querySelector(".dinero").textContent).toBe("$1100");
        expect(sonidoCobro).not.toHaveBeenCalled();
        expect(cliente.documento.querySelector(".monto-flotante")).toBeNull();
        expect(espectador.dialogo.open).toBeTrue();
        expect(espectador.documento.getElementById("trivia-premio").textContent).toBe("Premio: $100");
        enVentana(cliente.ventana,() => cliente.documento.getElementById("trivia-cerrar").click());
        await procesarPendientes();
        while (fotogramas.length) fotogramas.shift()();
        await procesarPendientes();
        expect(cliente.dialogo.open).toBeFalse();
        expect(cliente.documento.querySelector(".monto-flotante")).toBeNull();
        expect(sonidoCobro).not.toHaveBeenCalled();
        terminarCierre();
        await procesarPendientes();
        while (fotogramas.length) fotogramas.shift()();
        await procesarPendientes();
        const monto = cliente.documento.querySelector(".monto-flotante--gana");
        expect(monto?.isConnected).toBeTrue();
        expect(monto?.textContent).toBe("+$100");
        expect(cliente.documento.querySelectorAll(".moneda-alquiler").length).toBe(6);
        expect(Number.parseFloat(monto?.style.top)).toBeGreaterThanOrEqual(48);
        expect(monto?.dataset.capaSuperior).toBe("visible");
        expect(orden).toEqual(["cierre-terminado",...Array(6).fill("moneda"),"monto"]);
        expect(sonidoCobro).toHaveBeenCalledTimes(1);
        enviar("FINALIZADA"); enviar("PREMIO_ENTREGADO");
        expect(cliente.documento.querySelectorAll(".monto-flotante").length).toBe(1);
        expect(sonidoCobro).toHaveBeenCalledTimes(1);
        jasmine.clock().tick(8000);
        expect(espectador.dialogo.open).toBeTrue();
        expect(espectador.documento.querySelector(".monto-flotante")).toBeNull();
        expect(espectador.documento.getElementById("1").querySelector(".dinero").textContent).toBe("$1100");
        expect(espectador.documento.getElementById("2").querySelector(".dinero").textContent).toBe("$1000");
        enVentana(espectador.ventana,() => espectador.documento.getElementById("trivia-cerrar").click());
        await procesarPendientes();
        while (fotogramasEspectador.length) fotogramasEspectador.shift()();
        await procesarPendientes();
        while (fotogramasEspectador.length) fotogramasEspectador.shift()();
        await procesarPendientes();
        expect(espectador.documento.querySelectorAll(".moneda-alquiler").length).toBe(6);
        expect(espectador.documento.querySelector(".moneda-alquiler").style.left).toBe("340px");
        expect(espectador.documento.querySelector(".monto-flotante").textContent).toBe("+$100");
        expect(sonidoCobro).toHaveBeenCalledTimes(2);
        enviar("PREMIO_ENTREGADO");
        expect(sonidoCobro).toHaveBeenCalledTimes(2);
        terminarMonto?.();
    });

    it("ordenaMovimientoRuletaResultadoYPreguntaEnAmbosClientesSinPerderMensajes", () => {
        estado = {...estado,enJuego:[{id:1,apodo:"Ana"}]};
        const fotogramas = [];
        const ana = crearCliente(1,{conFicha:true,fotogramas});
        const pepe = crearCliente(2,{conFicha:true,fotogramas});
        for (const cliente of [ana,pepe]) publicarRuleta(cliente,esperando);
        expect(ana.panel.hidden).toBeTrue();
        expect(pepe.panel.hidden).toBeTrue();
        for (const cliente of [ana,pepe]) {
            cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent("tablero:movimiento-iniciado",{detail:{idJugador:1}}));
            publicarRuleta(cliente,sorteada);
        }
        publicarPregunta(false);
        for (const cliente of [ana,pepe]) {
            expect(cliente.dialogo.open).toBeFalse();
            expect(cliente.panel.hidden).toBeTrue();
            enVentana(cliente.ventana,() => cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent(
                "tablero:movimiento-finalizado",{detail:{idJugador:1,enTrivia:true}})));
            expect(cliente.panel.hidden).toBeFalse();
            expect(cliente.documento.getElementById("ruleta-resultado").textContent).toBe("Girando...");
            expect(cliente.dialogo.open).toBeFalse();
            enVentana(cliente.ventana,() => cliente.rueda.onRest({currentIndex:0}));
            expect(cliente.documento.getElementById("ruleta-resultado").textContent).toBe("Resultado: FÁCIL");
            expect(cliente.dialogo.open).toBeFalse();
        }
        enVentana(ana.ventana,() => jasmine.clock().tick(3000));
        while (fotogramas.length) fotogramas.shift()();
        comprobarPregunta(ana);
        comprobarPregunta(pepe);
        expect(pepe.documento.querySelector(".trivia__opcion").disabled).toBeTrue();
    });

    it("conservaElCierreYLaRondaSiguienteRecibidosMientrasLaRuletaGira", () => {
        const cierres = [];
        const cliente = crearCliente(1,{esperarTrasCierre:continuar => cierres.push(continuar)});
        publicarRuleta(cliente,esperando);
        publicarRuleta(cliente,sorteada);
        publicarPregunta(false);
        const enviar = evento => suscripciones[0].recibir(JSON.stringify(evento));
        enviar({tipo:"RONDA_CERRADA",rondaCerrada:1,opcionCorrecta:1,eliminados:[],estado});
        enviar({tipo:"RONDA_NUEVA",estado:{...estado,ronda:2},pregunta:{...pregunta,id:2,ronda:2}});
        expect(cliente.dialogo.open).toBeFalse();
        enVentana(cliente.ventana,() => cliente.rueda.onRest({currentIndex:0}));
        enVentana(cliente.ventana,() => jasmine.clock().tick(3000));
        expect(cierres.length).toBe(1);
        expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 1 de 5");
        enVentana(cliente.ventana,() => cierres[0]());
        expect(cliente.documento.getElementById("trivia-ronda").textContent).toBe("Ronda 2 de 5");
    });

    it("presentaLaRuletaSiSuActivacionLlegaDespuesDelFinDelMovimiento", () => {
        const cliente = crearCliente(1,{conFicha:true});
        cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent("tablero:movimiento-finalizado",
            {detail:{idJugador:1,enTrivia:true}}));
        publicarRuleta(cliente,esperando);
        expect(cliente.panel.hidden).toBeFalse();
        publicarRuleta(cliente,sorteada);
        publicarPregunta(false);
        expect(cliente.dialogo.open).toBeFalse();
        enVentana(cliente.ventana,() => cliente.rueda.onRest({currentIndex:0}));
        enVentana(cliente.ventana,() => jasmine.clock().tick(3000));
        comprobarPregunta(cliente);
    });

    it("laRecuperacionHttpTambienEsperaUnMovimientoQueYaEstaEnCurso", async () => {
        const cliente = crearCliente(1,{conFicha:true,consultarRuleta:async () => esperando});
        cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent("tablero:movimiento-iniciado",{detail:{idJugador:1}}));
        await procesarPendientes();
        expect(cliente.panel.hidden).toBeTrue();
        enVentana(cliente.ventana,() => cliente.documento.dispatchEvent(new cliente.ventana.CustomEvent(
            "tablero:movimiento-finalizado",{detail:{idJugador:1,enTrivia:true}})));
        expect(cliente.panel.hidden).toBeFalse();
    });

    it("todosRegistranElRoboConfirmadoYUnClienteRecargadoRecuperaElMismoResultado", async () => {
        const ana = crearCliente(1);
        const pepe = crearCliente(2);
        const resultado = {...estado,idTorneo:"TORNEO-1",finalizada:true,idGanador:1,ganador:"Ana",
            premioEntregado:true,premioPendiente:false,tipoPremio:"ROBAR",propiedadRobada:"Terminal",propietarioAnterior:"Pepe"};
        for (const tipo of ["PREMIO_ENTREGADO","FINALIZADA","PREMIO_ENTREGADO"]) {
            for (const suscripcion of suscripciones) suscripcion.recibir(JSON.stringify({tipo,estado:resultado}));
        }
        for (const cliente of [ana,pepe]) {
            expect(cliente.registrarEnHistorial).toHaveBeenCalledOnceWith(
                "Ana ganó la trivia y recibió Terminal, robada a Pepe.",1,"trivia:TORNEO-1");
            expect(cliente.documento.getElementById("trivia-premio").textContent).toBe("Premio: Terminal, robada a Pepe");
        }
        const recargado = crearCliente(2,{historialRecuperado:[resultado]});
        await procesarPendientes();
        expect(recargado.registrarEnHistorial).toHaveBeenCalledOnceWith(
            "Ana ganó la trivia y recibió Terminal, robada a Pepe.",1,"trivia:TORNEO-1");
        expect(recargado.dialogo.open).toBeFalse();
    });

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
