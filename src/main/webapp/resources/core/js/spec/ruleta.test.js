
import { obtenerIndiceDificultad, girarRuleta, obtenerOpcionesVisualesRuleta, crearRuleta} from "../ruleta.js";
import { CONFIGURACION_RULETA } from "../configuracion-ruleta.js";

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


it("deberiaPrepararLosTresSegmentosParaSpinWheel", () => { //El test comprueba que la config funque con el formato que necesita Spin Wheel

      const opciones = obtenerOpcionesVisualesRuleta();

        expect(opciones.length).toBe(10);

        expect(opciones.filter(
            opcion => opcion.value === "FACIL"
        ).length).toBe(5);

        expect(opciones.filter(
            opcion => opcion.value === "MEDIO"
        ).length).toBe(3);

        expect(opciones.filter(
            opcion => opcion.value === "DIFICIL"
        ).length).toBe(2);

        expect(opciones.every(
            opcion => opcion.weight === 1
        )).toBeTrue();
    });


it("deberiaCrearLaRuletaConDiezSegmentos", () => {

    const contenedorSimulado = {};
    const WheelSimulado = jasmine.createSpy("Wheel");

    crearRuleta(contenedorSimulado, WheelSimulado);

    expect(WheelSimulado).toHaveBeenCalled();

    const configuracion = WheelSimulado.calls
        .mostRecent().args[1];

    // Comprueba que Spin Wheel reciba diez segmentos
    expect(configuracion.items.length).toBe(10);

    // Comprueba que todos los segmentos tengan el mismo tamaño
    expect(configuracion.items.map(item => item.weight))
        .toEqual([1, 1, 1, 1, 1, 1, 1, 1, 1, 1]);

    // Comprueba la cantidad de segmentos de cada dificultad
    expect(configuracion.items.filter(
        item => item.value === "FACIL"
    ).length).toBe(5);

    expect(configuracion.items.filter(
        item => item.value === "MEDIO"
    ).length).toBe(3);

    expect(configuracion.items.filter(
        item => item.value === "DIFICIL"
    ).length).toBe(2);

    // Comprueba que la ruleta no pueda manipularse manualmente
    expect(configuracion.isInteractive).toBeFalse();
});



it("deberiaConfigurarLosNuevosPorcentajes", () => {

    const probabilidades = CONFIGURACION_RULETA.map(
        opcion => opcion.probabilidad
    );

    expect(probabilidades).toEqual([50, 30, 20]);
});

it("deberiaConfigurarDiezSegmentosEnTotal", () => {

    const cantidadTotal = CONFIGURACION_RULETA.reduce(
        (suma, opcion) => suma + opcion.cantidadSegmentos,
        0
    );

    expect(cantidadTotal).toBe(10);
});


it("deberiaUbicarElPunteroALaDerecha", () => {

    const contenedorSimulado = {};
    const WheelSimulado = jasmine.createSpy("Wheel");

    crearRuleta(contenedorSimulado, WheelSimulado);

    const configuracion = WheelSimulado.calls
        .mostRecent().args[1];

    // 90 grados corresponde al lado derecho de la ruleta.
    expect(configuracion.pointerAngle).toBe(90);

    // La ruleta debe mantener sus diez segmentos.
    expect(configuracion.items.length).toBe(10);
});


});
