import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";
import {
    crearModalTrivia, nombreDificultad, textoEnJuego, textoPremio, formatearTiempo,
} from "../trivia.js";

// Se usa el <dialog id="trivia"> real de partida.html, así el test no queda desactualizado.
const HTML_PARTIDA = readFileSync(
    new URL("../../../../WEB-INF/views/thymeleaf/partida.html", import.meta.url), "utf8");
const HTML_TRIVIA = HTML_PARTIDA.match(/<dialog id="trivia"[\s\S]*?<\/dialog>/)[0];

const PREGUNTA = {
    id: "p1",
    dificultad: "MEDIO",
    enunciado: "¿Qué río separa a Buenos Aires de Uruguay?",
    opciones: ["Río Paraná", "Río de la Plata", "Río Salado", "Río Matanza"],
};

const PROPIEDADES_AJENAS = [
    { posicion: 1, nombre: "Feria La Salada", propietario: "Emi", nivelMejora: 0 },
    { posicion: 2, nombre: "Plaza San Justo", propietario: "Emi", nivelMejora: 2 },
    { posicion: 4, nombre: "Estación Laferrere", propietario: "Nahu", nivelMejora: 0 },
];

// Deja correr las promesas pendientes (el reloj de jasmine no toca setImmediate).
const procesarPendientes = () => new Promise(resolve => setImmediate(resolve));

