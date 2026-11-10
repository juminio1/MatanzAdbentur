
import { configurarRuleta } from "../ruleta-demo.js";

describe("Integración del temporizador con la ruleta", () => {

    let documentoMock;
    let elementos;
    let solicitarMock;
    let crearMock;
    let animarMock;
    let ruletaMock;

    beforeEach(() => {

        jasmine.clock().install();

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

        documentoMock = {
            getElementById: jasmine.createSpy("getElementById")
                .and.callFake(id => elementos[id])
        };

        ruletaMock = {
            items: [
                { value: "FACIL" },
                { value: "MEDIO" },
                { value: "DIFICIL" }
            ],
            onRest: null
        };

        solicitarMock = jasmine.createSpy("solicitar")
            .and.resolveTo("FACIL");

        crearMock = jasmine.createSpy("crear")
            .and.returnValue(ruletaMock);

        animarMock = jasmine.createSpy("animar");

        configurarRuleta(
            documentoMock,
            solicitarMock,
            crearMock,
            animarMock
        );
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    // Ejecuta el evento registrado para un botón.
    function hacerClick(id) {

        const llamadas = elementos[id]
            .addEventListener.calls.allArgs();

        const eventoClick = llamadas.find(
            argumentos => argumentos[0] === "click"
        );

        if (!eventoClick) {
            throw new Error(
                `No se registró el evento click de ${id}`
            );
        }

        return eventoClick[1]();
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

    it("deberiaCancelarElGiroAutomaticoCuandoElJugadorPresionaGirar",
        async () => {

            hacerClick("btn-abrir-ruleta");

            jasmine.clock().tick(3000);

            await hacerClick("btn-girar-ruleta");

            jasmine.clock().tick(10000);

            expect(solicitarMock).toHaveBeenCalledTimes(1);
            expect(animarMock).toHaveBeenCalledTimes(1);
        }
    );

    it("noDeberiaRealizarDosSorteosCuandoElJugadorPresionaGirar",
        async () => {

            hacerClick("btn-abrir-ruleta");

            const primerClick = hacerClick("btn-girar-ruleta");
            const segundoClick = hacerClick("btn-girar-ruleta");

            await Promise.all([primerClick, segundoClick]);

            expect(solicitarMock).toHaveBeenCalledTimes(1);
            expect(animarMock).toHaveBeenCalledTimes(1);
        }
    );

    it("deberiaRealizarUnSoloSorteoCuandoFinalizaElTemporizador",
        async () => {

            hacerClick("btn-abrir-ruleta");

            jasmine.clock().tick(10000);

            await Promise.resolve();
            await Promise.resolve();

            expect(solicitarMock).toHaveBeenCalledTimes(1);
            expect(animarMock).toHaveBeenCalledTimes(1);

            jasmine.clock().tick(10000);

            expect(solicitarMock).toHaveBeenCalledTimes(1);
        }
    );

    it("noDeberiaGirarAutomaticamenteSiYaSeRealizoUnGiroManual",
        async () => {

            hacerClick("btn-abrir-ruleta");

            jasmine.clock().tick(5000);

            await hacerClick("btn-girar-ruleta");

            jasmine.clock().tick(15000);

            expect(solicitarMock).toHaveBeenCalledTimes(1);
        }
    );

    it("noDeberiaPermitirOtroSorteoDespuesDeFinalizarElGiro",
        async () => {

            hacerClick("btn-abrir-ruleta");

            await hacerClick("btn-girar-ruleta");

            // Simula que la ruleta se detuvo en FACIL.
            finalizarGiro(0);

            // Intenta girar nuevamente sin cerrar el modal.
            await hacerClick("btn-girar-ruleta");

            expect(solicitarMock).toHaveBeenCalledTimes(1);
            expect(animarMock).toHaveBeenCalledTimes(1);

            expect(elementos["btn-girar-ruleta"].disabled)
                .toBeTrue();
        }
    );

    it("deberiaHabilitarElCierreCuandoFinalizaLaAnimacion",
        async () => {

            hacerClick("btn-abrir-ruleta");

            await hacerClick("btn-girar-ruleta");

            expect(elementos["btn-cerrar-ruleta"].disabled)
                .toBeTrue();

            finalizarGiro(0);

            expect(elementos["btn-cerrar-ruleta"].disabled)
                .toBeFalse();

            expect(elementos["btn-girar-ruleta"].disabled)
                .toBeTrue();
        }
    );

    it("deberiaMostrarLaDificultadCuandoFinalizaLaAnimacion",
        async () => {

            hacerClick("btn-abrir-ruleta");

            await hacerClick("btn-girar-ruleta");

            finalizarGiro(0);

            expect(elementos["ruleta-resultado"].textContent)
                .toBe("Resultado: FÁCIL");
        }
    );

    it("deberiaPermitirUnNuevoGiroAlReabrirLaRuleta",
        async () => {

            hacerClick("btn-abrir-ruleta");

            await hacerClick("btn-girar-ruleta");

            finalizarGiro(0);

            hacerClick("btn-cerrar-ruleta");

            expect(elementos["panel-ruleta"].hidden)
                .toBeTrue();

            hacerClick("btn-abrir-ruleta");

            await hacerClick("btn-girar-ruleta");

            expect(solicitarMock).toHaveBeenCalledTimes(2);
            expect(animarMock).toHaveBeenCalledTimes(2);

            // La instancia visual se reutiliza.
            expect(crearMock).toHaveBeenCalledTimes(1);
        }
    );

});
