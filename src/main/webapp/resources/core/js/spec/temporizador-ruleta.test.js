
import { iniciarTemporizadorRuleta } from "../temporizador-ruleta.js";

describe("Temporizador de ruleta", () => {

    beforeEach(() => {
        jasmine.clock().install();
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it("deberiaGirarAutomaticamenteDespuesDeDiezSegundos", () => {

        const girarMock = jasmine.createSpy("girar");

        iniciarTemporizadorRuleta(10, girarMock);

        jasmine.clock().tick(10000);

        expect(girarMock).toHaveBeenCalledTimes(1);
    });

    it("noDeberiaGirarAutomaticamenteAntesDeLosDiezSegundos", () => {

        const girarMock = jasmine.createSpy("girar");

        iniciarTemporizadorRuleta(10, girarMock);

        jasmine.clock().tick(9999);

        expect(girarMock).not.toHaveBeenCalled();
    });

    it("deberiaCancelarElTemporizadorSiElJugadorPresionaGirar", () => {

        const girarMock = jasmine.createSpy("girar");

        const temporizador = iniciarTemporizadorRuleta(10, girarMock);

        // Simula que el jugador presiona el btn girar antes de los 10 segundos.
        clearTimeout(temporizador);

        jasmine.clock().tick(10000);

        // El giro automático no debe ejecutarse.
        expect(girarMock).not.toHaveBeenCalled();
    });

    it("noDeberiaEjecutarElGiroMasDeUnaVez", () => {

        const girarMock = jasmine.createSpy("girar");

        iniciarTemporizadorRuleta(10, girarMock);

        jasmine.clock().tick(10000);

        expect(girarMock).toHaveBeenCalledTimes(1);

        // Simulamos que transcurren otros 10 segundos.
        jasmine.clock().tick(10000);

        expect(girarMock).toHaveBeenCalledTimes(1);
    });
});
