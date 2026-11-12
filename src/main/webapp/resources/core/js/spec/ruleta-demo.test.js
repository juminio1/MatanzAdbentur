
import { configurarRuleta } from "../ruleta-demo.js";

describe("Integración del temporizador y WebSocket con la ruleta", () => {

    let documentoMock;
    let elementos;
    let eventos;
    let solicitarMock;
    let consultarMock;
    let crearMock;
    let animarMock;
    let ruletaMock;
    let ventanaOriginal;

   
    beforeEach(() => {

        jasmine.clock().install();
        jasmine.clock().mockDate(new Date());

        ventanaOriginal = globalThis.window;


        globalThis.window = {
            location: {
                pathname: "/spring/partida",
                search: "?codigo=ABC123"
            },
            codigoSala: "ABC123",
            usuarioSesionId: 10
        };

        function crearElemento() {
            return {
                hidden: false,
                disabled: false,
                textContent: "",
                addEventListener: jasmine.createSpy("addEventListener"),
                classList: {
                    add: jasmine.createSpy("add"),
                    remove: jasmine.createSpy("remove")
                }
            };
        }

        elementos = {
            "btn-abrir-ruleta": crearElemento(),
            "btn-girar-ruleta": crearElemento(),
            "btn-cerrar-ruleta": crearElemento(),
            "panel-ruleta": crearElemento(),
            "ruleta-visual": crearElemento(),
            "ruleta-resultado": crearElemento(),
            "ruleta-temporizador": crearElemento()
        };

        elementos["panel-ruleta"].hidden = true;

        eventos = {};

        documentoMock = {
            getElementById: jasmine.createSpy("getElementById").and.callFake(id => elementos[id]),
            addEventListener: jasmine.createSpy("addEventListener").and.callFake((nombre, callback) => {
                eventos[nombre] = callback;
            })
        };

        ruletaMock = {
            items: [
                { value: "FACIL" },
                { value: "MEDIO" },
                { value: "DIFICIL" }
            ],
            onRest: null
        };

        solicitarMock = jasmine.createSpy("solicitar").and.callFake(async () => estadoSorteado());

        consultarMock = jasmine.createSpy("consultar").and.resolveTo(null);

        crearMock = jasmine.createSpy("crear").and.returnValue(ruletaMock);

        animarMock = jasmine.createSpy("animar");
    });

    afterEach(() => {

        jasmine.clock().uninstall();

        if (ventanaOriginal === undefined) {
            delete globalThis.window;
        } else {
            globalThis.window = ventanaOriginal;
        }
    });

    function configurar() {

        configurarRuleta(
            documentoMock,
            solicitarMock,
            crearMock,
            animarMock,
            consultarMock
        );
    }

    function estadoEsperando(activacionId = "ACT-1", idJugador = 10) {

        return {
            codigoPartida: "ABC123",
            activacionId,
            idJugador,
            estado: "ESPERANDO",
            dificultad: null,
            venceEn: Date.now() + 10000
        };
    }

    function estadoSorteado(activacionId = "ACT-1", dificultad = "FACIL") {

        return {
            codigoPartida: "ABC123",
            activacionId,
            idJugador: 10,
            estado: "SORTEADA",
            dificultad,
            venceEn: Date.now() + 10000
        };
    }

    // Ejecuta el evento registrado para un botón.
    function hacerClick(id) {

        const llamadas = elementos[id].addEventListener.calls.allArgs();

        const eventoClick = llamadas.find(argumentos => argumentos[0] === "click");

        if (!eventoClick) {
            throw new Error(`No se registró el evento click de ${id}`);
        }

        return eventoClick[1]();
    }

    // Simula que el backend envía un estado mediante WebSocket.
    function recibirEstado(estado) {

        if (!eventos["ruleta:estado"]) {
            throw new Error("No se registró el evento ruleta:estado");
        }

        eventos["ruleta:estado"]({
            detail: estado
        });
    }

    // Simula que Spin Wheel terminó la animación.
    function finalizarGiro(indice = 0) {

        if (!ruletaMock.onRest) {
            throw new Error("No se registró el evento onRest");
        }

        ruletaMock.onRest({
            currentIndex: indice
        });
    }

    // Permite resolver las promesas de los servicios simulados.
    async function resolverPromesas() {

        await Promise.resolve();
        await Promise.resolve();
        await Promise.resolve();
    }

    it("deberiaMostrarLaRuletaCuandoRecibeUnaActivacion",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            expect(elementos["panel-ruleta"].hidden).toBeFalse();

            expect(crearMock).toHaveBeenCalledTimes(1);

            expect(elementos["ruleta-resultado"].textContent).toBe("¡Probá tu suerte!");

            expect(elementos["btn-girar-ruleta"].disabled).toBeFalse();

            expect(elementos["btn-cerrar-ruleta"].disabled).toBeTrue();
        }
    );

    it("deberiaHabilitarElGiroSolamenteAlJugadorCorrespondiente",
        async () => {

            configurar();

            recibirEstado(estadoEsperando("ACT-1", 20));

            expect(elementos["btn-girar-ruleta"].disabled).toBeTrue();

            hacerClick("btn-girar-ruleta");

            await resolverPromesas();

            expect(solicitarMock).not.toHaveBeenCalled();

            expect(animarMock).not.toHaveBeenCalled();
        }
    );

    it("deberiaSolicitarUnSoloSorteoAlPresionarGirar",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            hacerClick("btn-girar-ruleta");
            hacerClick("btn-girar-ruleta");

            await resolverPromesas();

            expect(solicitarMock).toHaveBeenCalledTimes(1);

            expect(solicitarMock).toHaveBeenCalledWith(
                "/spring",
                "ABC123",
                "ACT-1"
            );

            expect(animarMock).toHaveBeenCalledTimes(1);

            expect(animarMock).toHaveBeenCalledWith("FACIL", ruletaMock);
        }
    );

    it("noDeberiaSolicitarElSorteoDesdeElTemporizadorDelFrontend",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            // Simula que transcurren los 10 segundos.
            jasmine.clock().tick(10000);

            await resolverPromesas();

            // El frontend no debe realizar el sorteo automático.
            expect(solicitarMock).not.toHaveBeenCalled();

            expect(animarMock).not.toHaveBeenCalled();

            expect(elementos["btn-girar-ruleta"].disabled).toBeTrue();

            // El backend comunica el resultado definitivo.
            recibirEstado(estadoSorteado());

            expect(animarMock).toHaveBeenCalledTimes(1);
        }
    );

    it("deberiaAnimarLaDificultadRecibidaPorWebSocket",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            // El backend informa que la dificultad sorteada es MEDIA.
            recibirEstado(estadoSorteado("ACT-1", "MEDIO"));

            expect(animarMock).toHaveBeenCalledTimes(1);

            expect(animarMock).toHaveBeenCalledWith("MEDIO", ruletaMock);

            // Recibir el resultado no debe generar otra solicitud REST.
            expect(solicitarMock).not.toHaveBeenCalled();
        }
    );

    it("noDeberiaAnimarDosVecesLaMismaActivacion",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            const resultado = estadoSorteado();

            recibirEstado(resultado);
            recibirEstado(resultado);

            expect(animarMock).toHaveBeenCalledTimes(1);

            // La ruleta ya fue sorteada.
            expect(elementos["btn-girar-ruleta"].disabled).toBeTrue();

            expect(solicitarMock).not.toHaveBeenCalled();
        }
    );

    it("deberiaMostrarLaDificultadCuandoFinalizaLaAnimacion",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            recibirEstado(estadoSorteado());

            expect(elementos["btn-cerrar-ruleta"].disabled).toBeTrue();

            // Simula que la ruleta se detuvo en FACIL.
            finalizarGiro(0);

            expect(elementos["ruleta-resultado"].textContent).toBe("Resultado: FÁCIL");

            expect(elementos["btn-cerrar-ruleta"].disabled).toBeFalse();

            expect(elementos["btn-girar-ruleta"].disabled).toBeTrue();
        }
    );

    it("deberiaRecuperarElEstadoDeLaRuletaAlRecargarLaPagina",
        async () => {

            // Simula que el backend tiene una activación pendiente.
            consultarMock.and.resolveTo(estadoEsperando());

            configurar();

            await resolverPromesas();

            expect(consultarMock).toHaveBeenCalledWith("/spring", "ABC123");

            expect(elementos["panel-ruleta"].hidden).toBeFalse();

            expect(elementos["btn-girar-ruleta"].disabled).toBeFalse();

            expect(crearMock).toHaveBeenCalledTimes(1);
        }
    );

    it("noDeberiaReabrirUnaRuletaYaSorteadaAlRecargarLaPagina",
        async () => {

            // Simula recargar después de la trivia: el backend sigue con la activación sorteada.
            consultarMock.and.resolveTo(estadoSorteado("ACT-1", "DIFICIL"));

            configurar();

            await resolverPromesas();

            expect(elementos["panel-ruleta"].hidden).toBeTrue();
            expect(crearMock).not.toHaveBeenCalled();
        }
    );

    it("noDeberiaPermitirOtroSorteoAlReabrirLaMismaActivacion",
        async () => {

            configurar();

            recibirEstado(estadoEsperando());

            hacerClick("btn-girar-ruleta");

            await resolverPromesas();

            finalizarGiro(0);

            hacerClick("btn-cerrar-ruleta");

            expect(elementos["panel-ruleta"].hidden).toBeTrue();

            hacerClick("btn-abrir-ruleta");

            expect(elementos["panel-ruleta"].hidden).toBeFalse();

            expect(elementos["btn-girar-ruleta"].disabled).toBeTrue();

            hacerClick("btn-girar-ruleta");

            await resolverPromesas();

            // Una misma activación permite un único sorteo.
            expect(solicitarMock).toHaveBeenCalledTimes(1);

            expect(animarMock).toHaveBeenCalledTimes(1);

            // La instancia visual se reutiliza.
            expect(crearMock).toHaveBeenCalledTimes(1);
        }
    );

});
