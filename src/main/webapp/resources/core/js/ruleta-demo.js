
import { Wheel } from "./vendor/spin-wheel-esm.js";

import {
    crearRuleta,
    girarRuleta
} from "./ruleta.js";

export function inicializarRuleta(contenedor) {
    return crearRuleta(contenedor, Wheel);
}

export function ejecutarGiroDePrueba(dificultad, ruleta) {
    girarRuleta(dificultad, ruleta);
}

export function configurarPruebaRuleta() {

    const botonAbrir = document.getElementById("btn-abrir-ruleta");
    const botonGirar = document.getElementById("btn-girar-ruleta");
    const botonCerrar = document.getElementById("btn-cerrar-ruleta");

    const panel = document.getElementById("panel-ruleta");
    const contenedor = document.getElementById("ruleta-visual");
    const resultado = document.getElementById("ruleta-resultado");

    if (!botonAbrir || !botonGirar || !botonCerrar ||
        !panel || !contenedor || !resultado) {
        return;
    }

    let ruleta = null;
    let girando = false;
    let dificultadElegida = null;

    // Secuencia temporal de prueba hasta conectar el backend.
    const dificultadesPrueba = ["FACIL", "MEDIO", "DIFICIL"];
    let siguientePrueba = 0;

    const nombres = {
        FACIL: "FÁCIL",
        MEDIO: "MEDIA",
        DIFICIL: "DIFÍCIL"
    };

    function mostrarResultado(dificultad) {

        resultado.classList.remove(
            "ruleta-resultado--facil",
            "ruleta-resultado--medio",
            "ruleta-resultado--dificil"
        );

        resultado.classList.add(
            `ruleta-resultado--${dificultad.toLowerCase()}`
        );

        resultado.textContent =
            `Resultado: ${nombres[dificultad]}`;
    }

    botonAbrir.addEventListener("click", () => {

        panel.hidden = false;

        if (!ruleta) {
            try {
                ruleta = inicializarRuleta(contenedor);

                ruleta.onRest = (evento) => {

                    // Obtiene el segmento que realmente señala la flecha.
                    const indiceReal = evento.currentIndex;
                    const segmento = ruleta.items[indiceReal];

                    if (!segmento) {
                        resultado.textContent =
                            "No se pudo identificar el resultado";
                    } else if (segmento.value !== dificultadElegida) {

                        // No mostramos un ganador incorrecto.
                        resultado.textContent =
                            "Error: el giro no coincide con el resultado";

                        console.error(
                            "Inconsistencia en la ruleta:",
                            {
                                esperado: dificultadElegida,
                                obtenido: segmento.value,
                                indiceReal
                            }
                        );

                    } else {
                        mostrarResultado(segmento.value);
                    }

                    girando = false;
                    botonGirar.disabled = false;
                    botonCerrar.disabled = false;
                };

            } catch (error) {
                console.error("No se pudo crear la ruleta:", error);
                resultado.textContent = "Error al cargar la ruleta";
                botonGirar.disabled = true;
            }
        }
    });

    botonGirar.addEventListener("click", () => {

        if (girando || !ruleta) {
            return;
        }

        // Solo para probar la animación.
        // El backend realizará el sorteo definitivo.
        dificultadElegida =
            dificultadesPrueba[siguientePrueba];

        siguientePrueba =
            (siguientePrueba + 1) % dificultadesPrueba.length;

        girando = true;
        botonGirar.disabled = true;
        botonCerrar.disabled = true;

        resultado.classList.remove(
            "ruleta-resultado--facil",
            "ruleta-resultado--medio",
            "ruleta-resultado--dificil"
        );

        resultado.textContent = "Girando...";

        try {
            ejecutarGiroDePrueba(dificultadElegida, ruleta);
        } catch (error) {
            console.error("Error al girar la ruleta:", error);

            resultado.textContent = "No se pudo realizar el giro";
            girando = false;
            botonGirar.disabled = false;
            botonCerrar.disabled = false;
        }
    });

    botonCerrar.addEventListener("click", () => {
        if (!girando) {
            panel.hidden = true;
        }
    });
}

if (document.readyState === "loading") {
    document.addEventListener(
        "DOMContentLoaded",
        configurarPruebaRuleta
    );
} else {
    configurarPruebaRuleta();
}
