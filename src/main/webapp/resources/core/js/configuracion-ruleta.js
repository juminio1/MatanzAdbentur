
export const CONFIGURACION_RULETA = [ //Desde aca se configura el porcentaje de la ruleta.
    {
        dificultad: "FACIL",
        probabilidad: 50,
        premio: 100,
        cantidadSegmentos: 5
    },
    {
        dificultad: "MEDIO",
        probabilidad: 30,
        premio: 250,
        cantidadSegmentos: 3
    },
    {
        dificultad: "DIFICIL",
        probabilidad: 20,
        premio: 500,
        cantidadSegmentos: 2
    }
];

const opciones = CONFIGURACION_RULETA.map(opcion => ({
    label: opcion.dificultad,
    weight: opcion.probabilidad //Permite que cada segmento tenga un tamaño proporcional al porcentaje config
}));
