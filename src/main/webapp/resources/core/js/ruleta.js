
import { CONFIGURACION_RULETA } from "./configuracion-ruleta.js";

export function obtenerIndiceDificultad(dificultad) {

    const indice = CONFIGURACION_RULETA.findIndex(
        opcion => opcion.dificultad === dificultad
    );

    if (indice === -1) {
        throw new Error("La dificultad no es válida");
    }

    return indice;
}

export function girarRuleta(dificultad, ruleta) { //Dificultad: Es el resultado que envia el back. Ruleta: Es la instancia de Spin Wheel + la animación

    const indice = obtenerIndiceDificultad(dificultad);

    ruleta.spinToItem(indice, 4000, true, 3, 1); //Indica a la librería que anime el giro y se detenga en el segmento indicado.
}

