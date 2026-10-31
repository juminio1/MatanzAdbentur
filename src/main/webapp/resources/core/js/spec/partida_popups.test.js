import { readFileSync } from "node:fs";
import { Script, runInContext } from "node:vm";
import { JSDOM } from "jsdom";

const plantilla = readFileSync(new URL("../../../../WEB-INF/views/thymeleaf/partida.html", import.meta.url), "utf8");
const codigoPartida = readFileSync(new URL("../partida.js", import.meta.url), "utf8");

describe("Popups de eventos globales y deudas", function () {
    let pagina;
    let ventana;
    let contexto;

    beforeEach(async function () {
        pagina = new JSDOM(plantilla, {
            url: "http://localhost/spring/partida?codigo=ABC",
            runScripts: "outside-only",
        });
        ventana = pagina.window;
        await new Promise((resolver) => ventana.addEventListener("DOMContentLoaded", resolver, { once: true }));
        ventana.usuarioSesionId = 1;
        ventana.dineroUsuarioSesion = 500;
        ventana.fetch = jasmine.createSpy("fetch");
        const jugadores = ventana.document.querySelector(".jugadores");
        jugadores.innerHTML = '<article class="jugador" id="1"><h3 class="nombre-jugador">messi10L</h3><span class="dinero">$500</span></article>'
            + '<article class="jugador" id="2"><h3 class="nombre-jugador">test123</h3><span class="dinero">$200</span></article>';
        ventana.HTMLDialogElement.prototype.showModal = function () { this.open = true; };
        ventana.HTMLDialogElement.prototype.close = function () {
            this.open = false;
            this.dispatchEvent(new ventana.Event("close"));
        };
        contexto = pagina.getInternalVMContext();
        new Script(codigoPartida).runInContext(contexto);
    });

    afterEach(function () {
        ventana.close();
    });

    function mostrarDeuda(motivo, acreedor = null) {
        ventana.respuestaDeuda = { tieneDeudaPendiente: true, montoPendiente: 1000, motivo, acreedor };
        runInContext("estadoDeudaLocal = { estado: 'actualizado', deuda: validarRespuestaDeuda(respuestaDeuda), error: null }; actualizarAvisoDeuda();", contexto);
    }

    it("dadoQueElServidorConservaLosNombresDeLasFasesCuandoSeMuestraElEstadoEntoncesElFrontendReconoceTodasLasFases", function () {
        const fases = {
            INICIADO: "Evento iniciado", TIRADA: "Tiradas en curso", EMPATE: "Empate",
            DESEMPATE: "Desempate", FINALIZADO: "Evento finalizado",
        };
        for (const [fase, etiqueta] of Object.entries(fases)) {
            ventana.resultadoGlobal = {
                fase, tipo: "TRAMITES_AFIP", etapa: 0, participantes: [1, 2],
                tiradas: {}, confirmados: [], segundosRestantes: 8,
            };

            runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

            expect(ventana.document.getElementById("evento-global-fase").textContent).toBe(etiqueta);
            expect(ventana.document.getElementById("modal-evento-global").classList
                .contains("modal-evento-global--finalizado")).toBe(fase === "FINALIZADO");
        }
    });

    it("dadoQueElGanadorYaEstaPrimeroCuandoSeMuestraTramitesEntoncesSeExplicaElMotivoConSuNombre", function () {
        ventana.resultadoGlobal = {
            fase: "FINALIZADO", tipo: "TRAMITES_AFIP", etapa: 0, idJugadorElegido: 2,
            participantes: [1, 2], tiradas: { 1: 3, 2: 5 }, confirmados: [], segundosRestantes: 35,
            detalle: "no avanza porque ya es el jugador más adelantado del tablero.",
        };

        runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

        expect(ventana.document.getElementById("evento-global-resultado").textContent)
            .toBe("test123 no avanza porque ya es el jugador más adelantado del tablero.");
    });

    it("dadoQueElGanadorDebeAvanzarCuandoSeMuestraTramitesEntoncesSeConservaElAnuncioDeMovimiento", function () {
        ventana.resultadoGlobal = {
            fase: "FINALIZADO", tipo: "TRAMITES_AFIP", etapa: 0, idJugadorElegido: 1,
            participantes: [1, 2], tiradas: { 1: 6, 2: 3 }, confirmados: [], segundosRestantes: 35,
            detalle: "se moverá del casillero 5 al 20 cuando se reanude la partida.",
        };

        runInContext("mostrarEstadoEventoGlobal(resultadoGlobal)", contexto);

        const mensaje = ventana.document.getElementById("evento-global-resultado").textContent;
        expect(mensaje).toContain("messi10L se moverá");
        expect(mensaje).toContain("cuando se reanude la partida.");
        expect(mensaje).not.toContain("no avanza");
    });

    it("dadoQueKevinGeneraUnaDeudaCuandoSeAbreElAvisoEntoncesSeMuestranElMotivoElSaldoYElImporteReales", function () {
        mostrarDeuda("EVENTO_GLOBAL_ROBO");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Robo de dinero — El Kevin");
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$500");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
        expect(ventana.document.getElementById("deuda-acreedor-contenedor").hidden).toBeTrue();
    });

    it("dadoQueLaDeudaEsUnAlquilerCuandoSeAbreElAvisoEntoncesSeMuestranElOrigenYElAcreedor", function () {
        mostrarDeuda("ALQUILER", { idJugador: 2, apodo: "José" });

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Alquiler de propiedad");
        expect(ventana.document.getElementById("deuda-acreedor").textContent).toBe("José");
        expect(ventana.document.getElementById("deuda-acreedor-contenedor").hidden).toBeFalse();
    });

    it("dadoQueElMotivoTieneUnaDescripcionCuandoSeAbreElAvisoEntoncesSeConservaLaDescripcion", function () {
        mostrarDeuda("Multa por control de tránsito");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Multa por control de tránsito");
    });

    it("dadoQueUnaDeudaAntiguaNoGuardaElEventoCuandoSeAbreElAvisoEntoncesNoSeInventaSuOrigen", function () {
        mostrarDeuda("EVENTO_RESTA");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Pago por un evento del tablero");
        expect(ventana.document.getElementById("deuda-motivo").textContent).not.toContain("Kevin");
    });

    it("dadoQueElMotivoEsUnCodigoDesconocidoCuandoSeAbreElAvisoEntoncesSeMuestraUnTextoEntendible", function () {
        mostrarDeuda("ORIGEN_DESCONOCIDO");

        expect(ventana.document.getElementById("deuda-motivo").textContent).toBe("Pago pendiente del juego");
    });

    it("dadoQueElSaldoRealEsCeroCuandoSeAbreElAvisoEntoncesSeMuestraCero", function () {
        ventana.dineroUsuarioSesion = 0;

        mostrarDeuda("ALQUILER");

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$0");
    });

    it("dadoQueNoSeConoceElSaldoCuandoSeAbreElAvisoEntoncesNoSeInventaUnImporte", function () {
        ventana.dineroUsuarioSesion = undefined;

        mostrarDeuda("ALQUILER");

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("No disponible");
    });

    it("dadoQueLlegaUnSaldoPorWebSocketCuandoSeActualizaElJugadorEntoncesElAvisoReflejaSoloElSaldoPropio", function () {
        mostrarDeuda("ALQUILER");

        runInContext("actualizarDineroJugador(2, 9000)", contexto);
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$500");
        runInContext("actualizarDineroJugador(1, 750)", contexto);

        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$750");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
    });

    it("dadoQueSeVendeUnaPropiedadCuandoElServidorActualizaElDineroEntoncesElAvisoMuestraElNuevoSaldo", async function () {
        mostrarDeuda("EVENTO_GLOBAL_ROBO");
        ventana.confirm = () => true;
        ventana.actualizarEtiquetaPrecioPropiedad = jasmine.createSpy("actualizarEtiquetaPrecioPropiedad");
        ventana.cargarPropiedadesPropias = jasmine.createSpy("cargarPropiedadesPropias").and.resolveTo();
        ventana.fetch.and.callFake(async (url) => ({
            ok: true,
            json: async () => url.endsWith("/vender")
                ? { idJugador: 1, dineroJugador: 750 }
                : ventana.respuestaDeuda,
        }));

        await runInContext("venderPropiedadBono({nombre:'Propiedad de prueba', posicion:5, precioVenta:250})", contexto);

        expect(ventana.fetch).toHaveBeenCalledWith("/spring/partida/ABC/propiedades/5/vender",
            { method: "POST", credentials: "same-origin" });
        expect(ventana.document.getElementById("deuda-saldo").textContent).toBe("$750");
        expect(ventana.document.getElementById("deuda-importe").textContent).toBe("$1.000");
        expect(ventana.cargarPropiedadesPropias).toHaveBeenCalled();
    });

    it("dadoQueHayDeudaCuandoSeUsanLosBotonesEntoncesSeConservanLaConfirmacionYElAccesoAPropiedades", function () {
        mostrarDeuda("ALQUILER");
        ventana.cargarPropiedadesPropias = jasmine.createSpy("cargarPropiedadesPropias").and.resolveTo();

        ventana.document.getElementById("btn-declarar-quiebra").click();

        expect(ventana.document.getElementById("deuda-confirmacion").hidden).toBeFalse();
        expect(ventana.fetch).not.toHaveBeenCalled();
        ventana.document.getElementById("btn-cancelar-quiebra").click();
        expect(ventana.document.getElementById("deuda-confirmacion").hidden).toBeTrue();
        ventana.document.getElementById("btn-vender-desde-deuda").click();
        expect(ventana.document.getElementById("modal-deuda-pendiente").open).toBeFalse();
        expect(ventana.document.getElementById("modal-bono-propiedades").open).toBeTrue();
        expect(ventana.cargarPropiedadesPropias).toHaveBeenCalled();
    });
});
