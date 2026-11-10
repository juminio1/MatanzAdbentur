
import { Wheel } from "./vendor/spin-wheel-esm.js";
import { solicitarDificultad } from "./servicio-ruleta.js";
import { iniciarTemporizadorRuleta } from "./temporizador-ruleta.js";

import {
    crearRuleta,
    girarRuleta
} from "./ruleta.js";

export function inicializarRuleta(contenedor) {
    return crearRuleta(contenedor, Wheel);
}

export function ejecutarGiroRuleta(dificultad, ruleta) {
    girarRuleta(dificultad, ruleta);
}

export function configurarRuleta(
    documento = document,
    solicitar = solicitarDificultad,
    crear = inicializarRuleta,
    animar = ejecutarGiroRuleta
) {

    const botonAbrir = documento.getElementById("btn-abrir-ruleta");
    const botonGirar = documento.getElementById("btn-girar-ruleta");
    const botonCerrar = documento.getElementById("btn-cerrar-ruleta");

    const panel = documento.getElementById("panel-ruleta");
    const contenedor = documento.getElementById("ruleta-visual");
    const resultado = documento.getElementById("ruleta-resultado");
    const contador = documento.getElementById("ruleta-temporizador");

    if (!botonAbrir || !botonGirar || !botonCerrar ||
        !panel || !contenedor || !resultado || !contador) {
        return;
    }

    // Determina la ruta base de la aplicación.
    // En Jasmine no existe window, por eso verificamos antes de utilizarlo.
    const basePath = typeof window !== "undefined" &&
        window.location.pathname.startsWith("/spring")
        ? "/spring"
        : "";

    const SEGUNDOS_ESPERA = 10;

    let ruleta = null;
    let girando = false;
    let giroCompletado = false;
    let dificultadElegida = null;

    let temporizadorAutomatico = null;
    let intervaloContador = null;

    const nombres = {
        FACIL: "FÁCIL",
        MEDIO: "MEDIA",
        DIFICIL: "DIFÍCIL"
    };

    function limpiarEstilosResultado() {

        resultado.classList.remove(
            "ruleta-resultado--facil",
            "ruleta-resultado--medio",
            "ruleta-resultado--dificil"
        );
    }

    function mostrarResultado(dificultad) {

        limpiarEstilosResultado();

        resultado.classList.add(
            `ruleta-resultado--${dificultad.toLowerCase()}`
        );

        resultado.textContent =
            `Resultado: ${nombres[dificultad]}`;
    }

    function limpiarTemporizador() {

        if (temporizadorAutomatico !== null) {
            clearTimeout(temporizadorAutomatico);
            temporizadorAutomatico = null;
        }

        if (intervaloContador !== null) {
            clearInterval(intervaloContador);
            intervaloContador = null;
        }
    }

    function iniciarCuentaRegresiva() {

        limpiarTemporizador();

        let segundosRestantes = SEGUNDOS_ESPERA;

        contador.hidden = false;

        contador.textContent =
            `Giro automático en ${segundosRestantes} segundos`;

        // Actualiza el contador visual cada segundo.
        intervaloContador = setInterval(() => {

            segundosRestantes--;

            contador.textContent =
                `Giro automático en ${Math.max(0, segundosRestantes)} segundos`;

        }, 1000);

        // Ejecuta automáticamente el giro al terminar el tiempo.
        temporizadorAutomatico = iniciarTemporizadorRuleta(
            SEGUNDOS_ESPERA,
            iniciarGiro
        );
    }

    async function iniciarGiro() {

        // Impide otro sorteo si ya se está girando
        // o si el giro de esta apertura ya se realizó.
        if (girando || giroCompletado || !ruleta || panel.hidden) {
            return;
        }

        // Cancela cualquier giro automático pendiente.
        limpiarTemporizador();

        // Evita que el botón y el temporizador
        // soliciten dos sorteos al backend.
        girando = true;
        giroCompletado = true;

        botonGirar.disabled = true;
        botonCerrar.disabled = true;

        contador.hidden = true;

        limpiarEstilosResultado();

        resultado.textContent = "Sorteando dificultad...";

        try {

            // Solicita al backend la dificultad sorteada.
            dificultadElegida = await solicitar(basePath);

            resultado.textContent = "Girando...";

            // El backend realiza el sorteo definitivo.
            // La ruleta solamente anima el resultado recibido.
            animar(dificultadElegida, ruleta);

            // El desbloqueo de los botones se realiza en onRest.

        } catch (error) {

            console.error("Error al sortear la dificultad:", error);

            resultado.textContent =
                "No se pudo realizar el giro";

            girando = false;

            // Permite reintentar si falló la solicitud
            // o si no pudo comenzar la animación.
            giroCompletado = false;
            dificultadElegida = null;

            botonGirar.disabled = false;
            botonCerrar.disabled = false;
        }
    }

    function finalizarGiro(evento) {

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

        // Una misma apertura permite un único sorteo.
        botonGirar.disabled = true;
        botonCerrar.disabled = false;
    }

    botonAbrir.addEventListener("click", () => {

        // Evita reiniciar el contador si el panel ya está abierto.
        if (!panel.hidden) {
            return;
        }

        panel.hidden = false;

        if (!ruleta) {

            try {

                ruleta = crear(contenedor);

                ruleta.onRest = finalizarGiro;

            } catch (error) {

                console.error("No se pudo crear la ruleta:", error);

                resultado.textContent = "Error al cargar la ruleta";

                botonGirar.disabled = true;
                botonCerrar.disabled = false;

                return;
            }
        }

        // Reinicia los datos para una nueva apertura.
        girando = false;
        giroCompletado = false;
        dificultadElegida = null;

        limpiarEstilosResultado();

        resultado.textContent = "¡Probá tu suerte!";

        botonGirar.disabled = false;

        // No permite cerrar mientras espera el giro obligatorio.
        botonCerrar.disabled = true;

        iniciarCuentaRegresiva();
    });

    botonGirar.addEventListener("click", () => {
        iniciarGiro();
    });

    botonCerrar.addEventListener("click", () => {

        if (!girando && !botonCerrar.disabled) {

            limpiarTemporizador();

            panel.hidden = true;
            contador.hidden = true;
        }
    });
}

// Inicializa la ruleta solamente cuando existe document.
// Esto evita errores al importar el módulo desde Jasmine en Node.js.
if (typeof document !== "undefined") {

    if (document.readyState === "loading") {

        document.addEventListener(
            "DOMContentLoaded",
            configurarRuleta
        );

    } else {
        configurarRuleta();
    }
}
