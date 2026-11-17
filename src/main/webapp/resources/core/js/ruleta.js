
import { CONFIGURACION_RULETA } from "./configuracion-ruleta.js";

// Define cómo se distribuyen las dificultades dentro de los 10 segmentos.
// Fácil aparece 5 veces, Media 3 veces y Difícil 2 veces.
const ORDEN_SEGMENTOS = [
    "FACIL",
    "MEDIO",
    "DIFICIL",
    "FACIL",
    "MEDIO",
    "FACIL",
    "DIFICIL",
    "FACIL",
    "MEDIO",
    "FACIL"
];

export function obtenerIndiceDificultad(dificultad) {

    // Busca el primer segmento que coincida con la dificultad recibida.
    const indice = ORDEN_SEGMENTOS.findIndex(
        opcion => opcion === dificultad
    );

    if (indice === -1) {
        throw new Error("La dificultad no es válida");
    }

    return indice;
}

export function girarRuleta(dificultad, ruleta) {
    // Dificultad: Es el resultado que envia el back.
    // Ruleta: Es la instancia de Spin Wheel + la animación.

    const indice = obtenerIndiceDificultad(dificultad);

    // Indica a la librería que anime el giro y se detenga
    // en el centro del segmento indicado.
    ruleta.spinToItem(indice, 4000, true, 3, 1);
}

export function obtenerOpcionesVisualesRuleta() {

    const colores = {
        FACIL: "#83CBA8",
        MEDIO: "#F5CA78",
        DIFICIL: "#EF9299"
    };

    // Recorre los 10 segmentos: solo color, sin texto adentro.
    return ORDEN_SEGMENTOS.map(dificultad => ({
        value: dificultad, //Guarda la dificultad a la que pertenece el segmento
        weight: 1, //Todos los segmentos tienen el mismo tamaño (10% cada uno)
        backgroundColor: colores[dificultad] //Asigna un color para cada dificultad
    }));
}

export function crearRuleta(contenedor, Wheel) {

    if (!contenedor) { //Este elemento es el que dibuja la ruleta
        throw new Error("No se encontró el contenedor de la ruleta");
    }

    const configuracion = {
        items: obtenerOpcionesVisualesRuleta(),
        isInteractive: false,

        // La flecha blanca está a la derecha.
        // En Spin Wheel 90 grados corresponde a esa posición.
        pointerAngle: 90,

        radius: 0.9,
        lineWidth: 3,
        lineColor: "#FFFFFF"
    };

    return new Wheel(contenedor, configuracion);
}
