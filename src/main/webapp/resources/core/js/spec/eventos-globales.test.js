import { aceptarEstadoEvento, puedeTirarEnEvento } from "../eventos-globales.js";

describe("Sincronización de eventos globales", () => {
    it("dadoQueSeCerroElEventoCuandoLlegaUnMensajeAntiguoDaComoResultadoQueNoSeReabre", () => {
        expect(aceptarEstadoEvento({ revision: 8, activo: false }, { revision: 7, activo: true })).toBeFalse();
    });
    it("dadoQueHayUnDesempateCuandoLlegaLaPrimeraEtapaDaComoResultadoQueNoRetrocede", () => {
        expect(aceptarEstadoEvento({ revision: 5, etapa: 2 }, { revision: 4, etapa: 1 })).toBeFalse();
        expect(aceptarEstadoEvento({ revision: 5 }, { revision: 6 })).toBeTrue();
    });
    it("dadoQueUnJugadorYaTiroCuandoSeActualizaLaInterfazDaComoResultadoSuBotonBloqueado", () => {
        const estado = { activo: true, resuelto: false, etapa: 2, habilitados: [1, 2], tiradas: { "2:1": 66 } };
        expect(puedeTirarEnEvento(estado, 1)).toBeFalse();
        expect(puedeTirarEnEvento(estado, 2)).toBeTrue();
        expect(puedeTirarEnEvento(estado, 3)).toBeFalse();
        expect(puedeTirarEnEvento({ ...estado, resuelto: true }, 2)).toBeFalse();
    });
});
