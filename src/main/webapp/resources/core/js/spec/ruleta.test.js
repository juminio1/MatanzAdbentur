
import { obtenerIndiceDificultad, girarRuleta } from "../ruleta.js";

describe("Ruleta de dificultad", () => {

    it("deberiaObtenerElIndiceDeLaDificultadFacil", () => {

        const indice = obtenerIndiceDificultad("FACIL");

        expect(indice).toBe(0);
    });

    it("deberiaObtenerElIndiceDeLaDificultadMedia", () => {

        const indice = obtenerIndiceDificultad("MEDIO");

        expect(indice).toBe(1);
    });

    it("deberiaObtenerElIndiceDeLaDificultadDificil", () => {

        const indice = obtenerIndiceDificultad("DIFICIL");

        expect(indice).toBe(2);
    });

    it("deberiaRechazarUnaDificultadInvalida", () => {

        expect(() => {
            obtenerIndiceDificultad("EXTREMO");
        }).toThrowError("La dificultad no es válida");
    });

    it("deberiaGirarHastaLaDificultadIndicada", () => {

    const ruletaSimulada = jasmine.createSpyObj(
        "ruleta",
        ["spinToItem"]
    );

    girarRuleta("DIFICIL", ruletaSimulada);

    expect(ruletaSimulada.spinToItem).toHaveBeenCalledWith(
        2, 4000, true, 3, 1
    );
});

});
