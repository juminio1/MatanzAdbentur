import { JSDOM } from "jsdom";
import { conectarTriviaConRuleta, filtrarPropiedadesParaRobar } from "../trivia-partida.js";
import { crearApiTriviaPrueba } from "../trivia-api.js";

const PREGUNTA = {
    id: "p1",
    dificultad: "DIFICIL",
    enunciado: "¿En qué año Argentina ganó su primer Mundial de fútbol?",
    opciones: ["1974", "1978", "1982", "1986"],
};

// El handler de la ruleta encadena varias promesas: se deja que terminen todas.
async function procesarPendientes() {
    for (let i = 0; i < 10; i++) await new Promise(resolve => setImmediate(resolve));
}

describe("Filtro de propiedades para robar", () => {

    const propiedades = [
        { posicion: 1, nombre: "Feria La Salada", propietarioId: 2 },
        { posicion: 2, nombre: "Mi casa", propietarioId: 1 },
        { posicion: 3, nombre: "Sin dueño", propietarioId: null },
        { posicion: 4, nombre: "Estación Laferrere", propietarioId: 3 },
    ];

    it("deberiaDejarSoloLasPropiedadesDeOtrosJugadores", () => {
        const robables = filtrarPropiedadesParaRobar(propiedades, 1);

        expect(robables.map(p => p.posicion)).toEqual([1, 4]);
    });

    it("deberiaReconocerAlJugadorAunqueElIdLlegueComoTexto", () => {
        const robables = filtrarPropiedadesParaRobar(propiedades, "1");

        expect(robables.map(p => p.posicion)).toEqual([1, 4]);
    });
});

describe("Trivia al frenar la ruleta", () => {

    let documento;
    let panelRuleta;
    let modal;
    let api;
    let esperar;
    let obtenerPropiedadesParaRobar;

    function frenarRuleta(dificultad = "DIFICIL") {
        documento.dispatchEvent(new documento.defaultView.CustomEvent("ruleta:frenada", {
            detail: { dificultad, activacionId: "a1" },
        }));
    }

    beforeEach(() => {
        documento = new JSDOM('<!doctype html><body><section id="panel-ruleta"></section></body>').window.document;
        panelRuleta = documento.getElementById("panel-ruleta");

        modal = jasmine.createSpyObj("modal", ["mostrarPregunta", "mostrarResultado", "mostrarFinal", "cerrar"]);
        modal.mostrarPregunta.and.resolveTo(1);
        modal.mostrarResultado.and.returnValue(true);
        modal.mostrarFinal.and.resolveTo({ premio: null });

        api = jasmine.createSpyObj("api", ["pedirPregunta", "enviarRespuesta"]);
        api.pedirPregunta.and.resolveTo(PREGUNTA);
        api.enviarRespuesta.and.resolveTo({ correcta: true, opcionCorrecta: 1 });

        esperar = jasmine.createSpy("esperar").and.resolveTo();
        obtenerPropiedadesParaRobar = jasmine.createSpy("obtenerPropiedadesParaRobar");

        conectarTriviaConRuleta({ documento, modal, api, esperar, obtenerPropiedadesParaRobar });
    });

    it("deberiaPedirUnaPreguntaDeLaDificultadQueSalioEnLaRuleta", async () => {
        frenarRuleta("MEDIO");
        await procesarPendientes();

        expect(api.pedirPregunta).toHaveBeenCalledOnceWith("MEDIO");
        expect(modal.mostrarPregunta).toHaveBeenCalledOnceWith(PREGUNTA);
    });

    it("deberiaDejarVerLaRuletaUnMomentoYDespuesOcultarla", async () => {
        frenarRuleta();
        await procesarPendientes();

        expect(esperar.calls.first().args).toEqual([1500]);
        expect(panelRuleta.hidden).toBeTrue();
    });

    it("deberiaEnviarLaOpcionElegidaYMostrarLaCorrectaQueDevuelveElServidor", async () => {
        frenarRuleta();
        await procesarPendientes();

        expect(api.enviarRespuesta).toHaveBeenCalledOnceWith("p1", 1);
        expect(modal.mostrarResultado).toHaveBeenCalledOnceWith(1, 1);
    });

    it("deberiaMostrarComoGanadorAlQueAcertoYDejarloElegirQueRobar", async () => {
        frenarRuleta("DIFICIL");
        await procesarPendientes();

        expect(modal.mostrarFinal).toHaveBeenCalledOnceWith({
            ganador: "vos",
            soyGanador: true,
            dificultad: "DIFICIL",
            obtenerPropiedadesParaRobar,
        });
    });

    it("deberiaMostrarQueNadieGanoSiErro", async () => {
        modal.mostrarResultado.and.returnValue(false);

        frenarRuleta("FACIL");
        await procesarPendientes();

        expect(modal.mostrarFinal).toHaveBeenCalledOnceWith(jasmine.objectContaining({
            ganador: null, soyGanador: false, dificultad: "FACIL",
        }));
    });

    it("noDeberiaArrancarOtraTriviaMientrasHayUnaEnCurso", async () => {
        modal.mostrarPregunta.and.returnValue(new Promise(() => {}));

        frenarRuleta();
        await procesarPendientes();
        frenarRuleta();
        await procesarPendientes();

        expect(api.pedirPregunta).toHaveBeenCalledTimes(1);
    });

    it("deberiaCerrarElModalYPermitirOtraTriviaSiAlgoFalla", async () => {
        spyOn(console, "error");
        api.pedirPregunta.and.rejectWith(new Error("Sin conexión"));

        frenarRuleta();
        await procesarPendientes();

        expect(modal.cerrar).toHaveBeenCalled();
        expect(console.error).toHaveBeenCalled();

        api.pedirPregunta.and.resolveTo(PREGUNTA);
        frenarRuleta();
        await procesarPendientes();

        expect(modal.mostrarPregunta).toHaveBeenCalledOnceWith(PREGUNTA);
    });
});

describe("API de preguntas de prueba", () => {

    it("deberiaElegirUnaPreguntaDeLaDificultadPedida", async () => {
        const api = crearApiTriviaPrueba({ demora: 0 });

        for (const dificultad of ["FACIL", "MEDIO", "DIFICIL"]) {
            const pregunta = await api.pedirPregunta(dificultad);
            expect(pregunta.dificultad).toBe(dificultad);
        }
    });

    it("noDeberiaMandarLaRespuestaCorrectaJuntoConLaPregunta", async () => {
        const api = crearApiTriviaPrueba({ demora: 0 });

        const pregunta = await api.pedirPregunta("FACIL");

        expect(Object.keys(pregunta)).not.toContain("respuestaCorrecta");
        expect(pregunta.opciones.length).toBe(4);
    });
});
