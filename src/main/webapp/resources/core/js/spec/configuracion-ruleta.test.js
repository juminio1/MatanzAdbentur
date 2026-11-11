import { CONFIGURACION_RULETA } from "../configuracion-ruleta.js";

describe("Configuración de la ruleta", () => {

    it("deberiaConfigurarLasTresDificultades", () => {

        const dificultades = CONFIGURACION_RULETA.map(
            opcion => opcion.dificultad
        );

        expect(dificultades).toEqual([
            "FACIL",
            "MEDIO",
            "DIFICIL"
        ]);
    });

    it("deberiaConfigurarProbabilidadesQueSumenCien", () => {

        const total = CONFIGURACION_RULETA.reduce((suma, opcion) => suma + opcion.probabilidad, 0);

        expect(total).toBe(100);
    });

});