describe("Modal de trivia", () => {

    let documentoOriginal;
    let ventana;
    let dialogo;
    let modal;

    const elemento = id => ventana.document.getElementById(id);
    const opciones = () => [...ventana.document.querySelectorAll(".trivia__opcion")];
    const botonPremio = premio => ventana.document.querySelector(`[data-premio="${premio}"]`);

    beforeEach(() => {
        jasmine.clock().install();
        jasmine.clock().mockDate(new Date());

        ventana = new JSDOM(`<!doctype html><body>${HTML_TRIVIA}</body>`).window;
        documentoOriginal = globalThis.document;
        globalThis.document = ventana.document;

        // jsdom no implementa showModal() ni close() del <dialog>.
        dialogo = elemento("trivia");
        dialogo.showModal = function () { this.open = true; };
        dialogo.close = function () {
            this.open = false;
            this.dispatchEvent(new ventana.Event("close"));
        };

        modal = crearModalTrivia(dialogo);
    });

    afterEach(() => {
        jasmine.clock().uninstall();
        globalThis.document = documentoOriginal;
    });

    describe("Pregunta y temporizador", () => {

        it("deberiaMostrarElEnunciadoLasCuatroOpcionesLaRondaYLaDificultad", () => {
            modal.mostrarPregunta(PREGUNTA, { numeroRonda: 2 });

            expect(elemento("trivia-enunciado").textContent).toBe(PREGUNTA.enunciado);
            expect(opciones().map(o => o.textContent)).toEqual([
                "ARío Paraná", "BRío de la Plata", "CRío Salado", "DRío Matanza",
            ]);
            expect(elemento("trivia-ronda").textContent).toBe("Ronda 2 de 5");
            expect(elemento("trivia-dificultad").textContent).toBe("Media");
        });

        it("deberiaEmpezarConVeinteSegundosYLaBarraLlena", () => {
            modal.mostrarPregunta(PREGUNTA);

            expect(elemento("trivia-segundos").textContent).toBe("20");
            expect(dialogo.querySelector(".trivia__reloj").style.getPropertyValue("--restante")).toBe("1");
        });

        it("deberiaMarcarElRelojComoUrgenteRecienEnLosUltimosCincoSegundos", () => {
            modal.mostrarPregunta(PREGUNTA);
            const reloj = dialogo.querySelector(".trivia__reloj");

            jasmine.clock().tick(14000);
            expect(elemento("trivia-segundos").textContent).toBe("6");
            expect(reloj.classList.contains("trivia__reloj--urgente")).toBeFalse();

            jasmine.clock().tick(1000);
            expect(elemento("trivia-segundos").textContent).toBe("5");
            expect(reloj.classList.contains("trivia__reloj--urgente")).toBeTrue();
        });

        it("deberiaDevolverNullYBloquearLasOpcionesCuandoSeTerminaElTiempo", async () => {
            const respuesta = modal.mostrarPregunta(PREGUNTA);

            jasmine.clock().tick(20000);

            expect(await respuesta).toBeNull();
            expect(elemento("trivia-estado").textContent).toBe("Se terminó el tiempo");
            expect(opciones().every(o => o.disabled)).toBeTrue();
        });
    });

    describe("Respuesta del jugador", () => {

        it("deberiaDevolverLaOpcionElegidaYBloquearTodas", async () => {
            const respuesta = modal.mostrarPregunta(PREGUNTA);

            opciones()[2].click();

            expect(await respuesta).toBe(2);
            expect(opciones().every(o => o.disabled)).toBeTrue();
            expect(opciones().map(o => o.classList.contains("trivia__opcion--elegida")))
                .toEqual([false, false, true, false]);
            expect(elemento("trivia-estado").textContent).toBe("Esperando a los demás…");
        });

        it("deberiaFrenarElRelojAlResponder", () => {
            modal.mostrarPregunta(PREGUNTA);
            jasmine.clock().tick(3000);

            opciones()[0].click();
            jasmine.clock().tick(10000);

            expect(elemento("trivia-segundos").textContent).toBe("17");
        });
    });

    describe("Bloqueo de la pantalla", () => {

        it("deberiaAbrirElModalAlMostrarLaPregunta", () => {
            modal.mostrarPregunta(PREGUNTA);

            expect(dialogo.open).toBeTrue();
        });

        it("noDeberiaCerrarseConEscape", () => {
            modal.mostrarPregunta(PREGUNTA);
            const escape = new ventana.Event("cancel", { cancelable: true });

            dialogo.dispatchEvent(escape);

            expect(escape.defaultPrevented).toBeTrue();
        });

        it("deberiaReabrirseSiElNavegadorLoCierraDuranteLaTrivia", () => {
            modal.mostrarPregunta(PREGUNTA);

            dialogo.close();

            expect(dialogo.open).toBeTrue();
        });

        it("deberiaCerrarseCuandoTerminaLaTrivia", () => {
            modal.mostrarPregunta(PREGUNTA);

            modal.cerrar();

            expect(dialogo.open).toBeFalse();
        });
    });

    describe("Resultado de la ronda", () => {

        it("deberiaPintarLaCorrectaYLaElegidaYAvisarQueQuedoAfueraSiErro", () => {
            modal.mostrarPregunta(PREGUNTA);
            opciones()[0].click();

            const acerto = modal.mostrarResultado(0, 1);

            expect(acerto).toBeFalse();
            expect(opciones()[0].classList.contains("trivia__opcion--incorrecta")).toBeTrue();
            expect(opciones()[1].classList.contains("trivia__opcion--correcta")).toBeTrue();
            expect(elemento("trivia-estado").textContent).toBe("Incorrecto");
            expect(elemento("trivia-aviso").textContent).toBe("Quedaste afuera de la trivia");
        });

        it("deberiaFelicitarSinAvisoDeEliminacionSiAcerto", () => {
            modal.mostrarPregunta(PREGUNTA);
            opciones()[1].click();

            const acerto = modal.mostrarResultado(1, 1);

            expect(acerto).toBeTrue();
            expect(elemento("trivia-estado").textContent).toBe("¡Correcto!");
            expect(elemento("trivia-aviso").textContent).toBe("");
        });

        it("deberiaAvisarQueSeTerminoElTiempoSiNoContesto", () => {
            modal.mostrarPregunta(PREGUNTA);
            jasmine.clock().tick(20000);

            expect(modal.mostrarResultado(null, 1)).toBeFalse();
            expect(elemento("trivia-estado").textContent).toBe("Se terminó el tiempo");
            expect(elemento("trivia-aviso").textContent).toBe("Quedaste afuera de la trivia");
        });
    });

    describe("Presos y eliminados", () => {

        it("deberiaMostrarleLaPreguntaAlPresoSinDejarloResponder", () => {
            modal.mostrarPregunta(PREGUNTA, { espectador: "preso" });

            expect(dialogo.classList.contains("trivia--espectador")).toBeTrue();
            expect(opciones().every(o => o.disabled)).toBeTrue();
            expect(elemento("trivia-aviso").textContent).toBe("Estás en la cárcel: mirás la trivia sin jugar");
            expect(elemento("trivia-estado").textContent).toBe("Los demás están respondiendo…");
        });

        it("deberiaMostrarleAlEliminadoCualEraLaCorrecta", () => {
            modal.mostrarPregunta(PREGUNTA, { espectador: "eliminado" });

            const acerto = modal.mostrarResultado(null, 2);

            expect(acerto).toBeFalse();
            expect(elemento("trivia-estado").textContent).toBe("La respuesta correcta era la C");
            expect(elemento("trivia-aviso").textContent).toBe("Quedaste afuera: mirás cómo sigue la trivia");
        });

        it("deberiaListarLosJugadoresQueSiguenEnJuego", () => {
            modal.mostrarPregunta(PREGUNTA, { enJuego: ["Rafa", "Emi", "Nahu"] });

            expect(elemento("trivia-jugadores").textContent).toBe("En juego (3): Rafa, Emi y Nahu");
        });
    });

    describe("Pantalla final", () => {

        it("deberiaMostrarElGanadorYSuPremio", () => {
            modal.mostrarFinal({ ganador: "Emi", dificultad: "MEDIO" });

            expect(dialogo.classList.contains("trivia--final")).toBeTrue();
            expect(elemento("trivia-final-titulo").textContent).toBe("Ganó Emi");
            expect(elemento("trivia-final-detalle").textContent).toBe("Fue el último en pie");
            expect(elemento("trivia-premio").textContent).toBe("Premio: $250");
        });

        it("deberiaFelicitarAlJugadorQueGano", () => {
            modal.mostrarFinal({ ganador: "Rafa", soyGanador: true, dificultad: "FACIL" });

            expect(elemento("trivia-final-titulo").textContent).toBe("¡Ganaste la trivia!");
            expect(dialogo.classList.contains("trivia--ganaste")).toBeTrue();
        });

        it("deberiaOrdenarLosTiemposDelDesempateDelMasRapidoAlMasLento", () => {
            modal.mostrarFinal({
                ganador: "Emi",
                dificultad: "FACIL",
                desempate: [
                    { nombre: "Rafa", milisegundos: 41230 },
                    { nombre: "Emi", milisegundos: 38950 },
                    { nombre: "Nahu", milisegundos: 52004 },
                ],
            });

            const filas = [...elemento("trivia-tiempos").children].map(fila => fila.textContent);
            expect(filas).toEqual(["Emi39,0 s", "Rafa41,2 s", "Nahu52,0 s"]);
            expect(elemento("trivia-final-detalle").textContent)
                .toBe("Llegaron varios al final: gana el que respondió más rápido");
        });

        it("deberiaAvisarQueNadieGanoSinPremio", () => {
            modal.mostrarFinal({ ganador: null, dificultad: "DIFICIL" });

            expect(elemento("trivia-final-titulo").textContent).toBe("Nadie ganó");
            expect(elemento("trivia-premio").textContent).toBe("");
            expect(dialogo.classList.contains("trivia--sin-ganador")).toBeTrue();
        });

        it("deberiaCerrarseYResolverAlVolverAlTablero", async () => {
            const final = modal.mostrarFinal({ ganador: "Emi", dificultad: "FACIL" });

            elemento("trivia-cerrar").click();

            expect(await final).toEqual({ premio: null });
            expect(dialogo.open).toBeFalse();
        });
    });

    describe("Selector de premio", () => {

        const ganarComplicada = obtenerPropiedadesParaRobar => modal.mostrarFinal({
            ganador: "Rafa", soyGanador: true, dificultad: "DIFICIL", obtenerPropiedadesParaRobar,
        });

        it("deberiaPedirleElegirPremioAlGanadorDeUnaComplicada", () => {
            ganarComplicada();

            expect(elemento("trivia-elegir-premio").hidden).toBeFalse();
            expect(elemento("trivia-cerrar").hidden).toBeTrue();
            expect(elemento("trivia-premio").textContent).toBe("Elegí tu premio");
        });

        it("noDeberiaPedirElegirPremioSiLaComplicadaLaGanoOtro", () => {
            modal.mostrarFinal({ ganador: "Emi", dificultad: "DIFICIL" });

            expect(elemento("trivia-elegir-premio").hidden).toBeTrue();
            expect(elemento("trivia-premio").textContent).toBe("Premio: $500 o robar una propiedad");
        });

        it("deberiaDevolverPlataAlElegirLosQuinientos", async () => {
            const final = ganarComplicada();

            botonPremio("PLATA").click();
            expect(elemento("trivia-premio").textContent).toBe("Premio: $500");
            elemento("trivia-cerrar").click();

            expect(await final).toEqual({ premio: "PLATA" });
        });

        it("deberiaListarLasPropiedadesAjenasAgrupadasPorDuenoYAvisarLasMejoras", async () => {
            ganarComplicada(async () => PROPIEDADES_AJENAS);

            botonPremio("ROBAR").click();
            await procesarPendientes();

            const duenos = [...dialogo.querySelectorAll(".trivia__robar-dueno")].map(t => t.textContent);
            const avisos = [...dialogo.querySelectorAll(".trivia__robar-mejoras")];
            expect(duenos).toEqual(["De Emi", "De Nahu"]);
            expect(avisos.length).toBe(1);
            expect(avisos[0].parentElement.textContent).toContain("Plaza San Justo");
        });

        it("deberiaDevolverLaPropiedadElegidaParaRobar", async () => {
            const final = ganarComplicada(async () => PROPIEDADES_AJENAS);

            botonPremio("ROBAR").click();
            await procesarPendientes();
            [...dialogo.querySelectorAll(".trivia__robar-opcion")]
                .find(boton => boton.textContent.startsWith("Estación Laferrere")).click();
            expect(elemento("trivia-premio").textContent).toBe("Premio: Estación Laferrere, robada a Nahu");
            elemento("trivia-cerrar").click();

            expect(await final).toEqual({ premio: "ROBAR", posicion: 4 });
        });

        it("deberiaVolverALosDosBotonesDesdeLaLista", async () => {
            ganarComplicada(async () => PROPIEDADES_AJENAS);

            botonPremio("ROBAR").click();
            await procesarPendientes();
            elemento("trivia-robar-volver").click();

            expect(elemento("trivia-robar").hidden).toBeTrue();
            expect(elemento("trivia-elegir-premio").hidden).toBeFalse();
            expect(elemento("trivia-premio").textContent).toBe("Elegí tu premio");
        });

        it("deberiaDeshabilitarRobarSiNadieTienePropiedades", async () => {
            ganarComplicada(async () => []);

            botonPremio("ROBAR").click();
            await procesarPendientes();

            expect(botonPremio("ROBAR").disabled).toBeTrue();
            expect(elemento("trivia-elegir-premio").hidden).toBeFalse();
            expect(elemento("trivia-premio").textContent).toBe("Nadie tiene propiedades para robar");
        });

        it("deberiaDejarVolverAElegirSiFallaLaCargaDePropiedades", async () => {
            spyOn(console, "error");
            ganarComplicada(async () => { throw new Error("Sin conexión"); });

            botonPremio("ROBAR").click();
            await procesarPendientes();

            expect(botonPremio("ROBAR").disabled).toBeFalse();
            expect(elemento("trivia-elegir-premio").hidden).toBeFalse();
            expect(elemento("trivia-premio").textContent).toBe("No se pudieron cargar las propiedades");
        });
    });
});

describe("Textos de la trivia", () => {

    it("deberiaTraducirLaDificultadDelBackAlNombreEnPantalla", () => {
        expect(["FACIL", "MEDIO", "DIFICIL"].map(nombreDificultad)).toEqual(["Simple", "Media", "Complicada"]);
    });

    it("deberiaArmarElPremioSegunLaDificultad", () => {
        expect(textoPremio("FACIL")).toBe("Premio: $100");
        expect(textoPremio("DIFICIL")).toBe("Premio: $500 o robar una propiedad");
        expect(textoPremio(undefined)).toBe("");
    });

    it("deberiaArmarLaListaDeJugadoresEnJuego", () => {
        expect(textoEnJuego(["Rafa"])).toBe("En juego (1): Rafa");
        expect(textoEnJuego(["Rafa", "Emi"])).toBe("En juego (2): Rafa y Emi");
        expect(textoEnJuego([])).toBe("");
    });

    it("deberiaFormatearElTiempoConUnDecimalYComa", () => {
        expect(formatearTiempo(12345)).toBe("12,3 s");
        expect(formatearTiempo(38950)).toBe("39,0 s");
    });
});
